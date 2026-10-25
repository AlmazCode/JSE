
package jse.factory;

import jse.world.Enemy;
import jse.world.ObjectKind;

public final class EnemySpawner extends EntitySpawner<Enemy> {

    public EnemySpawner(GameObjectFamilyFactory factory) {
        super(factory, ObjectKind.ENEMY);
    }

    @Override
    protected Enemy createObject(SpawnRequest request) {
        return factory.createEnemy(request);
    }
}
