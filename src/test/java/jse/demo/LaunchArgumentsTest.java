package jse.demo;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LaunchArgumentsTest {
    @Test
    void selectsExamplesAndConfigWithoutChangingTheDefault() {
        assertEquals(LaunchArguments.Example.PREVIEW, LaunchArguments.parse(new String[0]).example());
        var args = LaunchArguments.parse(new String[] {"--example", "showcase", "--config", "local.properties"});
        assertEquals(LaunchArguments.Example.SHOWCASE, args.example());
        assertEquals(Path.of("local.properties"), args.config());
        assertTrue(LaunchArguments.parse(new String[] {"--help"}).help());
        assertEquals(LaunchArguments.Example.PREVIEW,
                LaunchArguments.parse(new String[] {"--config", "x", "--example", "preview"}).example());
    }
    @Test
    void malformedOrAmbiguousOptionsFailBeforeStartup() {
        for (String[] args : new String[][] {{"--example"}, {"--example", "arena"}, {"--config"},
                {"--config", "x", "--config", "y"}, {"--example", "preview", "--example", "showcase"},
                {"--help", "--example", "showcase"}, {"--unknown"}, {"--config", " "}})
            assertThrows(IllegalArgumentException.class, () -> LaunchArguments.parse(args));
    }
}
