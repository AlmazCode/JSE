package jse.factory;

import java.util.Objects;

import jse.world.ThemeId;
import jse.world.Player;
import jse.world.Enemy;
import jse.world.Pickup;

import jse.render.Renderer;
import jse.render.Graphic;
import jse.render.SpriteGraphic;

import jse.theme.space.SpacePlayer;
import jse.theme.space.SpaceEnemy;
import jse.theme.space.SpacePickup;

public final class SpaceFactory implements GameObjectFamilyFactory {

    private final Renderer renderer;
    private final EntitySettings settings;
    private final FamilySprites sprites;

    public SpaceFactory(
            Renderer renderer,
            EntitySettings settings,
            FamilySprites sprites) {

        this.renderer = Objects.requireNonNull(renderer);
        this.settings = Objects.requireNonNull(settings);
        this.sprites = Objects.requireNonNull(sprites);
    }

    @Override
    public ThemeId familyId() {
        return ThemeId.SPACE;
    }

    @Override
    public Player createPlayer(SpawnRequest request) {
        Graphic graphic = new SpriteGraphic(renderer, sprites.player());

        return new SpacePlayer(request, graphic, settings.player());
    }

    @Override
    public Enemy createEnemy(SpawnRequest request) {
        Graphic graphic = new SpriteGraphic(renderer, sprites.enemy());

        return new SpaceEnemy(request, graphic, settings.enemy());
    }

    @Override
    public Pickup createPickup(SpawnRequest request) {
        Graphic graphic = new SpriteGraphic(renderer, sprites.pickup());

        return new SpacePickup(request, graphic, settings.pickup());
    }
}