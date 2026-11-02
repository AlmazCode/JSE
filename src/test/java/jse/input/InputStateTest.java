package jse.input;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class InputStateTest {
    @Test
    void snapshotsDoNotRetainMutableCollections() {
        var keys = EnumSet.of(GameAction.MOVE_UP);
        var state = new InputState(keys, keys);
        keys.clear();
        assertTrue(state.isHeld(GameAction.MOVE_UP));
        assertTrue(state.isPressed(GameAction.MOVE_UP));
        assertThrows(UnsupportedOperationException.class, () -> state.held().clear());
    }
}
