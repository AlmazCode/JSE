package jse.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jse.GameFixtures;
import jse.core.EngineControl;
import jse.core.EngineServices;
import jse.core.Scene;
import jse.event.EventBus;
import jse.event.GameFinished;
import jse.factory.ForestFactory;
import jse.factory.SpawnRequest;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Rect;
import jse.math.Vec2;
import jse.physics.CollisionSystem;
import jse.render.RenderMode;
import jse.world.Enemy;
import jse.world.GameObject;
import jse.world.Pickup;
import jse.world.ThemeId;
import jse.world.World;
import org.junit.jupiter.api.Test;

class ArenaIntegrationTest {
    private static InputState input(GameAction... pressed) {
        return new InputState(Set.of(), Set.of(pressed));
    }

    @Test
    void realMovementIsNormalizedPausedOnFocusLossAndDoesNotResetOnVisualChanges()
            throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var scenes = new GameScenes(settings);
        var control = new Control();
        var renderer = new GameFixtures.CaptureRenderer();
        var scene = scenes.arena();
        scene.onEnter(new EngineServices(settings.engine(), renderer, control));
        var run = scene.run();
        var player = run.player();
        var initial = player.position();
        scene.update(0.1,
                new InputState(Set.of(GameAction.MOVE_RIGHT, GameAction.MOVE_DOWN), Set.of()));
        assertEquals(22, player.position().subtract(initial).length(), 1e-8);
        var moved = player.bounds();
        player.takeDamage(1);
        scene.update(0.1, input(GameAction.PAUSE));
        for (int i = 0; i < 100; i++) {
            scene.update(0.1, input(GameAction.SWITCH_RENDERER, GameAction.TOGGLE_OUTLINE,
                    GameAction.TOGGLE_HEALTH_BAR));
        }
        assertEquals(2, player.currentHealth());
        assertEquals(moved, player.bounds());
        assertEquals(1, player.id());
        scene.onFocusLost();
        scene.update(0.1, new InputState(Set.of(GameAction.MOVE_RIGHT), Set.of()));
        assertEquals(moved, player.bounds());
        assertTrue(run.paused());
        scene.update(0.1, input(GameAction.SWITCH_RENDERER, GameAction.TOGGLE_OUTLINE));
        scene.render(new GameFixtures.CaptureRenderer());
        assertEquals(RenderMode.WIREFRAME, scenes.preferences().mode());
        scene.onExit();
        assertTrue(run.events().isClosed());
        assertFalse(player.active());
    }

    @Test
    void resetThemeAndEscapeHaveTransitionPriorityAndPreserveOnlyVisualPreferences()
            throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var scenes = new GameScenes(settings);
        var renderer = new GameFixtures.CaptureRenderer();
        var control = new Control();
        var services = new EngineServices(settings.engine(), renderer, control);
        var scene = scenes.arena();
        scene.onEnter(services);
        scene.run().player().takeDamage(1);
        scene.update(0.1, input(GameAction.SWITCH_RENDERER, GameAction.TOGGLE_OUTLINE));
        scene.update(0.1, input(GameAction.SWITCH_THEME, GameAction.RESTART));
        assertInstanceOf(ArenaScene.class, control.next);
        var replacement = (ArenaScene) control.next;
        var previous = scene.run();
        scene.onExit();
        replacement.onEnter(services);
        assertEquals(ThemeId.SPACE, replacement.run().world().familyId());
        assertEquals(3, replacement.run().player().currentHealth());
        assertTrue(replacement.run().world().runId() > previous.world().runId());
        assertEquals(RenderMode.WIREFRAME, scenes.preferences().mode());
        assertFalse(scenes.preferences().outline());
        replacement.update(0.1,
                input(GameAction.BACK_TO_TITLE, GameAction.SWITCH_THEME, GameAction.RESTART));
        assertInstanceOf(TitleScene.class, control.next);
        assertEquals(ThemeId.SPACE, scenes.theme());
        replacement.onExit();
    }

    @Test
    void rulesCollectOnceApplyCooldownAndGiveLossPriorityOverTheLastPickup() {
        var renderer = new GameFixtures.CaptureRenderer();
        var factory = new ForestFactory(renderer, GameFixtures.entities(),
                GameFixtures.sprites("forest"));
        var events = new EventBus();
        var world = new World(1, new Rect(0, 0, 200, 200), ThemeId.FOREST, 10, events);
        var player = factory.createPlayer(new SpawnRequest(new Vec2(20, 20), new Vec2(24, 24)));
        var enemy = factory.createEnemy(new SpawnRequest(new Vec2(20, 20), new Vec2(24, 24)));
        var pickup = factory.createPickup(new SpawnRequest(new Vec2(20, 20), new Vec2(16, 16)));
        world.enqueueAdd(player);
        world.enqueueAdd(enemy);
        world.enqueueAdd(pickup);
        world.commitChanges(0);
        var rules = new ArenaRules(new ArenaSettings(1, 0.75, 1), events);
        var finished = new ArrayList<GameFinished>();
        events.subscribe(GameFinished.class, finished::add);
        player.takeDamage(2);
        var collisions = new CollisionSystem().findOverlaps(world.view());
        var duplicate = new ArrayList<>(collisions);
        duplicate.addAll(collisions);
        rules.applyCollisions(world, duplicate, 0.1, 1);
        world.commitChanges(1);
        rules.evaluateOutcome(world, 1);
        assertEquals(1, rules.score());
        assertEquals(0, player.currentHealth());
        assertEquals(ArenaRules.Status.LOST, rules.status());
        assertEquals(1, finished.size());
        rules.evaluateOutcome(world, 2);
        assertEquals(1, finished.size());
        assertTrue(world.findActive(pickup.id()).isEmpty());
    }

    @Test
    void contactCooldownAndPausedTimeDoNotCauseRepeatedDamage() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var run = new ArenaRun(1, ThemeId.FOREST, settings, new GameFixtures.CaptureRenderer());
        var objects = new ArrayList<GameObject>();
        run.world().forEachActive(objects::add);
        var enemy = objects.stream().filter(Enemy.class::isInstance).map(Enemy.class::cast)
                .findFirst().orElseThrow();
        enemy.setBehavior(new jse.ai.IdleBehavior());
        enemy.setPosition(run.player().position());
        run.advance(0.1, input());
        assertEquals(2, run.player().currentHealth());
        run.advance(0.1, input());
        assertEquals(2, run.player().currentHealth());
        run.togglePause();
        run.advance(10, input());
        assertEquals(2, run.player().currentHealth());
        run.togglePause();
        run.advance(0.65, input());
        assertEquals(1, run.player().currentHealth());
        run.close();
    }

    @Test
    void collectingAllRealPickupsWinsOnceAndFreezesMovement() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var run = new ArenaRun(1, ThemeId.SPACE, settings, new GameFixtures.CaptureRenderer());
        var pickups = new ArrayList<GameObject>();
        run.world().forEachActive(object -> {
            if (object instanceof Pickup) {
                pickups.add(object);
            }
        });
        var finished = new ArrayList<GameFinished>();
        run.events().subscribe(GameFinished.class, finished::add);
        for (var pickup : pickups) {
            run.player().setPosition(pickup.position());
            run.advance(0.1, input());
        }
        assertEquals(8, run.rules().score());
        assertEquals(ArenaRules.Status.WON, run.rules().status());
        assertEquals(3, run.player().currentHealth());
        assertEquals(1, finished.size());
        var position = run.player().position();
        run.advance(0.1, new InputState(Set.of(GameAction.MOVE_RIGHT), Set.of()));
        assertEquals(position, run.player().position());
        assertEquals(1, finished.size());
        run.close();
    }

    static class Control implements EngineControl {
        Scene next;
        boolean stopped;
        final List<RenderMode> modes = new ArrayList<>();

        public void requestScene(Scene scene) {
            next = scene;
        }

        public void setRenderMode(RenderMode mode) {
            modes.add(mode);
        }

        public void stop() {
            stopped = true;
        }
    }
}
