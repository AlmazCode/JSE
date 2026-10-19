package jse.render;

import java.util.Objects;
import jse.math.Rect;

public final class HealthBarDecorator extends GraphicDecorator {
    private final HealthView health;
    private final DecorationSettings settings;

    public HealthBarDecorator(Graphic wrapped, HealthView health, DecorationSettings settings) {
        super(wrapped);
        this.health = Objects.requireNonNull(health, "health");
        this.settings = Objects.requireNonNull(settings, "settings");
        requireMaxHealth();
    }

    private int requireMaxHealth() {
        int max = health.maxHealth();
        if (max <= 0)
            throw new IllegalArgumentException("Maximum health must be positive");
        return max;
    }

    @Override
    protected void drawLayer(Rect bounds) {
        int max = requireMaxHealth();
        double ratio = Math.max(0, Math.min(1, (double) health.currentHealth() / max));
        double y = bounds.y() - settings.barOffset() - settings.barHeight();
        renderer.drawRectangle(new Rect(bounds.x(), y, bounds.width(), settings.barHeight()),
                settings.barBackground());
        if (ratio > 0)
            renderer.drawRectangle(new Rect(bounds.x(), y, bounds.width() * ratio, settings.barHeight()),
                    settings.barFill());
    }
}
