package jse.demo;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;
import jse.assets.SpriteId;
import jse.core.EngineConfig;
import jse.math.Rect;
import jse.math.Rgba;
import jse.render.DecorationSettings;
import jse.render.awt.RenderStyle;
import jse.platform.awt.KeyBindings;

public record ShowcaseSettings(DecorationSettings decorations, List<SpriteId> forestSprites, List<SpriteId> spaceSprites,
        double sampleSize, double padding, double headerHeight, double lineHeight, int currentHealth, int maxHealth,
        Rgba objectColor, Rgba textColor, Rgba mutedColor) {
    public ShowcaseSettings {
        Objects.requireNonNull(decorations, "decorations");
        forestSprites = List.copyOf(forestSprites);
        spaceSprites = List.copyOf(spaceSprites);
        Objects.requireNonNull(objectColor, "objectColor");
        Objects.requireNonNull(textColor, "textColor");
        Objects.requireNonNull(mutedColor, "mutedColor");
        if (forestSprites.size() != 3 || spaceSprites.size() != 3)
            throw new IllegalArgumentException("Each showcase family needs player, enemy and pickup sprites");
        if (!Double.isFinite(sampleSize) || sampleSize <= 0 || !Double.isFinite(padding) || padding < 0
                || !Double.isFinite(headerHeight) || headerHeight <= 0 || !Double.isFinite(lineHeight) || lineHeight <= 0
                || maxHealth <= 0 || currentHealth < 0 || currentHealth > maxHealth)
            throw new IllegalArgumentException("Invalid showcase dimensions or sample health");
    }

    static ShowcaseSettings from(Properties values) {
        var reader = new LaunchSettings.SettingsReader(values);
        var decorations = new DecorationSettings(reader.number("showcase.outline_width"),
                reader.color("showcase.outline_color"), reader.number("showcase.bar_offset"),
                reader.number("showcase.bar_height"), reader.color("showcase.bar_fill"),
                reader.color("showcase.bar_background"));
        return new ShowcaseSettings(decorations, family(values, "forest"), family(values, "space"),
                reader.number("showcase.sample_size"), reader.number("showcase.padding"),
                reader.number("showcase.header_height"), reader.number("showcase.line_height"),
                reader.integer("showcase.health_current"), reader.integer("showcase.health_max"),
                reader.color("showcase.object_color"), reader.color("showcase.text_color"),
                reader.color("showcase.muted_color"));
    }

    private static List<SpriteId> family(Properties values, String theme) {
        return Stream.of("player", "enemy", "pickup")
                .map(kind -> new SpriteId(values.getProperty("showcase." + theme + "_" + kind).trim())).toList();
    }

    public List<SpriteId> sprites() {
        return Stream.concat(forestSprites.stream(), spaceSprites.stream()).distinct().toList();
    }

    double columnWidth(EngineConfig config) { return (config.width() - padding * 2) / 3; }
    double rowHeight(EngineConfig config) { return (config.height() - headerHeight - padding - lineHeight) / 4; }

    Rect sampleBounds(EngineConfig config, int row, int column) {
        double top = Math.max(decorations.outlineWidth(), decorations.barOffset() + decorations.barHeight());
        return new Rect(padding + columnWidth(config) * column + decorations.outlineWidth() + 2,
                headerHeight + rowHeight(config) * row + top, sampleSize, sampleSize);
    }

    public void validateLayout(EngineConfig config, RenderStyle style, KeyBindings keys) {
        var font = new Font(style.fontFamily(), Font.PLAIN, style.fontSize());
        var context = new FontRenderContext(null, style.antialias(), false);
        var metrics = font.getLineMetrics("Ag", context);
        double top = Math.max(decorations.outlineWidth(), decorations.barOffset() + decorations.barHeight());
        double stroke = style.strokeWidth() / 2;
        double rowContent = top + sampleSize
                + Math.max(decorations.outlineWidth(), style.fontSize() + metrics.getDescent()) + stroke;
        if (lineHeight < metrics.getHeight() || padding < metrics.getDescent()
                || headerHeight < lineHeight * 5 + padding
                || padding < decorations.outlineWidth() + stroke || rowHeight(config) < rowContent
                || columnWidth(config) < sampleSize + decorations.outlineWidth() * 2 + stroke + 4)
            throw new IllegalArgumentException("Showcase samples, labels and decoration layers must fit the viewport");
        double labelWidth = Stream.concat(sprites().stream().map(SpriteId::path),
                        Stream.of(ShowcaseLabels.BASE, ShowcaseLabels.DECORATED, "Rectangle", "Player", "Enemy", "Pickup"))
                .mapToDouble(text -> font.getStringBounds(text, context).getWidth())
                .max().orElse(0);
        if (labelWidth + decorations.outlineWidth() + 4 > columnWidth(config))
            throw new IllegalArgumentException("Showcase sprite labels must fit the viewport columns");
        for (String text : ShowcaseLabels.fullWidthText(this, keys)) {
            if (font.getStringBounds(text, context).getWidth() > config.width() - padding * 2)
                throw new IllegalArgumentException("Showcase help text must fit the viewport; widen the window or shorten key bindings");
        }
    }
}
