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

    /**
     * Compatibility with the initial AI interface; new clients use playerPosition().
     */
    public Vec2 getPlayerPosition() {
        return playerPosition.orElse(null);
    }

    public boolean isWithinBounds(Vec2 position) {
        return arena.contains(position);
    }
}
