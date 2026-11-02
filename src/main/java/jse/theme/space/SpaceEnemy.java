package jse.theme.space;

import jse.factory.EnemySettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;
import jse.world.Enemy;
import jse.world.ThemeId;

public final class SpaceEnemy extends Enemy {

    public SpaceEnemy(SpawnRequest request, Graphic graphic, EnemySettings settings) {
        super(ThemeId.SPACE, request, graphic, settings);
    }
}
