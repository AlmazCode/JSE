package jse.render.awt;

import java.awt.geom.Line2D;
import jse.assets.SpriteId;
import jse.assets.AssetManager;
import jse.math.Rect;
import jse.math.Rgba;
import jse.math.Vec2;

public final class WireframeRenderer extends AbstractFrameRenderer {
    private final boolean spriteLabels;
    public WireframeRenderer(AssetManager assets, RenderStyle style) {
        this(assets, style, true);
    }
    public WireframeRenderer(AssetManager assets, RenderStyle style, boolean spriteLabels) {
        super(assets, style);
        this.spriteLabels = spriteLabels;
    }

    @Override
    public void drawSprite(SpriteId sprite, Rect bounds) {
        drawOutline(bounds, style.wireframeColor());
        var graphics = frame();
        graphics.draw(new Line2D.Double(bounds.x(), bounds.y(), bounds.right(), bounds.bottom()));
        graphics.draw(new Line2D.Double(bounds.right(), bounds.y(), bounds.x(), bounds.bottom()));
        if (spriteLabels) drawText(sprite.path(), new Vec2(bounds.x(), bounds.bottom() + style.fontSize()), style.wireframeColor());
    }

    @Override
    public void drawRectangle(Rect bounds, Rgba color) {
        drawOutline(bounds, color);
    }
}
