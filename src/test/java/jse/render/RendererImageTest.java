package jse.render;

import java.awt.image.BufferedImage;
import jse.assets.SpriteId;
import jse.assets.AssetManager;
import jse.assets.MissingSpriteStyle;
import jse.math.Rect;
import jse.math.Rgba;
import jse.render.awt.FrameRenderer;
import jse.render.awt.RenderStyle;
import jse.render.awt.FilledRenderer;
import jse.render.awt.WireframeRenderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RendererImageTest {
    private static final Rgba WHITE = new Rgba(255, 255, 255, 255);
    private FrameRenderer renderer(boolean filled) {
        var assets = new AssetManager(
                getClass().getClassLoader(), new MissingSpriteStyle(8, 8, WHITE, new Rgba(0, 0, 0, 255)));
        var style = new RenderStyle("Dialog", 12, 1, WHITE, false);
        return filled ? new FilledRenderer(assets, style) : new WireframeRenderer(assets, style);
    }
    @Test
    void bridgeReplacementChangesPixelsWithoutChangingBounds() {
        var filledImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        var wireImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        var filled = renderer(true);
        var wire = renderer(false);
        var graphic = new RectangleGraphic(filled, WHITE);
        var bounds = new Rect(5, 5, 20, 20);
        var graphics = filledImage.createGraphics();
        filled.beginFrame(graphics, 40, 40);
        graphic.draw(bounds);
        filled.endFrame();
        graphics.dispose();
        graphic.setRenderer(wire);
        graphics = wireImage.createGraphics();
        wire.beginFrame(graphics, 40, 40);
        graphic.draw(bounds);
        wire.endFrame();
        graphics.dispose();
        assertEquals(0xffffffff, filledImage.getRGB(15, 15));
        assertEquals(0, wireImage.getRGB(15, 15));
        assertEquals(0xffffffff, wireImage.getRGB(5, 10));
        assertThrows(IllegalStateException.class, () -> graphic.draw(bounds));
    }
    @Test
    void frameCannotBeReenteredAndSpriteProducesPixelsInBothModes() {
        for (boolean filled : new boolean[] {true, false}) {
            var image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            var renderer = renderer(filled);
            renderer.beginFrame(graphics, 60, 60);
            assertThrows(IllegalStateException.class, () -> renderer.beginFrame(graphics, 60, 60));
            new SpriteGraphic(renderer, new SpriteId("missing.png")).draw(new Rect(5, 5, 20, 20));
            renderer.endFrame();
            renderer.endFrame();
            graphics.dispose();
            assertNotEquals(0, image.getRGB(5, 10));
        }
    }
}
