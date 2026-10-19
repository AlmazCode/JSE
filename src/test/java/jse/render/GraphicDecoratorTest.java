package jse.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import jse.assets.AssetManager;
import jse.assets.MissingSpriteStyle;
import jse.assets.SpriteId;
import jse.math.Rect;
import jse.math.Rgba;
import jse.math.Vec2;
import jse.render.awt.FilledRenderer;
import jse.render.awt.RenderStyle;
import jse.render.awt.WireframeRenderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GraphicDecoratorTest {
    private static final Rgba WHITE = Rgba.parse("#FFFFFF"), RED = Rgba.parse("#FF0000"),
            GREEN = Rgba.parse("#00FF00"), DARK = Rgba.parse("#222222");
    private final DecorationSettings style = new DecorationSettings(2, RED, 6, 4, GREEN, DARK);
    private static final Rect BOUNDS = new Rect(10, 20, 30, 20);

    @Test
    void layersDrawAfterTheBaseAndReadLiveClampedHealth() {
        var renderer = new RecordingRenderer();
        var health = new MutableHealth(2, 3);
        var graphic = GraphicLayers.compose(new RectangleGraphic(renderer, WHITE), health, true, true, style);
        graphic.draw(BOUNDS);
        assertEquals(7, renderer.rectangles.size());
        assertEquals(BOUNDS, renderer.rectangles.get(0));
        assertEquals(List.of(WHITE, RED, RED, RED, RED, DARK, GREEN), renderer.colors);
        assertEquals(new Rect(10, 10, 30, 4), renderer.rectangles.get(5));
        assertEquals(new Rect(10, 10, 20, 4), renderer.rectangles.get(6));
        health.current = -5;
        renderer.clear();
        graphic.draw(BOUNDS);
        assertEquals(6, renderer.rectangles.size());
        health.current = 100;
        renderer.clear();
        graphic.draw(BOUNDS);
        assertEquals(new Rect(10, 10, 30, 4), renderer.rectangles.get(6));
        health.max = 0;
        assertThrows(IllegalArgumentException.class, () -> graphic.draw(BOUNDS));
    }

    @Test
    void independentLayersAndObjectsWithoutHealthAreSupported() {
        var renderer = new RecordingRenderer();
        var base = new RectangleGraphic(renderer, WHITE);
        GraphicLayers.compose(base, null, false, true, style).draw(BOUNDS);
        assertEquals(List.of(BOUNDS), renderer.rectangles);
        renderer.clear();
        new OutlineDecorator(base, style).draw(BOUNDS);
        assertEquals(5, renderer.rectangles.size());
        renderer.clear();
        new HealthBarDecorator(base, new MutableHealth(2, 3), style).draw(BOUNDS);
        assertEquals(3, renderer.rectangles.size());
    }

    @Test
    void rendererReplacementChangesRealPixelsAcrossEveryLayer() {
        var assets = new AssetManager(getClass().getClassLoader(), new MissingSpriteStyle(8, 8, WHITE, RED));
        var renderStyle = new RenderStyle("Dialog", 12, 1, WHITE, false);
        var filled = new FilledRenderer(assets, renderStyle);
        var wire = new WireframeRenderer(assets, renderStyle);
        var graphic = GraphicLayers.compose(new RectangleGraphic(filled, WHITE), new MutableHealth(2, 3),
                true, true, style);
        var image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        filled.beginFrame(graphics, 60, 60);
        graphic.draw(BOUNDS);
        filled.endFrame();
        graphics.dispose();
        assertEquals(0xffffffff, image.getRGB(25, 30));
        assertEquals(0xffff0000, image.getRGB(8, 25));
        assertEquals(0xff00ff00, image.getRGB(15, 12));
        assertEquals(0xff222222, image.getRGB(35, 12));
        graphic.setRenderer(wire);
        image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        wire.beginFrame(graphics, 60, 60);
        graphic.draw(BOUNDS);
        wire.endFrame();
        graphics.dispose();
        assertEquals(0, image.getRGB(25, 30));
        assertEquals(0xffff0000, image.getRGB(8, 25));
        assertEquals(0xff00ff00, image.getRGB(15, 10));
    }

    @Test
    void repeatedTogglesAlwaysComposeFromTheUndecoratedBase() {
        var renderer = new RecordingRenderer();
        var base = new RectangleGraphic(renderer, WHITE);
        for (int i = 0; i < 200; i++) {
            var graphic = GraphicLayers.compose(base, new MutableHealth(1, 3), i % 2 == 0, i % 3 == 0, style);
            renderer.clear();
            graphic.draw(BOUNDS);
            assertEquals(1 + (i % 2 == 0 ? 4 : 0) + (i % 3 == 0 ? 2 : 0), renderer.rectangles.size());
        }
        assertSame(base, GraphicLayers.compose(base, null, false, false, style));
        var decorated = new OutlineDecorator(base, style);
        assertThrows(IllegalArgumentException.class,
                () -> GraphicLayers.compose(decorated, null, true, false, style));
    }

    @Test
    void invalidStyleAndHealthAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DecorationSettings(0, RED, 6, 4, GREEN, DARK));
        assertThrows(IllegalArgumentException.class, () -> new DecorationSettings(Double.NaN, RED, 6, 4, GREEN, DARK));
        assertThrows(IllegalArgumentException.class, () -> new DecorationSettings(2, RED, -1, 4, GREEN, DARK));
        assertThrows(IllegalArgumentException.class, () -> new DecorationSettings(2, RED, 6, 0, GREEN, DARK));
        assertThrows(NullPointerException.class, () -> new DecorationSettings(2, null, 6, 4, GREEN, DARK));
        var base = new RectangleGraphic(new RecordingRenderer(), WHITE);
        assertThrows(IllegalArgumentException.class, () -> new HealthBarDecorator(base, new MutableHealth(1, 0), style));
        assertThrows(NullPointerException.class, () -> new HealthBarDecorator(base, null, style));
    }

    private static class MutableHealth implements HealthView {
        int current, max;
        MutableHealth(int current, int max) { this.current = current; this.max = max; }
        public int currentHealth() { return current; }
        public int maxHealth() { return max; }
    }
    private static class RecordingRenderer implements Renderer {
        final List<Rect> rectangles = new ArrayList<>();
        final List<Rgba> colors = new ArrayList<>();
        void clear() { rectangles.clear(); colors.clear(); }
        public void drawRectangle(Rect bounds, Rgba color) { rectangles.add(bounds); colors.add(color); }
        public void drawSprite(SpriteId id, Rect bounds) { fail("No sprites expected"); }
        public void drawOutline(Rect bounds, Rgba color) { fail("Outline uses bands, keeping width in layer settings"); }
        public void drawText(String text, Vec2 position, Rgba color) { fail("No text expected"); }
    }
}
