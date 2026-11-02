package jse.render;

import java.util.Objects;
import jse.math.Rect;

public abstract class GraphicDecorator extends Graphic {
    protected final Graphic wrapped;

    protected GraphicDecorator(Graphic wrapped) {
        super(Objects.requireNonNull(wrapped, "wrapped").renderer);
        this.wrapped = wrapped;
    }

    @Override
    public final void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);
        wrapped.setRenderer(renderer);
    }

    @Override
    public final void draw(Rect bounds) {
        Objects.requireNonNull(bounds, "bounds");
        wrapped.draw(bounds);
        drawLayer(bounds);
    }

    protected abstract void drawLayer(Rect bounds);
}
