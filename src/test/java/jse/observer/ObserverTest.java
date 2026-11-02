package jse.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jse.demo.ArenaRules;
import jse.event.BehaviorChanged;
import jse.event.EntityRemoved;
import jse.event.EntitySpawned;
import jse.event.EventBus;
import jse.event.GameFinished;
import jse.event.HealthChanged;
import jse.event.ItemCollected;
import jse.testing.Fixtures;
import jse.world.ObjectKind;
import jse.world.RemovalReason;
import jse.world.ThemeId;
import org.junit.jupiter.api.Test;

class ObserverTest {
    private final EventBus bus = new EventBus();

    @Test
    void hudStartsFromPlayerHealthAndFollowsEvents() {
        try (HudObserver hud = new HudObserver(bus, Fixtures.player(10, 10))) {
            assertEquals(0, hud.score());
            assertEquals(3, hud.health());
            assertTrue(hud.result().isEmpty());
            bus.publish(new ItemCollected(1, 1, 1, 2, 4));
            bus.publish(new HealthChanged(1, 2, 1, 3, 2));
            bus.publish(new GameFinished(1, 3, ArenaRules.Status.LOST, 4));
            assertEquals(4, hud.score());
            assertEquals(2, hud.health());
            assertEquals(ArenaRules.Status.LOST, hud.result().orElseThrow());
        }
    }

    @Test
    void hudIgnoresEventTypesItDidNotSubscribeTo() {
        try (HudObserver hud = new HudObserver(bus, Fixtures.player(10, 10))) {
            bus.publish(new EntitySpawned(1, 0, 5, ObjectKind.ENEMY, ThemeId.FOREST));
            assertEquals(0, hud.score());
            assertEquals(3, hud.health());
        }
    }

    @Test
    void logRecordsAllSixEventTypesAsReadableLines() {
        try (EventLogObserver log = new EventLogObserver(bus, 50)) {
            bus.publish(new EntitySpawned(1, 0, 1, ObjectKind.PLAYER, ThemeId.SPACE));
            bus.publish(new EntityRemoved(1, 1, 2, RemovalReason.COLLECTED));
            bus.publish(new ItemCollected(1, 1, 1, 2, 1));
            bus.publish(new HealthChanged(1, 2, 1, 3, 2));
            bus.publish(new BehaviorChanged(1, 3, 4, "Chase"));
            bus.publish(new GameFinished(1, 4, ArenaRules.Status.WON, 8));
            List<String> lines = log.lines();
            assertEquals(6, lines.size());
            assertEquals("[tick 0] PLAYER #1 spawned (SPACE)", lines.get(0));
            assertEquals("[tick 3] Enemy #4 behavior -> Chase", lines.get(4));
            assertEquals("[tick 4] Game finished: WON, score 8", lines.get(5));
            assertThrows(UnsupportedOperationException.class, () -> lines.add("x"));
        }
    }

    @Test
    void logKeepsOnlyTheLatestLinesInChronologicalOrder() {
        try (EventLogObserver log = new EventLogObserver(bus, 3)) {
            for (int i = 1; i <= 5; i++) {
                bus.publish(new BehaviorChanged(1, i, 1, "Chase"));
            }
            assertEquals(3, log.lines().size());
            assertTrue(log.lines().get(0).startsWith("[tick 3]"));
            assertTrue(log.lines().get(2).startsWith("[tick 5]"));
        }
        assertEquals(50, EventLogObserver.DEFAULT_CAPACITY);
    }

    @Test
    void closedObserverStopsReceivingAndReenablingStartsEmptyWithoutReplay() {
        EventLogObserver log = new EventLogObserver(bus, 50);
        bus.publish(new BehaviorChanged(1, 1, 1, "Chase"));
        log.close();
        log.close();
        bus.publish(new BehaviorChanged(1, 2, 1, "Flee"));
        assertEquals(1, log.lines().size());
        try (EventLogObserver again = new EventLogObserver(bus, 50)) {
            assertTrue(again.lines().isEmpty(), "no replay of missed events");
        }
    }

    @Test
    void observersAreIndependentOfEachOther() {
        HudObserver hud = new HudObserver(bus, Fixtures.player(10, 10));
        EventLogObserver log = new EventLogObserver(bus, 50);
        log.close();
        bus.publish(new ItemCollected(1, 1, 1, 2, 1));
        assertEquals(1, hud.score(), "HUD keeps working after the log is closed");
        hud.close();
    }

    @Test
    void twentyCreateCloseCyclesDoNotMultiplyDeliveries() {
        for (int cycle = 0; cycle < 20; cycle++) {
            HudObserver hud = new HudObserver(bus, Fixtures.player(10, 10));
            EventLogObserver log = new EventLogObserver(bus, 50);
            hud.close();
            log.close();
        }
        try (EventLogObserver fresh = new EventLogObserver(bus, 50)) {
            bus.publish(new BehaviorChanged(1, 1, 1, "Chase"));
            assertEquals(1, fresh.lines().size(), "no leftover subscriptions from earlier cycles");
        }
    }
}
