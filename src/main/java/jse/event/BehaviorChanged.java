package jse.event;

/**
 * Published when an enemy's strategy was really replaced.
 */
public record BehaviorChanged(long runId, long tick, long enemyId,
        String behaviorName) implements GameEvent {
    public BehaviorChanged {
        if (runId <= 0 || tick < 0 || enemyId <= 0) {
            throw new IllegalArgumentException("Invalid behavior event identity");
        }
        if (behaviorName == null || behaviorName.isBlank()) {
            throw new IllegalArgumentException("behaviorName must not be blank");
        }
    }
}
