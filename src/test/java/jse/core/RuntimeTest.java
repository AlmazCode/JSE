package jse.core;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import jse.input.GameAction;
import jse.input.InputState;
import jse.input.InputSource;
import jse.render.Renderer;
import jse.render.RenderMode;
import jse.math.Vec2;
import jse.math.Rect;
import jse.math.Rgba;
import jse.assets.SpriteId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeTest {
    private final EngineConfig config = new EngineConfig(960, 540, 60, 5, 0.25, 16);
    @Test
    void clockBoundsCatchupAndPreservesSubTickRemainder() {
        var clock = new FixedStepClock(config);
        assertEquals(0, clock.advance(0).steps());
        assertEquals(5, clock.advance(1_000_000_000).steps());
        assertEquals(11.0 / 12, clock.droppedSeconds(), 1e-12);
        assertTrue(clock.pendingSeconds() < 1.0 / 60);
        assertThrows(IllegalArgumentException.class, () -> clock.advance(0));
        assertEquals(1, clock.advance(1_016_666_667).steps());
    }
    @Test
    void commandsAppearOnlyInFirstCatchupStep() {
        var input = new Keys();
        var host = new Host();
        var scene = new RecordingScene();
        var engine = new JseEngine(config, host, input);
        engine.start(scene);
        engine.pump(0);
        engine.pump(50_000_000);
        assertEquals(3, scene.inputs.size());
        assertTrue(scene.inputs.get(0).isPressed(GameAction.PAUSE));
        assertFalse(scene.inputs.get(1).isPressed(GameAction.PAUSE));
        assertTrue(scene.inputs.get(2).isHeld(GameAction.MOVE_UP));
        assertEquals(3, engine.stats().completedUpdates());
        engine.stop();
        engine.stop();
        assertEquals(1, scene.exits);
        assertEquals(1, host.closed);
        assertThrows(IllegalStateException.class, () -> engine.start(scene));
    }
    @Test
    void lastTransitionWinsAndCancelsRemainingCatchup() {
        var input = new Keys();
        var host = new Host();
        var discarded = new RecordingScene();
        var next = new RecordingScene();
        var scene = new RecordingScene() {
            @Override
            public void update(double dt, InputState state) {
                super.update(dt, state);
                services.control().requestScene(discarded);
                services.control().requestScene(next);
            }
        };
        var engine = new JseEngine(config, host, input);
        engine.start(scene);
        engine.pump(0);
        engine.pump(100_000_000);
        assertEquals(1, scene.inputs.size());
        assertEquals(1, scene.exits);
        assertEquals(0, discarded.enters);
        assertEquals(1, next.enters);
        assertEquals(0, next.inputs.size());
        assertTrue(input.clears > 0);
        assertEquals(0, engine.stats().pendingSeconds());
        engine.stop();
        assertEquals(1, next.exits);
    }
    @Test
    void partialEntryFailureExitsOnceAndLeavesClosableErrorState() {
        var host = new Host();
        var scene = new RecordingScene() {
            @Override
            public void onEnter(EngineServices services) {
                super.onEnter(services);
                throw new IllegalStateException("entry failure");
            }
        };
        var engine = new JseEngine(config, host, new Keys());
        engine.start(scene);
        assertEquals(1, scene.exits);
        assertNotNull(host.error);
        assertEquals(0, host.closed);
        engine.pump(10);
        engine.stop();
        assertEquals(1, scene.exits);
        assertEquals(1, host.closed);
    }
    @Test
    void updateFailureAndStopDuringUpdateCannotRunFurtherSteps() {
        for (boolean fail : new boolean[] {false, true}) {
            var host = new Host();
            var scene = new RecordingScene() {
                @Override
                public void update(double dt, InputState state) {
                    super.update(dt, state);
                    if (fail)
                        throw new IllegalStateException("update failure");
                    services.control().stop();
                }
            };
            var engine = new JseEngine(config, host, new Keys());
            engine.start(scene);
            engine.pump(0);
            engine.pump(100_000_000);
            assertEquals(1, scene.inputs.size());
            assertEquals(1, scene.exits);
            assertEquals(fail ? 0 : 1, host.closed);
            if (fail)
                assertNotNull(host.error);
            engine.stop();
            assertEquals(1, host.closed);
        }
    }
    @Test
    void stopClosesHostEvenWhenSceneExitThrows() {
        var host = new Host();
        var scene = new RecordingScene() {
            @Override
            public void onExit() {
                super.onExit();
                throw new IllegalStateException("exit failure");
            }
        };
        var engine = new JseEngine(config, host, new Keys());
        engine.start(scene);
        assertThrows(IllegalStateException.class, engine::stop);
        engine.stop();
        assertEquals(1, scene.exits);
        assertEquals(1, host.closed);
    }
    @Test
    void stopDuringExitCancelsReplacementEntry() {
        var host = new Host();
        var next = new RecordingScene();
        var scene = new RecordingScene() {
            @Override
            public void onExit() {
                super.onExit();
                services.control().stop();
            }
        };
        var engine = new JseEngine(config, host, new Keys());
        engine.start(scene);
        engine.pump(0);
        engine.requestScene(next);
        engine.pump(20_000_000);
        assertEquals(1, scene.exits);
        assertEquals(0, next.enters);
        assertEquals(1, host.closed);
    }
    @Test
    void externalTransitionAdvancesClockWithoutReplayingPreviousGap() {
        var next = new RecordingScene();
        var engine = new JseEngine(config, new Host(), new Keys());
        engine.start(new RecordingScene());
        engine.pump(0);
        engine.requestScene(next);
        engine.pump(100_000_000);
        engine.pump(116_666_667);
        assertEquals(1, next.inputs.size());
        engine.stop();
    }
    @Test
    void hostPaintFailureStopsUpdatesButKeepsHostAvailableUntilClose() {
        var host = new Host();
        var scene = new RecordingScene();
        var engine = new JseEngine(config, host, new Keys());
        engine.start(scene);
        var failure = new IllegalStateException("paint failure");
        host.failed.accept(failure);
        engine.pump(0);
        engine.pump(100_000_000);
        assertSame(failure, host.error);
        assertTrue(scene.inputs.isEmpty());
        assertEquals(1, scene.exits);
        assertEquals(0, host.closed);
        engine.stop();
        assertEquals(1, host.closed);
    }
    @Test
    void reusedExceptionAcrossFailureAndCleanupPreservesOriginalCause() {
        for (boolean failOnEntry : new boolean[] {true, false}) {
            var failure = new IllegalStateException("shared failure");
            var host = new Host();
            var scene = new RecordingScene() {
                @Override
                public void onEnter(EngineServices services) {
                    super.onEnter(services);
                    if (failOnEntry)
                        throw failure;
                }
                @Override
                public void update(double dt, InputState input) {
                    throw failure;
                }
                @Override
                public void onExit() {
                    super.onExit();
                    throw failure;
                }
            };
            var engine = new JseEngine(config, host, new Keys());
            assertDoesNotThrow(() -> engine.start(scene));
            assertDoesNotThrow(() -> {
                engine.pump(0);
                engine.pump(100_000_000);
            });
            assertSame(failure, host.error);
            assertEquals(1, scene.exits);
            engine.stop();
            assertEquals(1, host.closed);
        }
    }

    @Test
    void operationsAreConfinedToTheStartingThread() throws Exception {
        var engine = new JseEngine(config, new Host(), new Keys());
        engine.start(new RecordingScene());
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var thread = new Thread(() -> {
            try {
                engine.pump(0);
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });
        thread.start();
        thread.join();
        assertInstanceOf(IllegalStateException.class, failure.get());
        engine.stop();
    }
    static class Keys implements InputSource {
        boolean consumed;
        int clears;
        public InputState snapshotAndConsumePressed() {
            var state = new InputState(Set.of(GameAction.MOVE_UP), consumed ? Set.of() : Set.of(GameAction.PAUSE));
            consumed = true;
            return state;
        }
        public void clear() {
            clears++;
            consumed = true;
        }
    }
    static class RecordingScene implements Scene {
        int enters, exits;
        EngineServices services;
        List<InputState> inputs = new ArrayList<>();
        public void onEnter(EngineServices services) {
            this.services = services;
            enters++;
        }
        public void update(double dt, InputState input) {
            assertEquals(1.0 / 60, dt);
            inputs.add(input);
        }
        public void render(Renderer renderer) {}
        public void onFocusLost() {}
        public void onExit() {
            exits++;
        }
    }
    static class Host implements EngineHost {
        int closed;
        Throwable error;
        Consumer<RuntimeException> failed;
        Renderer renderer = new Renderer() {
            public void drawSprite(SpriteId id, Rect bounds) {}
            public void drawRectangle(Rect bounds, Rgba color) {}
            public void drawOutline(Rect bounds, Rgba color) {}
            public void drawText(String text, Vec2 position, Rgba color) {}
        };
        public void open(EngineConfig config, LongConsumer pump, Consumer<Renderer> paint, Runnable focusLost,
                Runnable closeRequested, Consumer<RuntimeException> failed) {
            this.failed = failed;
        }
        public Renderer currentRenderer() {
            return renderer;
        }
        public void setRenderMode(RenderMode mode) {}
        public void startTimer() {}
        public void requestRepaint() {}
        public void showError(String message, Throwable cause) {
            error = cause;
        }
        public void close() {
            closed++;
        }
    }
}
