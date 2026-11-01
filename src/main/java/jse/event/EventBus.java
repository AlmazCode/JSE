package jse.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer pattern implementation: Event bus supporting exact-class dispatch,
 * deferred publication during event dispatching, and deterministic subscription cleanup.
 */
public final class EventBus implements AutoCloseable {

    private final Map<Class<? extends GameEvent>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();
    private final Queue<GameEvent> deferredQueue = new ArrayDeque<>();
    private boolean isDispatching = false;
    private boolean closed = false;

    /**
     * Subscribes a listener to an exact event class.
     *
     * @param eventClass the exact type of GameEvent
     * @param listener   consumer of the event
     * @param <E>        event type
     * @return Subscription handle for unregistering
     */
    public <E extends GameEvent> Subscription subscribe(Class<E> eventClass, EventListener<E> listener) {
        Objects.requireNonNull(eventClass, "eventClass cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        if (closed) {
            throw new IllegalStateException("EventBus is closed");
        }

        List<EventListener<?>> list = listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
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

        if (isDispatching) {
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
        if (closed || isDispatching) {
            return;
        }

        isDispatching = true;
        try {
            while (!deferredQueue.isEmpty()) {
                GameEvent event = deferredQueue.poll();
                if (event != null) {
                    dispatchToListeners(event);
                }
            }
        } finally {
            isDispatching = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchDirect(GameEvent event) {
        isDispatching = true;
        try {
            dispatchToListeners(event);
        } finally {
            isDispatching = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchToListeners(GameEvent event) {
        List<EventListener<?>> targets = listeners.get(event.getClass());
        if (targets == null || targets.isEmpty()) {
            return;
        }

        // Snapshot iteration to prevent concurrent modification and ensure deterministic execution
        for (EventListener<?> listener : targets.toArray(new EventListener<?>[0])) {
            try {
                ((EventListener<GameEvent>) listener).onEvent(event);
            } catch (Exception ex) {
                System.err.println("Error dispatching event " + event.getClass().getSimpleName() + ": " + ex.getMessage());
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
