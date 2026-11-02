package jse;

import java.util.ArrayList;
import java.util.List;
import jse.assets.SpriteId;
import jse.factory.EnemySettings;
import jse.factory.EntitySettings;
import jse.factory.FamilySprites;
import jse.factory.PickupSettings;
import jse.factory.PlayerSettings;
import jse.math.Rect;
import jse.math.Rgba;
import jse.math.Vec2;
import jse.render.Renderer;

public final class GameFixtures {

    private GameFixtures() {
    }

    public static EntitySettings entities() {
        return new EntitySettings(new PlayerSettings(new Vec2(24, 24), 220, 3),
                new EnemySettings(new Vec2(24, 24), 110), new PickupSettings(new Vec2(16, 16), 1));
    }

    public static FamilySprites sprites(String theme) {
        return new FamilySprites(new SpriteId("sprites/" + theme + "/player.png"),
                new SpriteId("sprites/" + theme + "/enemy.png"),
                new SpriteId("sprites/" + theme + "/pickup.png"));
    }

    public static class CaptureRenderer implements Renderer {

        public final List<Rect> rectangles = new ArrayList<>();

        public final List<Rgba> colors = new ArrayList<>();

        public final List<SpriteId> sprites = new ArrayList<>();

        public final List<String> text = new ArrayList<>();

        public void clear() {
            rectangles.clear();
            colors.clear();
            sprites.clear();
            text.clear();
        }

        public void drawRectangle(Rect bounds, Rgba color) {
            rectangles.add(bounds);
            colors.add(color);
        }

        public void drawSprite(SpriteId id, Rect bounds) {
            sprites.add(id);
        }

        public void drawOutline(Rect bounds, Rgba color) {
        }

        public void drawText(String value, Vec2 position, Rgba color) {
            text.add(value);
        }
    }
}
