package jse.demo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Objects;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Properties;
import java.util.Locale;
import javax.swing.KeyStroke;
import jse.assets.SpriteId;
import jse.assets.MissingSpriteStyle;
import jse.core.EngineConfig;
import jse.input.GameAction;
import jse.math.Vec2;
import jse.math.Rgba;
import jse.platform.awt.KeyBindings;
import jse.platform.awt.HostSettings;
import jse.render.RenderMode;
import jse.render.awt.RenderStyle;

public record LaunchSettings(EngineConfig engine, HostSettings host, KeyBindings keys, RenderStyle render,
        MissingSpriteStyle placeholder, List<SpriteId> preload, PreviewSettings preview) {
    public LaunchSettings {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(render, "render");
        Objects.requireNonNull(placeholder, "placeholder");
        Objects.requireNonNull(preview, "preview");
        preload = List.copyOf(preload);
        preview.area(engine);
        if (render.fontSize() > preview.lineHeight())
            throw new IllegalArgumentException("Font must fit the preview line height");
    }

    public static LaunchSettings load(Path overrides) throws IOException {
        var values = new Properties();
        try (var stream = LaunchSettings.class.getResourceAsStream("/jse.properties")) {
            if (stream == null)
                throw new IOException("Bundled jse.properties is missing");
            values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        if (overrides != null) {
            var extra = new Properties();
            try (var reader = Files.newBufferedReader(overrides, StandardCharsets.UTF_8)) {
                extra.load(reader);
            }
            for (String key : extra.stringPropertyNames()) {
                if (!values.containsKey(key))
                    throw new IllegalArgumentException("Unknown configuration key: " + key);
                values.setProperty(key, extra.getProperty(key));
            }
        }
        var reader = new SettingsReader(values);
        var engine = new EngineConfig(reader.integer("window.width"), reader.integer("window.height"),
                reader.integer("engine.target_ups"), reader.integer("engine.max_updates_per_pump"),
                reader.number("engine.max_frame_delta"), reader.integer("engine.timer_delay_ms"));
        var host = new HostSettings(
                reader.text("window.title"), reader.color("window.background"), reader.mode("render.mode"));
        var render = new RenderStyle(reader.text("render.font_family"), reader.integer("render.font_size"),
                (float) reader.number("render.stroke_width"), reader.color("render.wireframe_color"),
                reader.bool("render.antialias"));
        var placeholder = new MissingSpriteStyle(reader.integer("assets.placeholder_width"),
                reader.integer("assets.placeholder_height"), reader.color("assets.placeholder_background"),
                reader.color("assets.placeholder_mark"));
        var preload = tokens(values.getProperty("assets.preload"), true).stream().map(SpriteId::new).toList();
        var preview = new PreviewSettings(
                new Vec2(reader.number("preview.object_width"), reader.number("preview.object_height")),
                reader.number("preview.speed"), reader.number("preview.padding"),
                reader.number("preview.header_height"), reader.number("preview.line_height"),
                reader.color("preview.object_color"), reader.color("preview.text_color"),
                reader.color("preview.muted_color"));
        return new LaunchSettings(engine, host, bindings(values), render, placeholder, preload, preview);
    }

    private static KeyBindings bindings(Properties values) {
        var keys = new HashMap<Integer, GameAction>();
        for (String property : values.stringPropertyNames()) {
            if (!property.startsWith("input."))
                continue;
            var action = GameAction.valueOf(property.substring("input.".length()).toUpperCase(Locale.ROOT));
            for (String name : tokens(values.getProperty(property), false)) {
                var stroke = KeyStroke.getKeyStroke("pressed " + name.toUpperCase(Locale.ROOT));
                if (stroke == null || stroke.getKeyCode() == 0 || stroke.getModifiers() != 0)
                    throw new IllegalArgumentException("Invalid key for " + property + ": " + name);
                var previous = keys.putIfAbsent(stroke.getKeyCode(), action);
                if (previous != null && previous != action)
                    throw new IllegalArgumentException("Key assigned to multiple actions: " + name);
            }
        }
        return new KeyBindings(keys);
    }

    private static List<String> tokens(String value, boolean allowEmpty) {
        if (allowEmpty && value.isBlank())
            return List.of();
        var result = Arrays.stream(value.split(",", -1)).map(String::trim).toList();
        if (result.stream().anyMatch(String::isEmpty))
            throw new IllegalArgumentException("List entries must not be empty");
        return result;
    }

    private record SettingsReader(Properties values) {
        String text(String key) {
            var value = values.getProperty(key).trim();
            if (value.isEmpty())
                throw new IllegalArgumentException("Empty configuration value: " + key);
            return value;
        }
        int integer(String key) {
            try {
                return Integer.parseInt(text(key));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected integer for " + key, ex);
            }
        }
        double number(String key) {
            try {
                return Double.parseDouble(text(key));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected number for " + key, ex);
            }
        }
        Rgba color(String key) {
            try {
                return Rgba.parse(text(key));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid color for " + key, ex);
            }
        }
        boolean bool(String key) {
            String value = text(key);
            if (!value.equals("true") && !value.equals("false"))
                throw new IllegalArgumentException("Expected true or false for " + key);
            return Boolean.parseBoolean(value);
        }
        RenderMode mode(String key) {
            try {
                return RenderMode.valueOf(text(key).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid rendering mode for " + key, ex);
            }
        }
    }
}
