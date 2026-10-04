package jse.render.awt;

import java.util.Objects;
import jse.math.Rgba;

public record RenderStyle(String fontFamily, int fontSize, float strokeWidth, Rgba wireframeColor, boolean antialias) {
    public RenderStyle {
        Objects.requireNonNull(fontFamily, "fontFamily");
        Objects.requireNonNull(wireframeColor, "wireframeColor");
        if (fontFamily.isBlank() || fontSize <= 0 || !Float.isFinite(strokeWidth) || strokeWidth <= 0)
            throw new IllegalArgumentException("Font size and stroke width must be positive");
    }
}
