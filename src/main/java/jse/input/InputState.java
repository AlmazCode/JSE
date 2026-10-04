package jse.input;

import java.util.Set;

public record InputState(Set<GameAction> held, Set<GameAction> pressed) {
    public InputState {
        held = Set.copyOf(held);
        pressed = Set.copyOf(pressed);
    }

    public boolean isHeld(GameAction action) {
        return held.contains(action);
    }

    public boolean isPressed(GameAction action) {
        return pressed.contains(action);
    }
}
