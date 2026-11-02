package jse.demo;

import jse.core.EngineServices;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Vec2;
import jse.render.Renderer;

public final class TitleScene implements Scene {
    private final GameScenes scenes;
    private EngineServices services;

    TitleScene(GameScenes scenes) {
        this.scenes = scenes;
    }

    @Override
    public void onEnter(EngineServices services) {
        this.services = services;
        services.control().setRenderMode(scenes.preferences().mode());
    }

    @Override
    public void update(double dt, InputState input) {
        if (input.isPressed(GameAction.BACK_TO_TITLE) || input.isPressed(GameAction.QUIT)) {
            services.control().stop();
            return;
        }
        if (input.isPressed(GameAction.START_ARENA)) {
            services.control().requestScene(scenes.arena());
        } else if (input.isPressed(GameAction.SHOW_SHOWCASE)) {
            services.control().requestScene(scenes.showcase());
        }
    }

    @Override
    public void render(Renderer renderer) {
        var settings = scenes.settings();
        var keys = settings.keys();
        double x = settings.game().padding();
        double line = settings.game().lineHeight();
        var text = GameLabels.title(keys);
        for (int i = 0; i < text.size(); i++) {
            renderer.drawText(text.get(i), new Vec2(x, line * (i + 1)),
                    settings.showcase().textColor());
        }
    }

    @Override
    public void onFocusLost() {
    }

    @Override
    public void onExit() {
        services = null;
    }
}
