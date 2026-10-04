package jse.render;

import java.util.Objects;
import jse.assets.SpriteId;
import jse.math.Rect;

public final class SpriteGraphic extends Graphic {
    private final SpriteId sprite;
    public SpriteGraphic(Renderer renderer, SpriteId sprite) {
        super(renderer);
        this.sprite = Objects.requireNonNull(sprite, "sprite");
    }

    @Override
    public void draw(Rect bounds) {
        renderer.drawSprite(sprite, bounds);
    }
}
