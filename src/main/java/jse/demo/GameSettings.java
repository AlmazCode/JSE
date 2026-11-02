package jse.demo;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.Objects;
import java.util.Properties;
import jse.core.EngineConfig;
import jse.factory.EnemySettings;
import jse.factory.EntitySettings;
import jse.factory.PickupSettings;
import jse.factory.PlayerSettings;
import jse.math.Rect;
import jse.math.Vec2;
import jse.render.DecorationSettings;

public record GameSettings(EntitySettings entities, ArenaSettings rules, int worldCapacity,
        int enemyCount, int pickupCount, double padding, double headerHeight, double lineHeight,
        double patrolDistance, double fleeRadius, int logCapacity, int logRows) {
    public GameSettings {
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(rules, "rules");
        boolean validCounts = enemyCount >= 0 && enemyCount <= 16 && pickupCount > 0
                && pickupCount <= 16 && worldCapacity >= 1 + enemyCount + pickupCount
                && logCapacity > 0 && logRows > 0 && logRows <= 5;
        boolean reachableScore = (long) pickupCount * entities.pickup().value() >= rules
                .targetScore();
        boolean validDimensions = positiveFinite(padding) && positiveFinite(headerHeight)
                && positiveFinite(lineHeight) && positiveFinite(patrolDistance)
                && positiveFinite(fleeRadius);
        if (!validCounts || !reachableScore || !validDimensions) {
            throw new IllegalArgumentException(
                    "Game counts, capacity, dimensions and obtainable score must be valid");
        }
    }

    static GameSettings from(Properties values) {
        var reader = new LaunchSettings.SettingsReader(values);
        var player = new PlayerSettings(square(reader.number("game.player_size")),
                reader.number("game.player_speed"), reader.integer("game.player_health"));
        var enemy = new EnemySettings(square(reader.number("game.enemy_size")),
                reader.number("game.enemy_speed"));
        var pickup = new PickupSettings(square(reader.number("game.pickup_size")),
                reader.integer("game.pickup_value"));
        return new GameSettings(new EntitySettings(player, enemy, pickup),
                new ArenaSettings(reader.integer("game.target_score"),
                        reader.number("game.damage_cooldown"),
                        reader.integer("game.contact_damage")),
                reader.integer("game.world_capacity"), reader.integer("game.enemy_count"),
                reader.integer("game.pickup_count"), reader.number("game.padding"),
                reader.number("game.header_height"), reader.number("game.line_height"),
                reader.number("game.patrol_distance"), reader.number("game.flee_radius"),
                reader.integer("game.log_capacity"), reader.integer("game.log_rows"));
    }

    private static Vec2 square(double side) {
        return new Vec2(side, side);
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }

    public Rect arena(EngineConfig config, DecorationSettings decorations) {
        double edge = padding + decorations.outlineWidth();
        double top = headerHeight + Math.max(decorations.outlineWidth(),
                decorations.barOffset() + decorations.barHeight());
        return new Rect(edge, top, config.width() - edge * 2,
                config.height() - top - edge - lineHeight * (logRows + 1));
    }

    int columns() {
        return (int) Math.ceil(Math.sqrt(1 + enemyCount + pickupCount));
    }

    int rows() {
        return (1 + enemyCount + pickupCount + columns() - 1) / columns();
    }

    Vec2 position(Rect arena, int index, Vec2 size) {
        double cellWidth = arena.width() / columns();
        double cellHeight = arena.height() / rows();
        return new Vec2(arena.x() + (index % columns() + 0.5) * cellWidth - size.x() / 2,
                arena.y() + (index / columns() + 0.5) * cellHeight - size.y() / 2);
    }

    public void validateLayout(LaunchSettings settings) {
        var decorations = settings.showcase().decorations();
        var bounds = arena(settings.engine(), decorations);
        var font = new Font(settings.render().fontFamily(), Font.PLAIN,
                settings.render().fontSize());
        var context = new FontRenderContext(null, settings.render().antialias(), false);
        var metrics = font.getLineMetrics("Ag", context);
        double largestWidth = Math.max(entities.player().size().x(),
                Math.max(entities.enemy().size().x(), entities.pickup().size().x()));
        double largestHeight = Math.max(entities.player().size().y(),
                Math.max(entities.enemy().size().y(), entities.pickup().size().y()));
        double minimumPadding = Math.max(metrics.getDescent(),
                settings.render().strokeWidth() / 2 + decorations.outlineWidth());
        double titleHeight = GameLabels.title(settings.keys()).size() * lineHeight
                + metrics.getDescent();
        double cellWidth = largestWidth + 2 * decorations.outlineWidth();
        double cellHeight = largestHeight + decorations.barHeight() + decorations.barOffset()
                + 2 * decorations.outlineWidth();
        boolean textFits = padding >= minimumPadding && lineHeight >= metrics.getHeight()
                && headerHeight >= padding + lineHeight * 6
                && titleHeight <= settings.engine().height() - padding;
        boolean gridFits = bounds.width() / columns() >= cellWidth
                && bounds.height() / rows() >= cellHeight;
        if (!textFits || !gridFits) {
            throw new IllegalArgumentException("Game graphics and text must fit the viewport");
        }
        for (var text : GameLabels.validationText(settings)) {
            if (font.getStringBounds(text, context).getWidth() > settings.engine().width()
                    - padding * 2) {
                throw new IllegalArgumentException(
                        "Game help text must fit the viewport; widen the window or shorten bindings");
            }
        }
        settings.showcase().validateLayout(settings.engine(), settings.render(), settings.keys());
    }
}
