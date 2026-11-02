package jse.world;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jse.math.Rect;
import jse.math.Vec2;

public record WorldView(List<EntityView> entities, Optional<Vec2> playerPosition, Rect arena) {
    public WorldView {
        entities = List.copyOf(entities);
        Objects.requireNonNull(playerPosition, "playerPosition");
        Objects.requireNonNull(arena, "arena");
    }
}
