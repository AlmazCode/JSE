package jse.testing;

import jse.assets.SpriteId;
import jse.event.EventBus;
import jse.factory.EnemySettings;
import jse.factory.PickupSettings;
import jse.factory.PlayerSettings;
import jse.factory.SpawnRequest;
import jse.math.Rect;
import jse.math.Rgba;
import jse.math.Vec2;
import jse.render.Graphic;
import jse.render.RectangleGraphic;
import jse.render.Renderer;
import jse.world.Enemy;
import jse.world.Pickup;
import jse.world.Player;
import jse.world.ThemeId;
import jse.world.World;

/** Minimal concrete products and a silent renderer, so the patterns can be tested in isolation. */
public final class Fixtures {
    public static final Rect ARENA = new Rect(0, 0, 500, 300);
    public static final Vec2 SIZE = new Vec2(20, 20);

    private Fixtures() {
    }

    public static final Renderer SILENT = new Renderer() {
        @Override
        public void drawSprite(SpriteId sprite, Rect bounds) {
        }

        @Override
        public void drawRectangle(Rect bounds, Rgba color) {
        }

        @Override
        public void drawOutline(Rect bounds, Rgba color) {
        }

        @Override
        public void drawText(String text, Vec2 position, Rgba color) {
        }
    };

    public static Graphic newGraphic() {
        return new RectangleGraphic(SILENT, new Rgba(10, 20, 30, 255));
    }

    public static World world(EventBus bus) {
        return new World(1, ARENA, ThemeId.FOREST, 128, bus);
    }

    public static SpawnRequest at(double x, double y) {
        return new SpawnRequest(new Vec2(x, y), SIZE);
    }

    public static TestPlayer player(double x, double y) {
        return new TestPlayer(ThemeId.FOREST, at(x, y), 3);
    }

    public static TestEnemy enemy(double x, double y) {
        return new TestEnemy(ThemeId.FOREST, at(x, y), 100);
    }

    public static TestPickup pickup(double x, double y) {
        return new TestPickup(ThemeId.FOREST, at(x, y), 1);
    }

    public static final class TestPlayer extends Player {
        public TestPlayer(ThemeId family, SpawnRequest request, int health) {
            super(family, request, newGraphic(), new PlayerSettings(request.size(), 200, health));
        }
    }

    public static final class TestEnemy extends Enemy {
        public TestEnemy(ThemeId family, SpawnRequest request, double speed) {
            super(family, request, newGraphic(), new EnemySettings(request.size(), speed));
        }
    }

    public static final class TestPickup extends Pickup {
        public TestPickup(ThemeId family, SpawnRequest request, int value) {
            super(family, request, newGraphic(), new PickupSettings(request.size(), value));
        }
    }
}
