package jse.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import jse.GameFixtures;
import jse.core.EngineConfig;
import jse.core.EngineControl;
import jse.core.EngineHost;
import jse.core.EngineServices;
import jse.core.JseEngine;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputSource;
import jse.input.InputState;
import jse.render.RenderMode;
import jse.render.Renderer;
import org.junit.jupiter.api.Test;

class GameSceneLifecycleTest {
    @Test
    void twentyActualEngineCyclesCloseRunsAndKeepPreferencesAcrossEveryRoute() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var scenes = new GameScenes(settings);
        var input = new Keys();
        var host = new Host();
        var tracker = new Tracker();
        var engine = new JseEngine(settings.engine(), host, input);
        engine.start(tracker.wrap(scenes.title()));
        long time = 0;
        engine.pump(time);
        var oldRuns = new ArrayList<ArenaRun>();
        for (int cycle = 0; cycle < 20; cycle++) {
            time = command(engine, input, time, GameAction.START_ARENA);
            assertInstanceOf(ArenaScene.class, tracker.current);
            var scene = (ArenaScene) tracker.current;
            var run = scene.run();
            time = command(engine, input, time, GameAction.TOGGLE_OUTLINE,
                    GameAction.SWITCH_RENDERER);
            var mode = scenes.preferences().mode();
            var outline = scenes.preferences().outline();
            assertEquals(run, ((ArenaScene) tracker.current).run());
            time = command(engine, input, time, GameAction.RESTART);
            assertTrue(run.events().isClosed());
            assertFalse(run.player().active());
            oldRuns.add(run);
            run = ((ArenaScene) tracker.current).run();
            assertEquals(mode, scenes.preferences().mode());
            assertEquals(outline, scenes.preferences().outline());
            var theme = run.world().familyId();
            time = command(engine, input, time, GameAction.SWITCH_THEME);
            assertTrue(run.events().isClosed());
            oldRuns.add(run);
            assertNotEquals(theme, ((ArenaScene) tracker.current).run().world().familyId());
            run = ((ArenaScene) tracker.current).run();
            time = command(engine, input, time, GameAction.BACK_TO_TITLE);
            assertTrue(run.events().isClosed());
            oldRuns.add(run);
            assertInstanceOf(TitleScene.class, tracker.current);
            time = command(engine, input, time, GameAction.SHOW_SHOWCASE);
            assertInstanceOf(ShowcaseScene.class, tracker.current);
            time = command(engine, input, time, GameAction.TOGGLE_HEALTH_BAR);
            var health = scenes.preferences().healthBar();
            time = command(engine, input, time, GameAction.BACK_TO_TITLE);
            assertInstanceOf(TitleScene.class, tracker.current);
            assertEquals(mode, host.mode);
            assertEquals(health, scenes.preferences().healthBar());
            assertEquals(outline, scenes.preferences().outline());
        }
        assertEquals(60, oldRuns.size());
        assertTrue(oldRuns.stream().allMatch(run -> run.world().view().entities().isEmpty()));
        assertNull(host.failure);
        assertTrue(input.clears >= 120);
        command(engine, input, time, GameAction.BACK_TO_TITLE);
        assertEquals(1, host.closes);
    }

    @Test
    void outcomeFreezesMovementButKeepsVisualAndNavigationCommands() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var scenes = new GameScenes(settings);
        var control = new ArenaIntegrationTest.Control();
        var scene = scenes.arena();
        scene.onEnter(
                new EngineServices(settings.engine(), new GameFixtures.CaptureRenderer(), control));
        scene.run().player().takeDamage(Integer.MAX_VALUE);
        scene.update(0.1, new InputState(Set.of(), Set.of()));
        assertEquals(ArenaRules.Status.LOST, scene.run().rules().status());
        var position = scene.run().player().position();
        scene.update(0.1, new InputState(Set.of(GameAction.MOVE_RIGHT),
                Set.of(GameAction.SWITCH_RENDERER, GameAction.TOGGLE_HEALTH_BAR)));
        assertEquals(position, scene.run().player().position());
        assertEquals(RenderMode.WIREFRAME, scenes.preferences().mode());
        assertFalse(scenes.preferences().healthBar());
        scene.update(0.1, new InputState(Set.of(), Set.of(GameAction.RESTART)));
        assertInstanceOf(ArenaScene.class, control.next);
        scene.onExit();
    }

    private long command(JseEngine engine, Keys input, long time, GameAction... commands) {
        input.pressed = Set.of(commands);
        time += 20_000_000;
        engine.pump(time);
        engine.render(new GameFixtures.CaptureRenderer());
        return time;
    }

    private static class Tracker {
        Scene current;

        Scene wrap(Scene scene) {
            return new Scene() {
                public void onEnter(EngineServices services) {
                    current = scene;
                    scene.onEnter(new EngineServices(services.config(), services.initialRenderer(),
                            new EngineControl() {
                                public void requestScene(Scene next) {
                                    services.control().requestScene(wrap(next));
                                }

                                public void setRenderMode(RenderMode mode) {
                                    services.control().setRenderMode(mode);
                                }

                                public void stop() {
                                    services.control().stop();
                                }
                            }));
                }

                public void update(double dt, InputState input) {
                    scene.update(dt, input);
                }

                public void render(Renderer renderer) {
                    scene.render(renderer);
                }

                public void onFocusLost() {
                    scene.onFocusLost();
                }

                public void onExit() {
                    scene.onExit();
                }
            };
        }
    }

    private static class Keys implements InputSource {
        Set<GameAction> pressed = Set.of();
        int clears;

        public InputState snapshotAndConsumePressed() {
            var result = new InputState(Set.of(), pressed);
            pressed = Set.of();
            return result;
        }

        public void clear() {
            pressed = Set.of();
            clears++;
        }
    }

    private static class Host implements EngineHost {
        final Renderer renderer = new GameFixtures.CaptureRenderer();
        RenderMode mode = RenderMode.FILLED;
        Throwable failure;
        int closes;

        public void open(EngineConfig config, LongConsumer pump, Consumer<Renderer> paint,
                Runnable focus, Runnable close, Consumer<RuntimeException> failed) {
        }

        public Renderer currentRenderer() {
            return renderer;
        }

        public void setRenderMode(RenderMode mode) {
            this.mode = mode;
        }

        public void startTimer() {
        }

        public void requestRepaint() {
        }

        public void showError(String message, Throwable cause) {
            failure = cause;
        }

        public void close() {
            closes++;
        }
    }
}
