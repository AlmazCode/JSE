package jse.assets;

import java.util.Objects;
import jse.math.Rgba;

public record MissingSpriteStyle(int width, int height, Rgba background, Rgba mark) {
    public MissingSpriteStyle {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Placeholder dimensions must be positive");
        Objects.requireNonNull(background, "background");
        Objects.requireNonNull(mark, "mark");
    }
}
