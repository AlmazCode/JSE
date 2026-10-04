package jse.demo;

import java.util.Objects;
import jse.core.EngineConfig;
import jse.math.Vec2;
import jse.math.Rect;
import jse.math.Rgba;

public record PreviewSettings(Vec2 objectSize, double speed, double padding, double headerHeight, double lineHeight,
        Rgba objectColor, Rgba textColor, Rgba mutedColor) {
    public PreviewSettings {
        Objects.requireNonNull(objectSize, "objectSize");
        Objects.requireNonNull(objectColor, "objectColor");
        Objects.requireNonNull(textColor, "textColor");
        Objects.requireNonNull(mutedColor, "mutedColor");
        if (objectSize.x() <= 0 || objectSize.y() <= 0 || !Double.isFinite(speed) || speed <= 0
                || !Double.isFinite(padding) || padding < 0 || !Double.isFinite(headerHeight)
                || !Double.isFinite(lineHeight) || lineHeight <= 0 || headerHeight < lineHeight * 4 + padding)
            throw new IllegalArgumentException("Preview dimensions and speed must be valid");
    }

    public Rect area(EngineConfig config) {
        var area =
                new Rect(padding, headerHeight, config.width() - padding * 2, config.height() - headerHeight - padding);
        area.clampPosition(area.position(), objectSize);
        return area;
    }
}
