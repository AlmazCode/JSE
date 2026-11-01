package jse.factory;

import java.util.Objects;

import jse.world.ThemeId;
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
        Graphic graphic = new SpriteGraphic(renderer, sprites.player());

        return new ForestPlayer(request, graphic, settings.player());
    }

    @Override
    public Enemy createEnemy(SpawnRequest request) {
        Graphic graphic = new SpriteGraphic(renderer, sprites.enemy());

        return new ForestEnemy(request, graphic, settings.enemy());
    }

    @Override
    public Pickup createPickup(SpawnRequest request) {
        Graphic graphic = new SpriteGraphic(renderer, sprites.pickup());

        return new ForestPickup(request, graphic, settings.pickup());
    }
}