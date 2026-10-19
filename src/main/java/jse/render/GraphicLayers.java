package jse.render;

import java.util.Objects;

public final class GraphicLayers {
    private GraphicLayers() {}

    public static Graphic compose(Graphic base, HealthView health, boolean outline, boolean healthBar,
            DecorationSettings settings) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(settings, "settings");
        if (base instanceof GraphicDecorator)
            throw new IllegalArgumentException("Compose layers from the undecorated base graphic");
        Graphic result = base;
        if (outline)
            result = new OutlineDecorator(result, settings);
        if (healthBar && health != null)
            result = new HealthBarDecorator(result, health, settings);
        return result;
    }
}
