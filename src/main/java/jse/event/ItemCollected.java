package jse.event;

public record ItemCollected(long runId, long tick, long playerId, long pickupId,
        int newScore) implements GameEvent {

    public ItemCollected {
        if (runId <= 0 || tick < 0 || playerId <= 0 || pickupId <= 0 || newScore < 0) {
            throw new IllegalArgumentException("Invalid collection event");
        }
    }
}
