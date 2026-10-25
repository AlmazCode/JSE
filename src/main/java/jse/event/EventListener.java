package jse.event;

@FunctionalInterface
public interface EventListener<T extends GameEvent> {
    void onEvent(T event);
}
