package jse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.awt.event.KeyEvent;
import jse.assets.SpriteId;
import jse.core.EngineControl;
import jse.core.EngineServices;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Rect;
import jse.math.Rgba;
import jse.math.Vec2;
import jse.render.Renderer;
import jse.render.RenderMode;
import jse.platform.awt.KeyBindings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShowcaseSceneTest {
    @Test
    void remappedControlsAreConsistentThroughoutTheHelp() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.SHOWCASE);
        var mapping = new HashMap<>(settings.keys().keys());
        mapping.remove(KeyEvent.VK_F2);
        mapping.put(KeyEvent.VK_O, GameAction.TOGGLE_OUTLINE);
        var renderer = new CaptureRenderer();
        var scene = new ShowcaseScene(settings.showcase(), new KeyBindings(mapping), RenderMode.FILLED);
        scene.onEnter(new EngineServices(settings.engine(), renderer, new Control()));
        scene.render(renderer);
        assertTrue(renderer.text.stream().anyMatch(text -> text.contains("O: outline")));
        assertTrue(renderer.text.stream().noneMatch(text -> text.contains("F2")));
    }

    @Test
    void comparesBaseAndCombinedGraphicsAndUsesTheRendererPassedToEachFrame() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.SHOWCASE);
        var old = new CaptureRenderer();
        var current = new CaptureRenderer();
        var control = new Control();
        var scene = new ShowcaseScene(settings.showcase(), settings.keys(), RenderMode.FILLED);
        var services = new EngineServices(settings.engine(), old, control);
        scene.onEnter(services);
        scene.render(old);
        var initial = List.copyOf(old.shapes);
        scene.update(0.1, pressed(GameAction.SWITCH_RENDERER));
        assertEquals(List.of(RenderMode.WIREFRAME), control.modes);
        scene.render(current);
        assertEquals(initial, current.shapes);
        assertEquals(6, current.sprites.size());
        assertTrue(current.sprites.stream().allMatch(id -> id.path().startsWith("sprites/forest/")));
        assertEquals(2, current.colors.stream().filter(settings.showcase().decorations().barFill()::equals).count());
        assertEquals(0, old.shapes.size() - initial.size());
        assertTrue(current.text.stream().anyMatch(t -> t.contains("2 / 3")));
        scene.onFocusLost();
        current.clear();
        scene.render(current);
        assertEquals(initial, current.shapes);
        scene.onExit();
        scene.onEnter(services);
        current.clear();
        scene.render(current);
        assertEquals(initial, current.shapes);
    }
    @Test
    void repeatedTogglesAndThemeChangesKeepBoundedLayersAndNonPlayerHealthHidden() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.SHOWCASE);
        var renderer = new CaptureRenderer();
        var scene = new ShowcaseScene(settings.showcase(), settings.keys(), RenderMode.FILLED);
        scene.onEnter(new EngineServices(settings.engine(), renderer, new Control()));
        for (int i = 0; i < 100; i++) {
            scene.update(0.1, pressed(GameAction.TOGGLE_OUTLINE, GameAction.TOGGLE_HEALTH_BAR));
            renderer.clear();
            scene.render(renderer);
            assertEquals(i % 2 == 0 ? 2 : 22, renderer.shapes.size());
        }
        scene.update(0.1, pressed(GameAction.SWITCH_THEME));
        renderer.clear();
        scene.render(renderer);
        assertTrue(renderer.sprites.stream().allMatch(id -> id.path().startsWith("sprites/space/")));
        assertEquals(2, renderer.colors.stream().filter(settings.showcase().decorations().barFill()::equals).count());
        scene.update(0.1, pressed(GameAction.TOGGLE_OUTLINE));
        renderer.clear();
        scene.render(renderer);
        assertEquals(6, renderer.shapes.size());
        scene.update(0.1, pressed(GameAction.TOGGLE_HEALTH_BAR));
        renderer.clear();
        scene.render(renderer);
        assertEquals(2, renderer.shapes.size());
        scene.update(0.1, pressed(GameAction.TOGGLE_OUTLINE));
        renderer.clear();
        scene.render(renderer);
        assertEquals(18, renderer.shapes.size());
    }
    @Test
    void quitTakesPrecedenceAndInvalidDeltaIsRejected() throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.SHOWCASE);
        var control = new Control();
        var scene = new ShowcaseScene(settings.showcase(), settings.keys(), RenderMode.FILLED);
        scene.onEnter(new EngineServices(settings.engine(), new CaptureRenderer(), control));
        scene.update(0.1, pressed(GameAction.QUIT, GameAction.SWITCH_RENDERER));
        assertTrue(control.stopped);
        assertTrue(control.modes.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> scene.update(Double.NaN, pressed()));
        assertThrows(IllegalArgumentException.class, () -> scene.update(-1, pressed()));
    }
    private static InputState pressed(GameAction... actions) { return new InputState(Set.of(), Set.of(actions)); }
    private static class Control implements EngineControl {
        final List<RenderMode> modes = new ArrayList<>();
        boolean stopped;
        public void requestScene(Scene scene) { fail("Showcase is independent of game scenes"); }
        public void setRenderMode(RenderMode mode) { modes.add(mode); }
        public void stop() { stopped = true; }
    }
    private static class CaptureRenderer implements Renderer {
        final List<Rect> shapes = new ArrayList<>();
        final List<Rgba> colors = new ArrayList<>();
        final List<SpriteId> sprites = new ArrayList<>();
        final List<String> text = new ArrayList<>();
        void clear() { shapes.clear(); colors.clear(); sprites.clear(); text.clear(); }
        public void drawRectangle(Rect bounds, Rgba color) { shapes.add(bounds); colors.add(color); }
        public void drawSprite(SpriteId id, Rect bounds) { sprites.add(id); }
        public void drawOutline(Rect bounds, Rgba color) {}
        public void drawText(String value, Vec2 position, Rgba color) { text.add(value); }
    }
}
