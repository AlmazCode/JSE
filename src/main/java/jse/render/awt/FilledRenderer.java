package jse.render.awt;

import java.awt.geom.AffineTransform;
import jse.assets.SpriteId;
import jse.assets.AssetManager;
import jse.math.Rect;
import jse.math.Rgba;

public final class FilledRenderer extends AbstractFrameRenderer {
    public FilledRenderer(AssetManager assets, RenderStyle style) {
        super(assets, style);
    }

    @Override
    public void drawSprite(SpriteId sprite, Rect bounds) {
        var graphics = frame();
        var image = assets.get(sprite);
        var transform = AffineTransform.getTranslateInstance(bounds.x(), bounds.y());
        transform.scale(bounds.width() / image.getWidth(), bounds.height() / image.getHeight());
        graphics.drawImage(image, transform, null);
    }

    @Override
    public void drawRectangle(Rect bounds, Rgba color) {
        var graphics = frame();
        graphics.setColor(color(color));
        graphics.fill(shape(bounds));
    }
}
