package jse.input;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
