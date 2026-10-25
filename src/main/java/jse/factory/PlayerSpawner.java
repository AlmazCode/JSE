
package jse.factory;

import jse.world.ObjectKind;
import jse.world.Player;

public final class PlayerSpawner extends EntitySpawner<Player> {

    public PlayerSpawner(GameObjectFamilyFactory factory) {
        super(factory, ObjectKind.PLAYER);
    }

    @Override
    protected Player createObject(SpawnRequest request) {
        return factory.createPlayer(request);
    }
}
