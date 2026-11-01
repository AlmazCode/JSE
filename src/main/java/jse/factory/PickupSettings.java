
package jse.factory;

import java.util.Objects;
import jse.math.Vec2;

public record PickupSettings(Vec2 size, int value) {

    public PickupSettings {
        Objects.requireNonNull(size, "Size cannot be null");

        if (size.x() <= 0 || size.y() <= 0) {
            throw new IllegalArgumentException(
                "Pickup size must be positive"
            );
        }

        if (value <= 0) {
            throw new IllegalArgumentException(
                "Pickup value must be positive"
            );
        }
    }
}
