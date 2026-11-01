package jse.demo;

import java.util.ArrayList;
import java.util.Locale;
import jse.core.EngineServices;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Vec2;
import jse.render.Renderer;
import jse.world.GameObject;

public final class ArenaScene implements Scene {
    private final GameScenes scenes;
    private final EntityGraphics graphics;
    private EngineServices services;
    private ArenaRun run;
    ArenaScene(GameScenes scenes) {
        this.scenes = scenes;
        graphics = new EntityGraphics(scenes.settings().showcase().decorations());
    }
    public ArenaRun run() { return run; }
    @Override public void onEnter(EngineServices services) {
        this.services = services;
        services.control().setRenderMode(scenes.preferences().mode());
        run = scenes.newRun(services.initialRenderer());
    }
    @Override public void update(double dt, InputState input) {
        if (!Double.isFinite(dt) || dt <= 0) throw new IllegalArgumentException("Scene delta must be finite and positive");
        if (input.isPressed(GameAction.BACK_TO_TITLE)) { services.control().requestScene(scenes.title()); return; }
        if (input.isPressed(GameAction.QUIT)) { services.control().stop(); return; }
        if (input.isPressed(GameAction.SWITCH_THEME)) { services.control().requestScene(scenes.alternateTheme()); return; }
        if (input.isPressed(GameAction.RESTART)) { services.control().requestScene(scenes.arena()); return; }
        var preferences = scenes.preferences();
        if (input.isPressed(GameAction.SWITCH_RENDERER)) {
            preferences.toggleRenderer(); services.control().setRenderMode(preferences.mode());
        }
        if (input.isPressed(GameAction.TOGGLE_OUTLINE)) preferences.toggleOutline();
        if (input.isPressed(GameAction.TOGGLE_HEALTH_BAR)) preferences.toggleHealthBar();
        if (input.isPressed(GameAction.PAUSE)) run.togglePause();
        run.advance(dt, input);
    }
    @Override public void render(Renderer renderer) {
        var settings = scenes.settings(); var keys = settings.keys(); var preferences = scenes.preferences();
        var objects = new ArrayList<GameObject>();run.world().forEachActive(objects::add);
        graphics.render(objects, renderer, preferences);
        renderer.drawOutline(run.world().arena(), settings.showcase().mutedColor());
        double x = settings.game().padding(), line = settings.game().lineHeight();
        String state = run.rules().status() == ArenaRules.Status.RUNNING ? (run.paused() ? "Paused" : "Running") : run.rules().status().name();
        String[] text = {"JSE / Arena / " + run.world().familyId() + " / " + preferences.mode().name().toLowerCase(Locale.ROOT) + " / " + state,
                "Health " + run.player().currentHealth() + " / " + run.player().maxHealth() + "  |  Score " + run.rules().score()
                        + " / " + settings.game().rules().targetScore() + "  |  Run " + run.world().runId(),
                GameLabels.movement(keys), GameLabels.runControls(keys), GameLabels.visuals(keys)};
        for (int i = 0; i < text.length; i++) renderer.drawText(text[i], new Vec2(x, line * (i + 1)), settings.showcase().textColor());
    }
    @Override public void onFocusLost() { if (run != null) run.onFocusLost(); }
    @Override public void onExit() {
        graphics.close();
        if (run != null) { run.close(); run = null; }
        services = null;
    }
}
