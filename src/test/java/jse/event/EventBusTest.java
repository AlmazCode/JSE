package jse.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import jse.world.ObjectKind;
import jse.world.ThemeId;
import org.junit.jupiter.api.Test;

class EventBusTest {
    @Test
    void testExactClassDispatchAndSubscription() {
        var bus = new EventBus();
        List<EntitySpawned> received = new ArrayList<>();
        Subscription sub = bus.subscribe(EntitySpawned.class, received::add);
        EntitySpawned event = new EntitySpawned(1, 0, 1, ObjectKind.ENEMY, ThemeId.FOREST);
        bus.publish(event);
        assertEquals(1, received.size());
        assertEquals(ObjectKind.ENEMY, received.get(0).category());
        bus.publish(new HealthChanged(1, 1, 1, 3, 2));
        assertEquals(1, received.size(), "Other exact event types must not reach this listener");
        sub.close();
        bus.publish(new EntitySpawned(1, 2, 2, ObjectKind.ENEMY, ThemeId.FOREST));
        assertEquals(1, received.size(),
                "Unsubscribed listener should not receive subsequent events");
        bus.close();
    }

    @Test
    void testDeferredPublicationDuringDispatch() {
        var bus = new EventBus();
        List<String> log = new ArrayList<>();
        bus.subscribe(EntitySpawned.class, event -> {
            log.add("spawn:" + event.entityId());
            bus.publish(new HealthChanged(1, 1, event.entityId(), 3, 2));
        });
        bus.subscribe(EntitySpawned.class, event -> log.add("spawn-observer:" + event.entityId()));
        bus.subscribe(HealthChanged.class, event -> log.add("damage:" + event.playerId()));
        bus.publish(new EntitySpawned(1, 0, 42, ObjectKind.ENEMY, ThemeId.FOREST));
        assertEquals(List.of("spawn:42", "spawn-observer:42", "damage:42"), log);
        bus.close();
    }

    @Test
    void subscriptionChangesApplyToTheNextEvent() {
        var bus = new EventBus();
        var calls = new ArrayList<String>();
        var second = new AtomicReference<Subscription>();
        bus.subscribe(EntitySpawned.class, event -> {
            calls.add("first:" + event.entityId());
            second.get().close();
            if (event.entityId() == 1) {
                bus.subscribe(EntitySpawned.class, next -> calls.add("late:" + next.entityId()));
            }
        });
        second.set(bus.subscribe(EntitySpawned.class,
                event -> calls.add("second:" + event.entityId())));

        bus.publish(new EntitySpawned(1, 0, 1, ObjectKind.ENEMY, ThemeId.FOREST));
        bus.publish(new EntitySpawned(1, 1, 2, ObjectKind.ENEMY, ThemeId.FOREST));

        assertEquals(List.of("first:1", "second:1", "first:2", "late:2"), calls);
        bus.close();
    }
}
