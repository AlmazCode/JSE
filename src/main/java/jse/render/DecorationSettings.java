package jse.render;

import java.util.Objects;
import jse.math.Rgba;

public record DecorationSettings(double outlineWidth, Rgba outlineColor, double barOffset, double barHeight,
        Rgba barFill, Rgba barBackground) {
    public DecorationSettings {
        Objects.requireNonNull(outlineColor, "outlineColor");
        Objects.requireNonNull(barFill, "barFill");
        Objects.requireNonNull(barBackground, "barBackground");
        if (!Double.isFinite(outlineWidth) || outlineWidth <= 0 || !Double.isFinite(barOffset) || barOffset < 0
                || !Double.isFinite(barHeight) || barHeight <= 0)
            throw new IllegalArgumentException("Decoration dimensions must be finite, with positive widths and heights");
    }
}
