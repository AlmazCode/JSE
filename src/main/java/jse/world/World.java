package jse.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jse.event.EntityRemoved;
import jse.event.EntitySpawned;
import jse.event.EventBus;
import jse.event.GameEvent;
import jse.math.Rect;
import jse.math.Vec2;

public final class World implements AutoCloseable {
    private final long runId;
    private final Rect arena;
    private final ThemeId familyId;
    private final int capacity;
    private final EventBus events;
    private final Map<Long, GameObject> active = new LinkedHashMap<>();
    private final Map<Long, GameObject> pending = new LinkedHashMap<>();
    private final Map<Long, RemovalReason> removals = new LinkedHashMap<>();
    private long lastId;
    private boolean closed;

    public World(long runId, Rect arena, ThemeId familyId, int capacity, EventBus events) {
        if (runId <= 0 || capacity <= 0) {
            throw new IllegalArgumentException("World identity and capacity must be positive");
        }
        this.runId = runId;
        this.arena = Objects.requireNonNull(arena, "arena");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.capacity = capacity;
        this.events = Objects.requireNonNull(events, "events");
    }

    public long runId() {
        return runId;
    }

    public Rect arena() {
        return arena;
    }

    public ThemeId familyId() {
        return familyId;
    }

    public int sizeIncludingPending() {
        return (int) active.values().stream().filter(this::available).count() + pending.size();
    }

    public void enqueueAdd(GameObject object) {
        requireOpen();
        Objects.requireNonNull(object, "object");
        if (object.id() != 0 || object.active() || object.pendingRemoval()
                || object.familyId() != familyId) {
            throw new IllegalArgumentException(
                    "Object must be unregistered and belong to the world's family");
        }
        var bounds = object.bounds();
        if (!arena.contains(bounds.position())
                || !arena.contains(new Vec2(bounds.right(), bounds.bottom()))) {
            throw new IllegalArgumentException("Complete object bounds must fit inside the arena");
        }
        if (sizeIncludingPending() >= capacity) {
            throw new IllegalStateException("World capacity reached");
        }
        if (lastId == Long.MAX_VALUE) {
            throw new IllegalStateException("World IDs exhausted");
        }
        object.assignId(++lastId);
        pending.put(object.id(), object);
    }

    public void enqueueRemove(long entityId, RemovalReason reason) {
        requireOpen();
        Objects.requireNonNull(reason, "reason");
        var queued = pending.remove(entityId);
        if (queued != null) {
            queued.markPendingRemoval();
            return;
        }
        var object = active.get(entityId);
        if (object != null && available(object)) {
            object.markPendingRemoval();
            removals.put(entityId, reason);
        }
    }

    public void commitChanges(long tick) {
        requireOpen();
        if (tick < 0) {
            throw new IllegalArgumentException("Tick must be nonnegative");
        }
        var notifications = new ArrayList<GameEvent>();
        for (var entry : removals.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            var object = active.remove(entry.getKey());
            object.setActive(false);
            notifications.add(new EntityRemoved(runId, tick, object.id(), entry.getValue()));
        }
        removals.clear();
        for (var object : pending.values().stream().sorted(Comparator.comparingLong(GameObject::id))
                .toList()) {
            object.setActive(true);
            active.put(object.id(), object);
            notifications.add(new EntitySpawned(runId, tick, object.id(), object.category(),
                    object.familyId()));
        }
        pending.clear();
        // Publish after collection mutation: callback changes belong to the next commit.
        for (var event : notifications) {
            events.publish(event);
        }
    }

    public Optional<GameObject> findActive(long id) {
        return Optional.ofNullable(active.get(id)).filter(this::available);
    }

    public void forEachActive(Consumer<GameObject> action) {
        Objects.requireNonNull(action, "action");
        for (var object : List.copyOf(active.values())) {
            if (available(object)) {
                action.accept(object);
            }
        }
    }

    public WorldView view() {
        return snapshot(active.values().stream().filter(this::available));
    }

    public WorldView spawnView() {
        return snapshot(Stream.concat(active.values().stream().filter(this::available),
                pending.values().stream()));
    }

    private WorldView snapshot(Stream<GameObject> objects) {
        var entities = objects.sorted(Comparator.comparingLong(GameObject::id))
                .map(object -> new EntityView(object.id(), object.category(), object.familyId(),
                        object.position(), object.bounds()))
                .toList();
        var player = entities.stream().filter(object -> object.category() == ObjectKind.PLAYER)
                .map(EntityView::position).findFirst();
        return new WorldView(entities, player, arena);
    }

    private boolean available(GameObject object) {
        return object.active() && !object.pendingRemoval();
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("World is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (var object : active.values()) {
            object.setActive(false);
        }
        active.clear();
        pending.clear();
        removals.clear();
    }
}
