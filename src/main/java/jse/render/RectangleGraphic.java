package jse.render;

import java.util.Objects;
import jse.math.Rect;
import jse.math.Rgba;

public final class RectangleGraphic extends Graphic {
    private final Rgba color;
    public RectangleGraphic(Renderer renderer, Rgba color) {
        super(renderer);
        this.color = Objects.requireNonNull(color, "color");
    }

    @Override
    public void draw(Rect bounds) {
        renderer.drawRectangle(bounds, color);
    }
}
