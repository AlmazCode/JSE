package jse.demo;

import java.nio.file.Path;
import jse.core.Scene;

public record LaunchArguments(boolean help, Path config, Example example) {
    public enum Example { PREVIEW, SHOWCASE }

    public static LaunchArguments parse(String[] args) {
        if (args.length == 1 && args[0].equals("--help"))
            return new LaunchArguments(true, null, Example.PREVIEW);
        Path config = null;
        Example example = Example.PREVIEW;
        boolean selected = false;
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (i + 1 >= args.length || args[i + 1].isBlank())
                throw new IllegalArgumentException("Expected a value for " + option);
            String value = args[++i];
            if (option.equals("--config") && config == null) {
                config = Path.of(value);
            } else if (option.equals("--example") && !selected) {
                example = switch (value) {
                    case "preview" -> Example.PREVIEW;
                    case "showcase" -> Example.SHOWCASE;
                    default -> throw new IllegalArgumentException("Unknown example: " + value);
                };
                selected = true;
            } else {
                throw new IllegalArgumentException("Unknown or repeated option: " + option);
            }
        }
        return new LaunchArguments(false, config, example);
    }

    public Scene createScene(LaunchSettings settings) {
        return switch (example) {
            case PREVIEW -> new PreviewScene(settings.preview(), settings.keys(), settings.host().initialMode());
            case SHOWCASE -> {
                settings.showcase().validateLayout(settings.engine(), settings.render(), settings.keys());
                yield new ShowcaseScene(settings.showcase(), settings.keys(), settings.host().initialMode());
            }
        };
    }
}
