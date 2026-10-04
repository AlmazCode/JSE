package jse.demo;

import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LaunchSettingsTest {
    @TempDir Path directory;
    @Test
    void overridesAreValidatedBeforeLaunching() throws Exception {
        var path = directory.resolve("preview.properties");
        Files.writeString(path, "window.width=800\npreview.speed=300\ninput.move_up=I,UP\n");
        var settings = LaunchSettings.load(path);
        assertEquals(800, settings.engine().width());
        assertEquals(300, settings.preview().speed());
        Files.writeString(path, "window.wdth=800\n");
        assertThrows(IllegalArgumentException.class, () -> LaunchSettings.load(path));
        Files.writeString(path, "preview.object_width=2000\n");
        assertThrows(IllegalArgumentException.class, () -> LaunchSettings.load(path));
        Files.writeString(path, "render.antialias=perhaps\n");
        assertThrows(IllegalArgumentException.class, () -> LaunchSettings.load(path));
        Files.writeString(path, "input.pause=W\n");
        assertThrows(IllegalArgumentException.class, () -> LaunchSettings.load(path));
        Files.writeString(path, "engine.max_frame_delta=NaN\n");
        assertThrows(IllegalArgumentException.class, () -> LaunchSettings.load(path));
    }
}
