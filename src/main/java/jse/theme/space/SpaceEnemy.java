package jse.theme.space;

import jse.world.Enemy;
import jse.world.ThemeId;
import jse.factory.SpawnRequest;
import jse.factory.EnemySettings;
import jse.render.Graphic;

public final class SpaceEnemy extends Enemy {

    public SpaceEnemy(
            SpawnRequest request,
            Graphic graphic,
            EnemySettings settings) {

        super(ThemeId.SPACE, request, graphic, settings);
    }
}
