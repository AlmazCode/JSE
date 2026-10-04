package jse.render;

import java.util.Objects;
import jse.math.Rect;

public abstract class Graphic {
    protected Renderer renderer;
    protected Graphic(Renderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public abstract void draw(Rect bounds);
}
