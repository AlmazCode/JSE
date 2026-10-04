package jse.platform.awt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import jse.core.EngineConfig;
import jse.assets.AssetManager;
import jse.assets.MissingSpriteStyle;
import jse.math.Rect;
import jse.math.Rgba;
import jse.render.awt.RenderStyle;
import jse.render.awt.FilledRenderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GamePanelTest {
    @Test
    void paintFailureEndsFrameBeforeReportingAndPreservesOriginalGraphics() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var white = new Rgba(255, 255, 255, 255);
            var assets = new AssetManager(getClass().getClassLoader(), new MissingSpriteStyle(8, 8, white, white));
            var renderer = new FilledRenderer(assets, new RenderStyle("Dialog", 12, 1, white, false));
            int[] errors = {0};
            var panel = new GamePanel(new EngineConfig(100, 80, 60, 5, 0.25, 16), white,
                    ()
                            -> renderer,
                    target
                    -> {
                        target.drawRectangle(new Rect(1, 1, 5, 5), white);
                        throw new IllegalStateException("paint failure");
                    },
                    failure -> {
                        errors[0]++;
                        assertThrows(
                                IllegalStateException.class, () -> renderer.drawOutline(new Rect(1, 1, 5, 5), white));
                    });
            panel.setSize(100, 80);
            var image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            graphics.setColor(Color.RED);
            var transform = graphics.getTransform();
            var font = graphics.getFont();
            panel.paintComponent(graphics);
            assertEquals(1, errors[0]);
            assertEquals(Color.RED, graphics.getColor());
            assertEquals(transform, graphics.getTransform());
            assertEquals(font, graphics.getFont());
            graphics.dispose();
        });
    }
}
