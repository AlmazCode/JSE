package jse.factory;

import java.util.Objects;

import jse.world.ThemeId;
import jse.math.Vec2;
import jse.world.Player;
import jse.world.Enemy;
import jse.world.Pickup;

import jse.render.Renderer;
import jse.render.Graphic;
import jse.render.SpriteGraphic;

import jse.theme.forest.ForestPlayer;
import jse.theme.forest.ForestEnemy;
import jse.theme.forest.ForestPickup;

public final class ForestFactory implements GameObjectFamilyFactory {

    private final Renderer renderer;
    private final EntitySettings settings;
    private final FamilySprites sprites;

    public ForestFactory(
            Renderer renderer,
            EntitySettings settings,
            FamilySprites sprites) {

        this.renderer = Objects.requireNonNull(renderer);
        this.settings = Objects.requireNonNull(settings);
        this.sprites = Objects.requireNonNull(sprites);
    }

    @Override
    public ThemeId familyId() {
        return ThemeId.FOREST;
    }

    @Override
    public Player createPlayer(SpawnRequest request) {
        requireSize(request, settings.player().size());
        Graphic graphic = new SpriteGraphic(renderer, sprites.player());

        return new ForestPlayer(request, graphic, settings.player());
    }

    @Override
    public Enemy createEnemy(SpawnRequest request) {
        requireSize(request, settings.enemy().size());
        Graphic graphic = new SpriteGraphic(renderer, sprites.enemy());

        return new ForestEnemy(request, graphic, settings.enemy());
    }

    @Override
    public Pickup createPickup(SpawnRequest request) {
        requireSize(request, settings.pickup().size());
        Graphic graphic = new SpriteGraphic(renderer, sprites.pickup());

        return new ForestPickup(request, graphic, settings.pickup());
    }
    private static void requireSize(SpawnRequest request, Vec2 expected) {
        Objects.requireNonNull(request, "request");
        if (!request.size().equals(expected))
            throw new IllegalArgumentException("Requested size differs from category settings");
    }
}
