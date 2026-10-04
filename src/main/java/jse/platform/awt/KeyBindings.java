package jse.platform.awt;

import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.stream.Collectors;
import jse.input.GameAction;

public record KeyBindings(Map<Integer, GameAction> keys) {
    public KeyBindings {
        keys = Map.copyOf(keys);
        if (keys.isEmpty() || keys.keySet().stream().anyMatch(key -> key <= KeyEvent.VK_UNDEFINED))
            throw new IllegalArgumentException("At least one valid key binding is required");
    }

    public String label(GameAction action) {
        return keys.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == action)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> KeyEvent.getKeyText(entry.getKey()))
                .collect(Collectors.joining(" / "));
    }
}
