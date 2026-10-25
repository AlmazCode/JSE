package jse.event;

import java.util.Objects;

public record EntitySpawnedEvent(long entityId, String kind) implements GameEvent {
    public EntitySpawnedEvent {
        Objects.requireNonNull(kind, "kind cannot be null");
    }
}
