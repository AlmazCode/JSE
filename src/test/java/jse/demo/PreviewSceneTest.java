package jse.demo;

import java.util.Set;
import jse.assets.SpriteId;
import jse.core.EngineServices;
import jse.core.EngineControl;
import jse.core.Scene;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Vec2;
import jse.math.Rect;
import jse.math.Rgba;
import jse.render.Renderer;
import jse.render.RenderMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreviewSceneTest {
    @Test
    void movementIsNormalizedAndPauseFocusResetAndSwitchPreserveState() throws Exception {
        var settings = LaunchSettings.load(null);
        var renderer = new CaptureRenderer();
        var control = new EngineControl() {
            public void requestScene(Scene scene) {
                fail("Preview must not request game scenes");
            }
            public void setRenderMode(RenderMode mode) {
                assertEquals(RenderMode.WIREFRAME, mode);
            }
            public void stop() {}
        };
        var scene = new PreviewScene(settings.preview(), settings.keys(), settings.host().initialMode());
        scene.onEnter(new EngineServices(settings.engine(), renderer, control));
        scene.render(renderer);
        var initial = renderer.object.position();
        scene.update(0.1, new InputState(Set.of(GameAction.MOVE_UP, GameAction.MOVE_RIGHT), Set.of()));
        scene.render(renderer);
        assertEquals(22, renderer.object.position().subtract(initial).length(), 1e-10);
        var moved = renderer.object;
        scene.update(0.1, new InputState(Set.of(GameAction.MOVE_RIGHT), Set.of(GameAction.PAUSE)));
        scene.render(renderer);
        assertEquals(moved, renderer.object);
        scene.update(0.1, new InputState(Set.of(), Set.of(GameAction.PAUSE)));
        scene.onFocusLost();
        scene.update(0.1, new InputState(Set.of(GameAction.MOVE_RIGHT), Set.of()));
        scene.render(renderer);
        assertEquals(moved, renderer.object);
        scene.update(0.1, new InputState(Set.of(), Set.of(GameAction.SWITCH_RENDERER)));
        var second = new CaptureRenderer();
        scene.render(second);
        assertEquals(moved, second.object);
        scene.update(0.1, new InputState(Set.of(), Set.of(GameAction.RESTART)));
        scene.render(second);
        assertEquals(initial, second.object.position());
        scene.onExit();
    }
    static class CaptureRenderer implements Renderer {
        Rect object;
        public void drawSprite(SpriteId id, Rect bounds) {}
        public void drawRectangle(Rect bounds, Rgba color) {
            object = bounds;
        }
        public void drawOutline(Rect bounds, Rgba color) {}
        public void drawText(String text, Vec2 position, Rgba color) {}
    }
}
