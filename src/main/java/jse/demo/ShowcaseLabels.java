package jse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jse.input.GameAction;
import jse.platform.awt.KeyBindings;
import jse.render.RenderMode;

final class ShowcaseLabels {
    static final String TITLE = "JSE / Rendering Showcase";
    static final String BASE = "Base graphic", DECORATED = "Decorated graphic";
    private ShowcaseLabels() {}

    static String controls(KeyBindings keys) {
        return keys.label(GameAction.SWITCH_RENDERER) + ": renderer  |  "
                + keys.label(GameAction.TOGGLE_OUTLINE) + ": outline  |  "
                + keys.label(GameAction.TOGGLE_HEALTH_BAR) + ": health  |  "
                + keys.label(GameAction.SWITCH_THEME) + ": theme  |  " + keys.label(GameAction.QUIT) + ": close";
    }

    static String state(boolean outline, boolean healthBar) {
        return "Outline " + (outline ? "ON" : "OFF") + " / health " + (healthBar ? "ON" : "OFF")
                + " / health layers apply to the rectangle sample and Player";
    }

    static String info(ShowcaseSettings settings, RenderMode mode, boolean space) {
        return (space ? "Space" : "Forest") + " / " + mode.name().toLowerCase(Locale.ROOT)
                + " / sample health " + settings.currentHealth() + " / " + settings.maxHealth();
    }

    static String footer(KeyBindings keys) {
        return "Bridge + Decorator / " + keys.label(GameAction.TOGGLE_OUTLINE) + " + "
                + keys.label(GameAction.TOGGLE_HEALTH_BAR) + " layers / original JSE sprites";
    }

    static List<String> fullWidthText(ShowcaseSettings settings, KeyBindings keys) {
        var text = new ArrayList<>(List.of(TITLE, controls(keys), footer(keys)));
        for (boolean value : new boolean[] {false, true}) {
            for (var mode : RenderMode.values()) text.add(info(settings, mode, value));
            for (boolean other : new boolean[] {false, true}) text.add(state(value, other));
        }
        return text;
    }
}
