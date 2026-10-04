package jse.platform.awt;

import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import jse.input.GameAction;
import jse.input.InputState;
import jse.input.InputSource;

public final class SwingInputSource implements InputSource {
    private record InstalledKey(KeyStroke stroke, Object previousLocal, Object actionId) {}
    private final KeyBindings bindings;
    private final Set<Integer> down = new HashSet<>();
    private final EnumSet<GameAction> pressed = EnumSet.noneOf(GameAction.class);
    private final List<InstalledKey> installed = new ArrayList<>();
    private JComponent component;
    private boolean previousTraversal;

    public SwingInputSource(KeyBindings bindings) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
    }

    public void bind(JComponent target) {
        requireEdt();
        if (component != null)
            throw new IllegalStateException("Input is already bound");
        component = Objects.requireNonNull(target, "target");
        previousTraversal = component.getFocusTraversalKeysEnabled();
        component.setFocusTraversalKeysEnabled(false);
        var map = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var localKeys = map.keys() == null ? Set.<KeyStroke>of() : new HashSet<>(Arrays.asList(map.keys()));
        for (int code : bindings.keys().keySet()) {
            for (boolean released : new boolean[] {false, true}) {
                var stroke = KeyStroke.getKeyStroke(code, 0, released);
                var id = new Object();
                installed.add(new InstalledKey(stroke, localKeys.contains(stroke) ? map.get(stroke) : null, id));
                map.put(stroke, id);
                component.getActionMap().put(id, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        if (released)
                            release(code);
                        else
                            press(code);
                    }
                });
            }
        }
    }

    public void unbind() {
        requireEdt();
        if (component == null)
            return;
        var map = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (var key : installed) {
            if (key.previousLocal() == null)
                map.remove(key.stroke());
            else
                map.put(key.stroke(), key.previousLocal());
            component.getActionMap().remove(key.actionId());
        }
        component.setFocusTraversalKeysEnabled(previousTraversal);
        component = null;
        installed.clear();
        clear();
    }
    void press(int key) {
        requireEdt();
        var action = bindings.keys().get(key);
        if (action == null || down.contains(key))
            return;
        boolean alreadyHeld = down.stream().anyMatch(code -> bindings.keys().get(code) == action);
        down.add(key);
        if (!alreadyHeld)
            pressed.add(action);
    }
    void release(int key) {
        requireEdt();
        down.remove(key);
    }

    @Override
    public InputState snapshotAndConsumePressed() {
        requireEdt();
        var held = EnumSet.noneOf(GameAction.class);
        for (int key : down) held.add(bindings.keys().get(key));
        var state = new InputState(held, pressed);
        pressed.clear();
        return state;
    }

    @Override
    public void clear() {
        requireEdt();
        down.clear();
        pressed.clear();
    }
    static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Swing operations require EDT");
    }
}
