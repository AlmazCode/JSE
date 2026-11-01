package jse.factory;

import java.util.Objects;
import jse.math.Vec2;
import jse.render.Renderer;
import jse.render.SpriteGraphic;
import jse.world.*;
import jse.theme.forest.*;

public final class ForestFactory implements GameObjectFamilyFactory {
    private final Renderer renderer;
    private final EntitySettings settings;
    private final FamilySprites sprites;
    public ForestFactory(Renderer renderer, EntitySettings settings, FamilySprites sprites) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.sprites = Objects.requireNonNull(sprites, "sprites");
    }
    @Override public ThemeId familyId() { return ThemeId.FOREST; }
    @Override public Player createPlayer(SpawnRequest request) {
        requireSize(request, settings.player().size());
        return new ForestPlayer(request, new SpriteGraphic(renderer, sprites.player()), settings.player());
    }
    @Override public Enemy createEnemy(SpawnRequest request) {
        requireSize(request, settings.enemy().size());
        return new ForestEnemy(request, new SpriteGraphic(renderer, sprites.enemy()), settings.enemy());
    }
    @Override public Pickup createPickup(SpawnRequest request) {
        requireSize(request, settings.pickup().size());
        return new ForestPickup(request, new SpriteGraphic(renderer, sprites.pickup()), settings.pickup());
    }
    private static void requireSize(SpawnRequest request, Vec2 expected) {
        Objects.requireNonNull(request, "request");
        if (!request.size().equals(expected)) throw new IllegalArgumentException("Requested size differs from category settings");
    }
}
