package jse.demo;

import java.util.Objects;
import jse.core.EngineServices;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Vec2;
import jse.math.Rect;
import jse.platform.awt.KeyBindings;
import jse.render.Renderer;
import jse.render.RectangleGraphic;
import jse.render.RenderMode;

public final class PreviewScene implements Scene {
    private final PreviewSettings settings;
    private final KeyBindings keys;
    private RenderMode mode;
    private EngineServices services;
    private Rect area;
    private Vec2 position;
    private RectangleGraphic graphic;
    private boolean paused;

    public PreviewScene(PreviewSettings settings, KeyBindings keys, RenderMode initialMode) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.keys = Objects.requireNonNull(keys, "keys");
        mode = Objects.requireNonNull(initialMode, "initialMode");
    }

    @Override
    public void onEnter(EngineServices services) {
        this.services = services;
        area = settings.area(services.config());
        graphic = new RectangleGraphic(services.initialRenderer(), settings.objectColor());
        reset();
    }

    @Override
    public void update(double dt, InputState input) {
        if (!Double.isFinite(dt) || dt < 0)
            throw new IllegalArgumentException("Update delta must be finite and nonnegative");
        if (input.isPressed(GameAction.QUIT)) {
            services.control().stop();
            return;
        }
        if (input.isPressed(GameAction.PAUSE))
            paused = !paused;
        if (input.isPressed(GameAction.RESTART))
            reset();
        if (input.isPressed(GameAction.SWITCH_RENDERER)) {
            mode = mode == RenderMode.FILLED ? RenderMode.WIREFRAME : RenderMode.FILLED;
            services.control().setRenderMode(mode);
        }
        if (paused)
            return;
        int x = (input.isHeld(GameAction.MOVE_RIGHT) ? 1 : 0) - (input.isHeld(GameAction.MOVE_LEFT) ? 1 : 0);
        int y = (input.isHeld(GameAction.MOVE_DOWN) ? 1 : 0) - (input.isHeld(GameAction.MOVE_UP) ? 1 : 0);
        var delta = new Vec2(x, y).normalized().scale(settings.speed() * dt);
        position = area.clampPosition(position.add(delta), settings.objectSize());
    }

    private void reset() {
        position = area.position().add(area.size().subtract(settings.objectSize()).scale(0.5));
        paused = false;
    }

    @Override
    public void render(Renderer renderer) {
        graphic.setRenderer(renderer);
        renderer.drawOutline(area, settings.mutedColor());
        graphic.draw(new Rect(position.x(), position.y(), settings.objectSize().x(), settings.objectSize().y()));
        double x = settings.padding(), line = settings.lineHeight();
        renderer.drawText("JSE / Runtime Preview", new Vec2(x, line), settings.textColor());
        renderer.drawText("Move: " + keys.label(GameAction.MOVE_UP) + " | " + keys.label(GameAction.MOVE_LEFT) + " | "
                        + keys.label(GameAction.MOVE_DOWN) + " | " + keys.label(GameAction.MOVE_RIGHT),
                new Vec2(x, line * 2), settings.textColor());
        renderer.drawText(keys.label(GameAction.PAUSE) + ": pause  |  " + keys.label(GameAction.RESTART)
                        + ": reset  |  " + keys.label(GameAction.SWITCH_RENDERER) + ": renderer  |  "
                        + keys.label(GameAction.QUIT) + ": close",
                new Vec2(x, line * 3), settings.textColor());
        renderer.drawText((paused ? "Paused" : "Running") + " / " + mode.name().toLowerCase(java.util.Locale.ROOT)
                        + " / " + services.config().targetUps() + " updates per second",
                new Vec2(x, line * 4), settings.mutedColor());
    }

    @Override
    public void onFocusLost() {
        paused = true;
    }

    @Override
    public void onExit() {
        services = null;
        graphic = null;
    }
}
