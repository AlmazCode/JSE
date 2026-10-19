package jse.demo;

import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LaunchSettingsTest {
    @TempDir Path directory;

    @Test
    void previewOverridesCanReuseKeysReservedOnlyByTheShowcase() throws Exception {
        var path = directory.resolve("legacy.properties");
        for (String value : new String[] {"input.move_up=T", "input.pause=F2", "input.restart=F3"}) {
            Files.writeString(path, value);
            assertDoesNotThrow(() -> LaunchSettings.load(path), value);
        }
    }

    @Test
    void eachExampleChecksConflictsOnlyAmongItsActiveActions() throws Exception {
        var path = directory.resolve("shared-bindings.properties");
        Files.writeString(path, "input.move_up=T\ninput.pause=F2\ninput.restart=F3\n");
        assertDoesNotThrow(() -> LaunchSettings.load(path, LaunchArguments.Example.PREVIEW));
        assertDoesNotThrow(() -> LaunchSettings.load(path, LaunchArguments.Example.SHOWCASE));
        Files.writeString(path, "input.switch_renderer=T\n");
        assertDoesNotThrow(() -> LaunchSettings.load(path, LaunchArguments.Example.PREVIEW));
        assertThrows(IllegalArgumentException.class,
                () -> LaunchSettings.load(path, LaunchArguments.Example.SHOWCASE));
    }

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
