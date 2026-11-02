package jse.theme.forest;

import jse.factory.EnemySettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;
import jse.world.Enemy;
import jse.world.ThemeId;

public final class ForestEnemy extends Enemy {

    public ForestEnemy(SpawnRequest request, Graphic graphic, EnemySettings settings) {
        super(ThemeId.FOREST, request, graphic, settings);
    }
}
