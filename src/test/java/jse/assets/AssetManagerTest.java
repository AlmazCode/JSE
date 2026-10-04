package jse.assets;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import jse.math.Rgba;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssetManagerTest {
    @Test
    void cacheLookupDoesNotEmitDiagnosticsDuringDrawing() {
        var logger = java.util.logging.Logger.getLogger(AssetManager.class.getName());
        boolean parents = logger.getUseParentHandlers();
        var records = new java.util.ArrayList<java.util.logging.LogRecord>();
        var handler = new java.util.logging.Handler() {
            public void publish(java.util.logging.LogRecord record) {
                records.add(record);
            }
            public void flush() {}
            public void close() {}
        };
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            var manager = new AssetManager(new ClassLoader(null) {},
                    new MissingSpriteStyle(8, 8, new Rgba(255, 0, 255, 255), new Rgba(0, 0, 0, 255)));
            var missing = new SpriteId("unprepared.png");
            manager.get(missing);
            assertTrue(records.isEmpty(), "Cache lookup must not log during painting");
            manager.preload(List.of(missing, missing));
            assertEquals(1, records.size(), "Missing-resource diagnostics belong to preload");
            manager.get(missing);
            assertEquals(1, records.size());
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(parents);
        }
    }
    @Test
    void drawingDoesNotReadResourcesAndPreloadReadsEachResourceOnce() throws Exception {
        var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 1, 0xff123456);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        int[] reads = {0};
        var loader = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                reads[0]++;
                return name.equals("test.png") ? new ByteArrayInputStream(bytes.toByteArray()) : null;
            }
        };
        var manager = new AssetManager(
                loader, new MissingSpriteStyle(8, 8, new Rgba(255, 0, 255, 255), new Rgba(0, 0, 0, 255)));
        var id = new SpriteId("test.png");
        assertEquals(8, manager.get(id).getWidth());
        assertEquals(0, reads[0]);
        manager.preload(List.of(id, id));
        assertEquals(1, reads[0]);
        assertEquals(0xff123456, manager.get(id).getRGB(1, 1));
        manager.preload(List.of(id));
        assertEquals(1, reads[0]);
        var missing = new SpriteId("missing.png");
        manager.preload(List.of(missing, missing));
        assertEquals(2, reads[0]);
        assertSame(manager.get(missing), manager.get(missing));
        assertThrows(IllegalArgumentException.class, () -> new SpriteId("../test.png"));
    }
}
