package jse.demo;

import java.util.List;
import jse.input.GameAction;
import jse.platform.awt.KeyBindings;

final class GameLabels {
    private GameLabels() {}
    static String movement(KeyBindings keys) {
        return "Move: " + keys.label(GameAction.MOVE_UP) + " / " + keys.label(GameAction.MOVE_LEFT) + " / "
                + keys.label(GameAction.MOVE_DOWN) + " / " + keys.label(GameAction.MOVE_RIGHT)
                + "  |  " + keys.label(GameAction.PAUSE) + ": pause";
    }
    static String runControls(KeyBindings keys) {
        return keys.label(GameAction.RESTART) + ": reset  |  " + keys.label(GameAction.SWITCH_THEME) + ": theme  |  "
                + keys.label(GameAction.BACK_TO_TITLE) + ": menu";
    }
    static String visuals(KeyBindings keys) {
        return keys.label(GameAction.SWITCH_RENDERER) + ": renderer  |  " + keys.label(GameAction.TOGGLE_OUTLINE)
                + ": outline  |  " + keys.label(GameAction.TOGGLE_HEALTH_BAR) + ": health";
    }
    static String ai(KeyBindings keys, String selected, boolean logEnabled) {
        return keys.label(GameAction.SELECT_ENEMY) + ": enemy  |  " + keys.label(GameAction.SWITCH_BEHAVIOR)
                + ": behavior  |  " + keys.label(GameAction.TOGGLE_LOG) + ": log " + (logEnabled ? "ON" : "OFF")
                + "  |  Selected " + selected;
    }
    static List<String> title(KeyBindings keys) {
        return List.of("JSE / Game", "Six design patterns in one arena", "",
                keys.label(GameAction.START_ARENA) + ": start Arena", keys.label(GameAction.SHOW_SHOWCASE) + ": open Showcase",
                keys.label(GameAction.BACK_TO_TITLE) + ": close", "",
                "Visual preferences are kept when returning to the menu.");
    }
    static List<String> validationText(LaunchSettings settings) {
        var keys = settings.keys();
        var text = new java.util.ArrayList<>(List.of("JSE / Arena / Forest / wireframe / Paused", movement(keys), runControls(keys), visuals(keys), ai(keys, "#" + Long.MAX_VALUE + " Patrol", false),
                "Health " + settings.game().entities().player().maxHealth() + " / " + settings.game().entities().player().maxHealth()
                        + "  |  Score " + Integer.MAX_VALUE + " / " + settings.game().rules().targetScore() + "  |  Run " + Long.MAX_VALUE,
                "JSE / Game", keys.label(GameAction.START_ARENA) + ": start Arena",
                keys.label(GameAction.SHOW_SHOWCASE) + ": open Showcase",
                keys.label(GameAction.BACK_TO_TITLE) + ": close", "Collect pickups. Avoid enemies. Reset or change theme for a new run."));
        text.addAll(title(keys));
        return List.copyOf(text);
    }
}
