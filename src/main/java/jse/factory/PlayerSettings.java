
package jse.factory;

import java.util.Objects;
import jse.math.Vec2;

public record PlayerSettings(Vec2 size, double speed, int maxHealth) {

    public PlayerSettings {
        Objects.requireNonNull(size, "Size cannot be null");

        if (size.x() <= 0 || size.y() <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }

        if (!Double.isFinite(speed) || speed <= 0) {
            throw new IllegalArgumentException("Speed must be positive");
        }

        if (maxHealth <= 0) {
            throw new IllegalArgumentException("Max health must be positive");
        }
    }
}
