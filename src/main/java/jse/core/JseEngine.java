package jse.core;

import java.util.Objects;
import jse.input.InputSource;
import jse.render.Renderer;
import jse.render.RenderMode;

public final class JseEngine implements EngineControl, AutoCloseable {
    private enum State { NEW, RUNNING, FAILED, STOPPED }
    private final EngineConfig config;
    private final EngineHost host;
    private final InputSource input;
    private final SceneManager scenes = new SceneManager();
    private final FixedStepClock clock;
    private State state = State.NEW;
    private Thread owner;
    private long completedUpdates;

    public JseEngine(EngineConfig config, EngineHost host, InputSource input) {
        this.config = Objects.requireNonNull(config, "config");
        this.host = Objects.requireNonNull(host, "host");
        this.input = Objects.requireNonNull(input, "input");
        clock = new FixedStepClock(config);
    }

    public void start(Scene initial) {
        if (state != State.NEW)
            throw new IllegalStateException("Engine can only start once");
        Objects.requireNonNull(initial, "initialScene");
        owner = Thread.currentThread();
        state = State.RUNNING;
        try {
            host.open(config, this::pump, this::render, this::focusLost, this::stop, this::fail);
            scenes.activate(initial, services());
            if (state == State.RUNNING)
                host.startTimer();
        } catch (RuntimeException failure) {
            fail(failure);
        }
    }

    private EngineServices services() {
        return new EngineServices(config, host.currentRenderer(), this);
    }

    public void pump(long now) {
        checkThread();
        if (state != State.RUNNING)
            return;
        try {
            var plan = clock.advance(now);
            if (applyTransition()) {
                if (state == State.RUNNING)
                    host.requestRepaint();
                return;
            }
            for (int step = 0; step < plan.steps() && state == State.RUNNING; step++) {
                scenes.current().update(plan.dt(), input.snapshotAndConsumePressed());
                completedUpdates++;
                if (state != State.RUNNING || applyTransition())
                    break;
            }
            if (state == State.RUNNING)
                host.requestRepaint();
        } catch (RuntimeException failure) {
            fail(failure);
        }
    }

    private boolean applyTransition() {
        if (!scenes.hasPending())
            return false;
        input.clear();
        clock.resetAccumulator();
        return scenes.applyPending(this::services, () -> state == State.RUNNING);
    }

    public void render(Renderer renderer) {
        checkThread();
        if (state == State.RUNNING)
            scenes.current().render(renderer);
    }

    private void focusLost() {
        checkThread();
        if (state != State.RUNNING)
            return;
        try {
            input.clear();
            scenes.current().onFocusLost();
        } catch (RuntimeException failure) {
            fail(failure);
        }
    }

    private void fail(RuntimeException failure) {
        checkThread();
        if (state != State.RUNNING)
            return;
        state = State.FAILED;
        try {
            scenes.close();
        } catch (RuntimeException cleanup) {
            combine(failure, cleanup);
        }
        try {
            input.clear();
        } catch (RuntimeException cleanup) {
            combine(failure, cleanup);
        }
        clock.resetAccumulator();
        host.showError("The runtime stopped after an error.", failure);
    }

    @Override
    public void requestScene(Scene scene) {
        checkThread();
        requireRunning();
        scenes.request(scene);
    }

    @Override
    public void setRenderMode(RenderMode mode) {
        checkThread();
        requireRunning();
        host.setRenderMode(Objects.requireNonNull(mode, "mode"));
    }

    @Override
    public void stop() {
        if (owner != null)
            checkThread();
        if (state == State.STOPPED)
            return;
        state = State.STOPPED;
        RuntimeException failure = null;
        try {
            scenes.close();
        } catch (RuntimeException ex) {
            failure = ex;
        }
        try {
            input.clear();
        } catch (RuntimeException ex) {
            failure = combine(failure, ex);
        }
        clock.resetAccumulator();
        try {
            host.close();
        } catch (RuntimeException ex) {
            failure = combine(failure, ex);
        }
        if (failure != null)
            throw failure;
    }

    @Override
    public void close() {
        stop();
    }

    public EngineStats stats() {
        if (owner != null)
            checkThread();
        return new EngineStats(completedUpdates, clock.droppedSeconds(), clock.pendingSeconds());
    }

    private void requireRunning() {
        if (state != State.RUNNING)
            throw new IllegalStateException("Engine is not running");
    }

    private void checkThread() {
        if (owner != Thread.currentThread())
            throw new IllegalStateException("Engine must run on its starting thread");
    }

    private static RuntimeException combine(RuntimeException first, RuntimeException next) {
        if (first == null)
            return next;
        if (first != next)
            first.addSuppressed(next);
        return first;
    }
}
