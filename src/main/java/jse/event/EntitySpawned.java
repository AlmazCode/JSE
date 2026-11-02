package jse.event;

import java.util.Objects;
import jse.world.ObjectKind;
import jse.world.ThemeId;

public record EntitySpawned(long runId, long tick, long entityId, ObjectKind category,
        ThemeId familyId) implements GameEvent {

    public EntitySpawned {
        if (runId <= 0 || tick < 0 || entityId <= 0) {
            throw new IllegalArgumentException("Invalid event identity");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(familyId, "familyId");
    }
}
