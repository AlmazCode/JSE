package jse.event;

/**
 * Handle allowing safe unsubscription from an EventBus.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
