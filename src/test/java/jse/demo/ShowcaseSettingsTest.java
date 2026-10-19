package jse.demo;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ShowcaseSettingsTest {
    @TempDir Path directory;
    @Test
    void loadsVisualDefaultsAndOverridesAndPreloadsBothFamilies() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.SHOWCASE);
        var showcase = settings.showcase();
        assertEquals(6, showcase.sprites().size());
        assertEquals(2, showcase.currentHealth());
        assertEquals(3, showcase.maxHealth());
        showcase.validateLayout(settings.engine(), settings.render(), settings.keys());
        var file = directory.resolve("showcase.properties");
        Files.writeString(file, "showcase.outline_width=3\nshowcase.health_current=1\ninput.toggle_outline=O\n");
        settings = LaunchSettings.load(file, LaunchArguments.Example.SHOWCASE);
        assertEquals(3, settings.showcase().decorations().outlineWidth());
        assertEquals(1, settings.showcase().currentHealth());
        assertEquals("O", settings.keys().label(jse.input.GameAction.TOGGLE_OUTLINE));
    }
    @Test
    void rejectsInvalidHealthDimensionsAndClippedLayout() throws Exception {
        var file = directory.resolve("bad.properties");
        for (String value : new String[] {"showcase.health_max=0", "showcase.health_current=-1",
                "showcase.health_current=4", "showcase.outline_width=NaN", "showcase.bar_height=0",
                "showcase.forest_player=../player.png"}) {
            Files.writeString(file, value);
            assertThrows(IllegalArgumentException.class, () -> LaunchSettings.load(file), value);
        }
        for (String value : new String[] {"window.width=400", "window.height=250", "showcase.outline_width=200",
                "showcase.bar_offset=200", "showcase.sample_size=400", "showcase.header_height=20"}) {
            Files.writeString(file, value);
            var settings = LaunchSettings.load(file, LaunchArguments.Example.SHOWCASE);
            assertThrows(IllegalArgumentException.class,
                    () -> settings.showcase().validateLayout(settings.engine(), settings.render(), settings.keys()), value);
        }
    }

    @Test
    void launchRejectsClippedFontDescenders() throws Exception {
        var file = directory.resolve("clipped-text.properties");
        var args = LaunchArguments.parse(new String[] {"--example", "showcase"});
        for (String value : new String[] {"render.font_size=22\nshowcase.padding=3\n",
                "showcase.padding=3\n"}) {
            Files.writeString(file, value);
            var settings = LaunchSettings.load(file, LaunchArguments.Example.SHOWCASE);
            assertThrows(IllegalArgumentException.class, () -> args.createScene(settings), value);
        }
    }

    @Test
    void launchRejectsClippedRemappedHelpText() throws Exception {
        var file = directory.resolve("long-bindings.properties");
        Files.writeString(file, "window.width=660\ninput.switch_renderer=PAGE_UP\ninput.toggle_outline=PAGE_DOWN\n"
                + "input.toggle_health_bar=PRINTSCREEN\ninput.switch_theme=BACK_SPACE\n");
        var settings = LaunchSettings.load(file, LaunchArguments.Example.SHOWCASE);
        var args = LaunchArguments.parse(new String[] {"--example", "showcase"});
        assertThrows(IllegalArgumentException.class, () -> args.createScene(settings));
    }
}
