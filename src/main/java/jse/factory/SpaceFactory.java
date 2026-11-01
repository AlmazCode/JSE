package jse.factory;

import java.util.Objects;
import jse.math.Vec2;
import jse.render.Renderer;
import jse.render.SpriteGraphic;
import jse.world.*;
import jse.theme.space.*;

public final class SpaceFactory implements GameObjectFamilyFactory {
    private final Renderer renderer;
    private final EntitySettings settings;
    private final FamilySprites sprites;
    public SpaceFactory(Renderer renderer, EntitySettings settings, FamilySprites sprites) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.sprites = Objects.requireNonNull(sprites, "sprites");
    }
    @Override public ThemeId familyId() { return ThemeId.SPACE; }
    @Override public Player createPlayer(SpawnRequest request) {
        requireSize(request, settings.player().size());
        return new SpacePlayer(request, new SpriteGraphic(renderer, sprites.player()), settings.player());
    }
    @Override public Enemy createEnemy(SpawnRequest request) {
        requireSize(request, settings.enemy().size());
        return new SpaceEnemy(request, new SpriteGraphic(renderer, sprites.enemy()), settings.enemy());
    }
    @Override public Pickup createPickup(SpawnRequest request) {
        requireSize(request, settings.pickup().size());
        return new SpacePickup(request, new SpriteGraphic(renderer, sprites.pickup()), settings.pickup());
    }
    private static void requireSize(SpawnRequest request, Vec2 expected) {
        Objects.requireNonNull(request, "request");
        if (!request.size().equals(expected)) throw new IllegalArgumentException("Requested size differs from category settings");
    }
}
