package jse.core;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

public final class SceneManager implements AutoCloseable {
    private Scene current;
    private Scene pending;

    public void activate(Scene scene, EngineServices services) {
        Objects.requireNonNull(scene, "scene");
        if (current != null)
            throw new IllegalStateException("A scene is already active");
        current = scene;
        try {
            scene.onEnter(services);
        } catch (RuntimeException failure) {
            try {
                closeCurrent();
            } catch (RuntimeException cleanup) {
                if (failure != cleanup)
                    failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    public void request(Scene scene) {
        pending = Objects.requireNonNull(scene, "scene");
    }

    public boolean applyPending(Supplier<EngineServices> services, BooleanSupplier activationAllowed) {
        if (pending == null)
            return false;
        var next = pending;
        pending = null;
        closeCurrent();
        if (activationAllowed.getAsBoolean())
            activate(next, services.get());
        return true;
    }

    public Scene current() {
        return current;
    }

    public boolean hasPending() {
        return pending != null;
    }

    @Override
    public void close() {
        pending = null;
        closeCurrent();
    }

    private void closeCurrent() {
        var previous = current;
        current = null;
        if (previous != null)
            previous.onExit();
    }
}
