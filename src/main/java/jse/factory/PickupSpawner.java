
package jse.factory;

import jse.world.ObjectKind;
import jse.world.Pickup;

public final class PickupSpawner extends EntitySpawner<Pickup> {

    public PickupSpawner(GameObjectFamilyFactory factory) {
        super(factory, ObjectKind.PICKUP);
    }

    @Override
    protected Pickup createObject(SpawnRequest request) {
        return factory.createPickup(request);
    }
}
