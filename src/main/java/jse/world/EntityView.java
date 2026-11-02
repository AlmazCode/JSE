package jse.world;

import java.util.Objects;
import jse.math.Rect;
import jse.math.Vec2;

public record EntityView(long id, ObjectKind category, ThemeId familyId, Vec2 position,
        Rect bounds) {

    public EntityView {
        if (id <= 0) {
            throw new IllegalArgumentException("Entity ID must be positive");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(bounds, "bounds");
    }
}
