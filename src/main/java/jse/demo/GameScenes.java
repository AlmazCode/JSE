package jse.demo;

import java.util.Objects;
import jse.core.Scene;
import jse.render.Renderer;
import jse.render.VisualPreferences;
import jse.world.ThemeId;

/** Application composition; neither core nor the desktop host knows about game scenes. */
public final class GameScenes {
    private final LaunchSettings settings;
    private final VisualPreferences preferences;
    private ThemeId theme = ThemeId.FOREST;
    private long runId;
    private boolean logEnabled = true;
    public GameScenes(LaunchSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        settings.game().validateLayout(settings);
        preferences = new VisualPreferences(settings.host().initialMode());
    }
    LaunchSettings settings() { return settings; }
    public VisualPreferences preferences() { return preferences; }
    public boolean logEnabled() { return logEnabled; }
    void toggleLog() { logEnabled = !logEnabled; }
    public ThemeId theme() { return theme; }
    public TitleScene title() { return new TitleScene(this); }
    public ArenaScene arena() { return new ArenaScene(this); }
    Scene alternateTheme() {
        theme = theme == ThemeId.FOREST ? ThemeId.SPACE : ThemeId.FOREST;
        return arena();
    }
    ArenaRun newRun(Renderer renderer) {
        if (runId == Long.MAX_VALUE) throw new IllegalStateException("Run IDs exhausted");
        return new ArenaRun(++runId, theme, settings, renderer, logEnabled);
    }
    public ShowcaseScene showcase() {
        return new ShowcaseScene(settings.showcase(), settings.keys(), preferences, this::title);
    }
}
