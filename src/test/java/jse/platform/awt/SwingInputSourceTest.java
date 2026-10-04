package jse.platform.awt;

import java.awt.event.KeyEvent;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import jse.input.GameAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwingInputSourceTest {
    @Test
    void physicalAliasesAndKeyRepeatDoNotDuplicateLogicalPresses() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var source = new SwingInputSource(new KeyBindings(Map.of(KeyEvent.VK_W, GameAction.MOVE_UP, KeyEvent.VK_UP,
                    GameAction.MOVE_UP, KeyEvent.VK_P, GameAction.PAUSE)));
            source.press(KeyEvent.VK_W);
            assertTrue(source.snapshotAndConsumePressed().isPressed(GameAction.MOVE_UP));
            source.press(KeyEvent.VK_UP);
            source.release(KeyEvent.VK_W);
            var state = source.snapshotAndConsumePressed();
            assertTrue(state.isHeld(GameAction.MOVE_UP));
            assertFalse(state.isPressed(GameAction.MOVE_UP));
            source.press(KeyEvent.VK_P);
            source.press(KeyEvent.VK_P);
            source.release(KeyEvent.VK_P);
            assertTrue(source.snapshotAndConsumePressed().isPressed(GameAction.PAUSE));
            assertFalse(source.snapshotAndConsumePressed().isPressed(GameAction.PAUSE));
            source.clear();
            assertTrue(source.snapshotAndConsumePressed().held().isEmpty());
        });
    }
    @Test
    void unbindRestoresExistingBindingsAndFocusTraversal() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new JPanel();
            var stroke = KeyStroke.getKeyStroke(KeyEvent.VK_P, 0, false);
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "previous");
            var source = new SwingInputSource(new KeyBindings(Map.of(KeyEvent.VK_P, GameAction.PAUSE)));
            source.bind(panel);
            assertFalse(panel.getFocusTraversalKeysEnabled());
            Object action = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
            panel.getActionMap().get(action).actionPerformed(null);
            assertTrue(source.snapshotAndConsumePressed().isPressed(GameAction.PAUSE));
            source.unbind();
            source.unbind();
            assertEquals("previous", panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke));
            assertTrue(panel.getFocusTraversalKeysEnabled());
            assertNull(panel.getActionMap().get(action));
        });
    }
}
