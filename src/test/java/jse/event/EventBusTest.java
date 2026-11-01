package jse.event;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    @Test
    void testExactClassDispatchAndSubscription() {
        var bus = new EventBus();
        List<EntitySpawnedEvent> received = new ArrayList<>();
        Subscription sub = bus.subscribe(EntitySpawnedEvent.class, received::add);

        EntitySpawnedEvent event = new EntitySpawnedEvent(1L, "Goblin");
        bus.publish(event);

        assertEquals(1, received.size());
        assertEquals("Goblin", received.get(0).kind());

        sub.close();
        bus.publish(new EntitySpawnedEvent(2L, "Orc"));
        assertEquals(1, received.size(), "Unsubscribed listener should not receive subsequent events");

        bus.close();
    }

    @Test
    void testDeferredPublicationDuringDispatch() {
        var bus = new EventBus();
        List<String> log = new ArrayList<>();

        bus.subscribe(EntitySpawnedEvent.class, event -> {
            log.add("spawn:" + event.entityId());
            bus.publish(new EntityDamagedEvent(event.entityId(), 10, 90));
        });

        bus.subscribe(EntityDamagedEvent.class, event -> log.add("damage:" + event.entityId()));

        bus.publish(new EntitySpawnedEvent(42L, "Boss"));

        assertEquals(2, log.size());
        assertEquals("spawn:42", log.get(0));
        assertEquals("damage:42", log.get(1));

        bus.close();
    }
}
