package jse.assets;

import java.util.HashSet;
import javax.imageio.ImageIO;
import jse.demo.LaunchSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameSpritesTest {
    @Test
    void allSixSpritesDecodeAsDistinctVisibleTransparentArtwork() throws Exception {
        var settings = LaunchSettings.load(null);
        var assets = new AssetManager(getClass().getClassLoader(), settings.placeholder());
        assets.preload(settings.showcase().sprites());
        var signatures = new HashSet<Integer>();
        for (var id : settings.showcase().sprites()) {
            try (var stream = getClass().getClassLoader().getResourceAsStream(id.path())) {
                assertNotNull(stream, id.path());
                var image = ImageIO.read(stream);
                assertNotNull(image);
                assertEquals(32, image.getWidth());
                assertEquals(32, image.getHeight());
                assertEquals(0, image.getRGB(0, 0));
                int visible = 0;
                int hash = 1;
                for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
                    int pixel = image.getRGB(x, y);
                    if ((pixel >>> 24) != 0) visible++;
                    hash = 31 * hash + pixel;
                    assertEquals(pixel, assets.get(id).getRGB(x, y));
                }
                assertTrue(visible > 50 && visible < 1024);
                signatures.add(hash);
            }
        }
        assertEquals(6, signatures.size());
    }
}
