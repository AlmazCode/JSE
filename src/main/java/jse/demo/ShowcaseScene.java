package jse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import jse.core.EngineServices;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Rect;
import jse.math.Vec2;
import jse.platform.awt.KeyBindings;
import jse.render.Graphic;
import jse.render.GraphicLayers;
import jse.render.HealthView;
import jse.render.RectangleGraphic;
import jse.render.RenderMode;
import jse.render.Renderer;
import jse.render.SpriteGraphic;
import jse.render.VisualPreferences;

public final class ShowcaseScene implements Scene {
    private final ShowcaseSettings settings;
    private final KeyBindings keys;
    private final VisualPreferences preferences;
    private final Supplier<Scene> menu;
    private final HealthView health;
    private final List<Sample> samples = new ArrayList<>();
    private EngineServices services;
    private RenderMode mode;
    private boolean space;
    private boolean outline;
    private boolean healthBar;

    public ShowcaseScene(ShowcaseSettings settings, KeyBindings keys, RenderMode initialMode) {
        this(settings, keys, new VisualPreferences(initialMode), null);
    }

    public ShowcaseScene(ShowcaseSettings settings, KeyBindings keys, VisualPreferences preferences,
            Supplier<Scene> menu) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.keys = Objects.requireNonNull(keys, "keys");
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.menu = menu;
        health = new HealthView() {
            @Override
            public int currentHealth() {
                return settings.currentHealth();
            }

            @Override
            public int maxHealth() {
                return settings.maxHealth();
            }
        };
    }

    @Override
    public void onEnter(EngineServices services) {
        this.services = Objects.requireNonNull(services, "services");
        mode = preferences.mode();
        space = false;
        outline = preferences.outline();
        healthBar = preferences.healthBar();
        if (menu != null) {
            services.control().setRenderMode(mode);
        }
        rebuildSamples(services.initialRenderer());
    }

    private void rebuildSamples(Renderer renderer) {
        samples.clear();
        var sprites = space ? settings.spaceSprites() : settings.forestSprites();
        samples.add(new Sample("Rectangle", new RectangleGraphic(renderer, settings.objectColor()),
                health));
        samples.add(new Sample("Player", new SpriteGraphic(renderer, sprites.get(0)), health));
        samples.add(new Sample("Enemy", new SpriteGraphic(renderer, sprites.get(1)), null));
        samples.add(new Sample("Pickup", new SpriteGraphic(renderer, sprites.get(2)), null));
        rebuildLayers();
    }

    private void rebuildLayers() {
        for (var sample : samples) {
            sample.graphic = GraphicLayers.compose(sample.base, sample.health, outline, healthBar,
                    settings.decorations());
        }
    }

    @Override
    public void update(double dt, InputState input) {
        if (!Double.isFinite(dt) || dt < 0) {
            throw new IllegalArgumentException("Update delta must be finite and nonnegative");
        }
        if (menu != null && input.isPressed(GameAction.BACK_TO_TITLE)) {
            services.control().requestScene(menu.get());
            return;
        }
        if (input.isPressed(GameAction.QUIT)) {
            services.control().stop();
            return;
        }
        if (input.isPressed(GameAction.SWITCH_RENDERER)) {
            preferences.toggleRenderer();
            mode = preferences.mode();
            services.control().setRenderMode(mode);
        }
        if (input.isPressed(GameAction.TOGGLE_OUTLINE)) {
            preferences.toggleOutline();
            outline = preferences.outline();
        }
        if (input.isPressed(GameAction.TOGGLE_HEALTH_BAR)) {
            preferences.toggleHealthBar();
            healthBar = preferences.healthBar();
        }
        if (input.isPressed(GameAction.SWITCH_THEME)) {
            space = !space;
            rebuildSamples(services.initialRenderer());
        } else if (input.isPressed(GameAction.TOGGLE_OUTLINE)
                || input.isPressed(GameAction.TOGGLE_HEALTH_BAR)) {
            rebuildLayers();
        }
    }

    @Override
    public void render(Renderer renderer) {
        double padding = settings.padding();
        double line = settings.lineHeight();
        renderer.drawText(ShowcaseLabels.TITLE, new Vec2(padding, line), settings.textColor());
        renderer.drawText(ShowcaseLabels.info(settings, mode, space), new Vec2(padding, line * 2),
                settings.textColor());
        renderer.drawText(ShowcaseLabels.controls(keys), new Vec2(padding, line * 3),
                settings.textColor());
        renderer.drawText(ShowcaseLabels.state(outline, healthBar), new Vec2(padding, line * 4),
                settings.mutedColor());
        var config = services.config();
        for (int column = 1; column <= 2; column++) {
            renderer.drawText(column == 1 ? ShowcaseLabels.BASE : ShowcaseLabels.DECORATED,
                    new Vec2(settings.sampleBounds(config, 0, column).x(),
                            settings.headerHeight() - 12),
                    settings.textColor());
        }
        for (int row = 0; row < samples.size(); row++) {
            var sample = samples.get(row);
            Rect baseBounds = settings.sampleBounds(config, row, 1);
            renderer.drawText(sample.label,
                    new Vec2(padding, baseBounds.y() + settings.sampleSize() / 2),
                    settings.textColor());
            // Render the base first: the decorated view shares this component, never another
            // entity.
            sample.base.setRenderer(renderer);
            sample.base.draw(baseBounds);
            sample.graphic.setRenderer(renderer);
            sample.graphic.draw(settings.sampleBounds(config, row, 2));
        }
        renderer.drawText(ShowcaseLabels.footer(keys), new Vec2(padding, config.height() - padding),
                settings.mutedColor());
    }

    @Override
    public void onFocusLost() {
    }

    @Override
    public void onExit() {
        samples.clear();
        services = null;
    }

    private static final class Sample {
        final String label;
        final Graphic base;
        final HealthView health;
        Graphic graphic;

        Sample(String label, Graphic base, HealthView health) {
            this.label = label;
            this.base = base;
            this.health = health;
        }
    }
}
