
package jse.factory;

import java.util.Objects;
import jse.math.Vec2;

public record SpawnRequest(Vec2 position, Vec2 size) {

    public SpawnRequest {
        Objects.requireNonNull(position, "Position cannot be null");
        Objects.requireNonNull(size, "Size cannot be null");

        if (size.x() <= 0 || size.y() <= 0) {
            throw new IllegalArgumentException(
                "Object size must be positive"
            );
        }
    }
}
