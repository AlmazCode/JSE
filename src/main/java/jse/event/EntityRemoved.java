package jse.event;

import java.util.Objects;
import jse.world.RemovalReason;

public record EntityRemoved(long runId, long tick, long entityId,
        RemovalReason reason) implements GameEvent {
    public EntityRemoved {
        if (runId <= 0 || tick < 0 || entityId <= 0) {
            throw new IllegalArgumentException("Invalid event identity");
        }
        Objects.requireNonNull(reason, "reason");
    }
}
