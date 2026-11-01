
package jse.factory;

import java.util.Objects;
import jse.math.Vec2;

public record EnemySettings(Vec2 size, double speed) {

    public EnemySettings {
        Objects.requireNonNull(size, "Size cannot be null");

        if (size.x() <= 0 || size.y() <= 0) {
            throw new IllegalArgumentException(
                "Enemy size must be positive"
            );
        }

        if (!Double.isFinite(speed) || speed <= 0) {
            throw new IllegalArgumentException(
                "Enemy speed must be positive"
            );
        }
    }
}
