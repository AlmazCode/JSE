package jse.render.awt;

import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import jse.assets.AssetManager;
import jse.math.Vec2;
import jse.math.Rect;
import jse.math.Rgba;

public abstract class AbstractFrameRenderer implements FrameRenderer {
    protected final AssetManager assets;
    protected final RenderStyle style;
    private final Font font;
    private final BasicStroke stroke;
    private Graphics2D frame;

    protected AbstractFrameRenderer(AssetManager assets, RenderStyle style) {
        this.assets = Objects.requireNonNull(assets, "assets");
        this.style = Objects.requireNonNull(style, "style");
        font = new Font(style.fontFamily(), Font.PLAIN, style.fontSize());
        stroke = new BasicStroke(style.strokeWidth());
    }

    @Override
    public final void beginFrame(Graphics2D graphics, int width, int height) {
        if (frame != null)
            throw new IllegalStateException("A frame is already active");
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Frame dimensions must be positive");
        frame = Objects.requireNonNull(graphics, "graphics");
        frame.setFont(font);
        frame.setStroke(stroke);
        frame.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                style.antialias() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public final void endFrame() {
        frame = null;
    }

    protected final Graphics2D frame() {
        if (frame == null)
            throw new IllegalStateException("Drawing requires an active frame");
        return frame;
    }

    protected static Color color(Rgba value) {
        return new Color(value.red(), value.green(), value.blue(), value.alpha());
    }

    protected static Rectangle2D shape(Rect bounds) {
        return new Rectangle2D.Double(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    @Override
    public final void drawOutline(Rect bounds, Rgba color) {
        var graphics = frame();
        graphics.setColor(color(color));
        graphics.draw(shape(bounds));
    }

    @Override
    public final void drawText(String text, Vec2 position, Rgba color) {
        var graphics = frame();
        graphics.setColor(color(color));
        graphics.drawString(Objects.requireNonNull(text, "text"), (float) position.x(), (float) position.y());
    }
}
