package jse.event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Delivers exact event types synchronously on the simulation thread. Publications made by a
 * listener are queued until the current event has reached its subscription snapshot. This bus is
 * not thread-safe.
 */
public final class EventBus implements AutoCloseable {
    private final Map<Class<? extends GameEvent>, List<EventListener<?>>> listeners = new HashMap<>();
    private final Queue<GameEvent> deferredQueue = new ArrayDeque<>();
    private boolean dispatching;
    private boolean closed;

    /**
     * Subscribes a listener to an exact event class.
     *
     * @param eventClass the exact type of GameEvent
     * @param listener   consumer of the event
     * @param <E>        event type
     * @return Subscription handle for unregistering
     */
    public <E extends GameEvent> Subscription subscribe(Class<E> eventClass,
            EventListener<E> listener) {
        Objects.requireNonNull(eventClass, "eventClass cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");
        if (closed) {
            throw new IllegalStateException("EventBus is closed");
        }
        List<EventListener<?>> list = listeners.computeIfAbsent(eventClass, k -> new ArrayList<>());
        list.add(listener);
        return () -> list.remove(listener);
    }

    /**
     * Publishes an event immediately if not dispatching, or enqueues it if dispatch is in progress.
     */
    public void publish(GameEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        if (closed) {
            return;
        }
        if (dispatching) {
            deferredQueue.offer(event);
            return;
        }
        dispatchDirect(event);
        drainDeferred();
    }

    /**
     * Explicitly dispatches all queued deferred events.
     */
    public void drainDeferred() {
        if (closed || dispatching) {
            return;
        }
        dispatching = true;
        try {
            while (!deferredQueue.isEmpty()) {
                GameEvent event = deferredQueue.poll();
                if (event != null) {
                    dispatchToListeners(event);
                }
            }
        } finally {
            dispatching = false;
        }
    }

    private void dispatchDirect(GameEvent event) {
        dispatching = true;
        try {
            dispatchToListeners(event);
        } finally {
            dispatching = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchToListeners(GameEvent event) {
        List<EventListener<?>> targets = listeners.get(event.getClass());
        if (targets == null || targets.isEmpty()) {
            return;
        }
        // Subscription changes take effect on the next event, not halfway through this one.
        for (EventListener<?> listener : targets.toArray(new EventListener<?>[0])) {
            try {
                ((EventListener<GameEvent>) listener).onEvent(event);
            } catch (Exception ex) {
                System.err.println("Error dispatching event " + event.getClass().getSimpleName()
                        + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        listeners.clear();
        deferredQueue.clear();
    }

    public boolean isClosed() {
        return closed;
    }
}
