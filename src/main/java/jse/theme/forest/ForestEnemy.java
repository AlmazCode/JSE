package jse.theme.forest;

import jse.world.Enemy;
import jse.world.ThemeId;
import jse.factory.SpawnRequest;
import jse.factory.EnemySettings;
import jse.render.Graphic;

public final class ForestEnemy extends Enemy {

    public ForestEnemy(
            SpawnRequest request,
            Graphic graphic,
            EnemySettings settings) {

        super(ThemeId.FOREST, request, graphic, settings);
    }
}
