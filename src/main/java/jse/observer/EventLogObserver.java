package jse.observer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jse.event.BehaviorChanged;
import jse.event.EntityRemoved;
import jse.event.EntitySpawned;
import jse.event.EventBus;
import jse.event.GameFinished;
import jse.event.HealthChanged;
import jse.event.ItemCollected;
import jse.event.Subscription;

/**
 * Concrete observer keeping the latest log lines of six event types, oldest first.
 */
public final class EventLogObserver implements AutoCloseable {

    public static final int DEFAULT_CAPACITY = 50;
    private final int capacity;
    private final ArrayDeque<String> lines = new ArrayDeque<>();
    private final List<Subscription> subscriptions = new ArrayList<>();

    public EventLogObserver(EventBus events) {
        this(events, DEFAULT_CAPACITY);
    }

    public EventLogObserver(EventBus events, int capacity) {
        Objects.requireNonNull(events, "events");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        subscriptions.add(events.subscribe(EntitySpawned.class, e -> add(e.tick(),
                e.category() + " #" + e.entityId() + " spawned (" + e.familyId() + ")")));
        subscriptions.add(events.subscribe(EntityRemoved.class,
                e -> add(e.tick(), "#" + e.entityId() + " removed: " + e.reason())));
        subscriptions.add(events.subscribe(ItemCollected.class, e -> add(e.tick(),
                "Pickup #" + e.pickupId() + " collected, score " + e.newScore())));
        subscriptions.add(events.subscribe(HealthChanged.class,
                e -> add(e.tick(), "Player health " + e.oldHealth() + " -> " + e.newHealth())));
        subscriptions.add(events.subscribe(GameFinished.class,
                e -> add(e.tick(), "Game finished: " + e.result() + ", score " + e.finalScore())));
        subscriptions.add(events.subscribe(BehaviorChanged.class,
                e -> add(e.tick(), "Enemy #" + e.enemyId() + " behavior -> " + e.behaviorName())));
    }

    /**
     * Immutable copy of the current lines, oldest first.
     */
    public List<String> lines() {
        return List.copyOf(lines);
    }

    private void add(long tick, String text) {
        lines.addLast("[tick " + tick + "] " + text);
        while (lines.size() > capacity) {
            lines.removeFirst();
        }
    }

    @Override
    public void close() {
        subscriptions.forEach(Subscription::close);
        subscriptions.clear();
    }
}
