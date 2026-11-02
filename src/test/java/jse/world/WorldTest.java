package jse.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import jse.GameFixtures;
import jse.event.EntityRemoved;
import jse.event.EntitySpawned;
import jse.event.EventBus;
import jse.event.GameEvent;
import jse.factory.ForestFactory;
import jse.factory.GameObjectFamilyFactory;
import jse.factory.SpaceFactory;
import jse.factory.SpawnRequest;
import jse.math.Rect;
import jse.math.Vec2;
import org.junit.jupiter.api.Test;

class WorldTest {

    private final GameObjectFamilyFactory factory = new ForestFactory(
            new GameFixtures.CaptureRenderer(), GameFixtures.entities(),
            GameFixtures.sprites("forest"));

    private Player player(double x) {
        return factory.createPlayer(new SpawnRequest(new Vec2(x, 20), new Vec2(24, 24)));
    }

    private World world(int capacity, EventBus events) {
        return new World(1, new Rect(0, 0, 200, 160), ThemeId.FOREST, capacity, events);
    }

    @Test
    void additionsAreQueuedIdsStableAndSnapshotsImmutable() {
        var events = new EventBus();
        var spawned = new ArrayList<EntitySpawned>();
        events.subscribe(EntitySpawned.class, spawned::add);
        var world = world(4, events);
        var first = player(10);
        world.enqueueAdd(first);
        assertEquals(1, first.id());
        assertFalse(first.active());
        assertTrue(world.findActive(1).isEmpty());
        assertTrue(world.view().entities().isEmpty());
        assertEquals(1, world.spawnView().entities().size());
        assertTrue(spawned.isEmpty());
        world.commitChanges(7);
        assertTrue(first.active());
        assertEquals(1, spawned.size());
        assertEquals(7, spawned.get(0).tick());
        var saved = world.view();
        first.setPosition(new Vec2(60, 40));
        assertEquals(new Vec2(10, 20), saved.playerPosition().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> saved.entities().clear());
        assertEquals(new Vec2(60, 40), world.view().playerPosition().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> world.enqueueAdd(first));
    }

    @Test
    void queuedCancellationConsumesItsIdWithoutLifecycleEvents() {
        var events = new EventBus();
        var changes = new ArrayList<GameEvent>();
        events.subscribe(EntitySpawned.class, changes::add);
        events.subscribe(EntityRemoved.class, changes::add);
        var world = world(2, events);
        var cancelled = player(10);
        world.enqueueAdd(cancelled);
        world.enqueueRemove(cancelled.id(), RemovalReason.DIAGNOSTIC);
        assertEquals(0, world.sizeIncludingPending());
        world.commitChanges(0);
        assertTrue(changes.isEmpty());
        var next = player(50);
        world.enqueueAdd(next);
        assertEquals(2, next.id());
    }

    @Test
    void removingDuringIterationImmediatelyHidesAnObjectAndCommitsOnce() {
        var events = new EventBus();
        var removed = new ArrayList<EntityRemoved>();
        events.subscribe(EntityRemoved.class, removed::add);
        var world = world(3, events);
        var first = player(10);
        var second = player(60);
        world.enqueueAdd(first);
        world.enqueueAdd(second);
        world.commitChanges(0);
        var visited = new ArrayList<Long>();
        world.forEachActive(object -> {
            visited.add(object.id());
            world.enqueueRemove(second.id(), RemovalReason.COLLECTED);
        });
        assertEquals(List.of(1L), visited);
        assertTrue(world.findActive(second.id()).isEmpty());
        assertEquals(1, world.view().entities().size());
        world.enqueueRemove(second.id(), RemovalReason.COLLECTED);
        world.commitChanges(1);
        world.commitChanges(2);
        assertEquals(1, removed.size());
        assertFalse(second.active());
    }

    @Test
    void capacityAndFamilyAndCompleteBoundsAreValidatedBeforeAssigningIds() {
        var world = world(1, new EventBus());
        var wrong = new SpaceFactory(new GameFixtures.CaptureRenderer(), GameFixtures.entities(),
                GameFixtures.sprites("space"))
                .createPlayer(new SpawnRequest(new Vec2(10, 20), new Vec2(24, 24)));
        assertThrows(IllegalArgumentException.class, () -> world.enqueueAdd(wrong));
        assertEquals(0, wrong.id());
        assertThrows(IllegalArgumentException.class, () -> world.enqueueAdd(player(190)));
        var first = player(10);
        world.enqueueAdd(first);
        var full = player(60);
        assertThrows(IllegalStateException.class, () -> world.enqueueAdd(full));
        assertEquals(0, full.id());
        world.enqueueRemove(first.id(), RemovalReason.DIAGNOSTIC);
        world.enqueueAdd(full);
        world.commitChanges(0);
        assertEquals(2, full.id());
        assertEquals(1, world.view().entities().size());
    }

    @Test
    void eventCallbacksQueueChangesForTheNextCommit() {
        var events = new EventBus();
        var world = world(3, events);
        events.subscribe(EntitySpawned.class, event -> {
            if (event.entityId() == 1) {
                world.enqueueAdd(player(70));
            }
        });
        world.enqueueAdd(player(10));
        world.commitChanges(0);
        assertEquals(1, world.view().entities().size());
        assertEquals(2, world.spawnView().entities().size());
        world.commitChanges(1);
        assertEquals(2, world.view().entities().size());
    }

    @Test
    void closeDeactivatesEntitiesAndRejectsFurtherMutation() {
        var world = world(3, new EventBus());
        var player = player(10);
        world.enqueueAdd(player);
        world.commitChanges(0);
        world.close();
        world.close();
        assertFalse(player.active());
        assertTrue(world.view().entities().isEmpty());
        assertThrows(IllegalStateException.class, () -> world.enqueueAdd(player(60)));
        assertThrows(IllegalStateException.class, () -> world.commitChanges(1));
    }
}
