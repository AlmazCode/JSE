package jse.render;

import java.util.Objects;

/**
 * Application-owned display choices, independent of a game's state and lifetime.
 */
public final class VisualPreferences {
    private RenderMode mode;
    private boolean outline = true;
    private boolean healthBar = true;

    public VisualPreferences(RenderMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public RenderMode mode() {
        return mode;
    }

    public boolean outline() {
        return outline;
    }

    public boolean healthBar() {
        return healthBar;
    }

    public void toggleRenderer() {
        mode = mode == RenderMode.FILLED ? RenderMode.WIREFRAME : RenderMode.FILLED;
    }

    public void toggleOutline() {
        outline = !outline;
    }

    public void toggleHealthBar() {
        healthBar = !healthBar;
    }
}
