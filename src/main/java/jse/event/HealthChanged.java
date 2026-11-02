package jse.event;

public record HealthChanged(long runId, long tick, long playerId, int oldHealth,
        int newHealth) implements GameEvent {
    public HealthChanged {
        if (runId <= 0 || tick < 0 || playerId <= 0 || oldHealth < 0 || newHealth < 0) {
            throw new IllegalArgumentException("Invalid health event");
        }
    }
}
