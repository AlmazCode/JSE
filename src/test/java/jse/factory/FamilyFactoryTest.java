package jse.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jse.GameFixtures;
import jse.event.EventBus;
import jse.math.Vec2;
import jse.world.ObjectKind;
import jse.world.ThemeId;
import jse.world.World;
import org.junit.jupiter.api.Test;

class FamilyFactoryTest {

    @Test
    void bothFamiliesCreateAllCategoriesWithInjectedSettingsAndSpriteIds() {
        var renderer = new GameFixtures.CaptureRenderer();
        for (var factory : List.of(
                new ForestFactory(renderer, GameFixtures.entities(),
                        GameFixtures.sprites("forest")),
                new SpaceFactory(renderer, GameFixtures.entities(),
                        GameFixtures.sprites("space")))) {
            var player = factory.createPlayer(new SpawnRequest(new Vec2(10, 20), new Vec2(24, 24)));
            var enemy = factory.createEnemy(new SpawnRequest(new Vec2(50, 20), new Vec2(24, 24)));
            var pickup = factory.createPickup(new SpawnRequest(new Vec2(90, 20), new Vec2(16, 16)));
            assertEquals(factory.familyId(), player.familyId());
            assertEquals(factory.familyId(), enemy.familyId());
            assertEquals(factory.familyId(), pickup.familyId());
            assertEquals(ObjectKind.PLAYER, player.category());
            assertEquals(ObjectKind.ENEMY, enemy.category());
            assertEquals(ObjectKind.PICKUP, pickup.category());
            assertEquals(3, player.currentHealth());
            player.takeDamage(2);
            assertEquals(1, player.currentHealth());
            player.takeDamage(Integer.MAX_VALUE);
            assertEquals(0, player.currentHealth());
            assertEquals(220, player.speed());
            assertEquals(110, enemy.speed());
            assertEquals(1, pickup.value());
            renderer.clear();
            for (var object : List.of(player, enemy, pickup)) {
                object.graphic().draw(object.bounds());
            }
            assertEquals(3, renderer.sprites.size());
            assertTrue(renderer.sprites.stream()
                    .allMatch(id -> id.path().contains(factory.familyId().name().toLowerCase())));
            assertThrows(IllegalArgumentException.class,
                    () -> factory.createPlayer(new SpawnRequest(new Vec2(0, 0), new Vec2(25, 24))));
        }
    }

    @Test
    void spawnersUseQueuedWorldRegistrationAndRejectWrongFamilies() {
        var factory = new ForestFactory(new GameFixtures.CaptureRenderer(), GameFixtures.entities(),
                GameFixtures.sprites("forest"));
        var world = new World(1, new jse.math.Rect(0, 0, 200, 200), ThemeId.FOREST, 5,
                new EventBus());
        var player = new PlayerSpawner(factory).spawn(world,
                new SpawnRequest(new Vec2(10, 20), new Vec2(24, 24)));
        var enemy = new EnemySpawner(factory).spawn(world,
                new SpawnRequest(new Vec2(50, 20), new Vec2(24, 24)));
        var pickup = new PickupSpawner(factory).spawn(world,
                new SpawnRequest(new Vec2(90, 20), new Vec2(16, 16)));
        assertEquals(3, world.sizeIncludingPending());
        assertFalse(player.active());
        world.commitChanges(0);
        assertTrue(player.active() && enemy.active() && pickup.active());
        var wrong = new World(2, new jse.math.Rect(0, 0, 200, 200), ThemeId.SPACE, 5,
                new EventBus());
        assertThrows(IllegalStateException.class, () -> new PlayerSpawner(factory).spawn(wrong,
                new SpawnRequest(new Vec2(10, 20), new Vec2(24, 24))));
    }
}
