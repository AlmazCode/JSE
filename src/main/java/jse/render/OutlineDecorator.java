package jse.render;

import java.util.Objects;
import jse.math.Rect;

public final class OutlineDecorator extends GraphicDecorator {
    private final DecorationSettings settings;

    public OutlineDecorator(Graphic wrapped, DecorationSettings settings) {
        super(wrapped);
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    protected void drawLayer(Rect bounds) {
        double width = settings.outlineWidth();
        var color = settings.outlineColor();
        renderer.drawRectangle(new Rect(bounds.x() - width, bounds.y() - width,
                bounds.width() + width * 2, width), color);
        renderer.drawRectangle(new Rect(bounds.x() - width, bounds.bottom(),
                bounds.width() + width * 2, width), color);
        renderer.drawRectangle(new Rect(bounds.x() - width, bounds.y(), width, bounds.height()), color);
        renderer.drawRectangle(new Rect(bounds.right(), bounds.y(), width, bounds.height()), color);
    }
}
