
package jse.factory;

import java.util.Objects;

import jse.world.GameObject;
import jse.world.ObjectKind;
import jse.world.World;

public abstract class EntitySpawner<T extends GameObject> {

    protected final GameObjectFamilyFactory factory;
    private final ObjectKind expectedKind;

    protected EntitySpawner(
            GameObjectFamilyFactory factory,
            ObjectKind expectedKind) {

        this.factory = Objects.requireNonNull(factory);
        this.expectedKind = Objects.requireNonNull(expectedKind);
    }

    public final T spawn(World world, SpawnRequest request) {

        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

        T object = Objects.requireNonNull(
            createObject(request),
            "Factory returned null"
        );

        if (object.category() != expectedKind) {
            throw new IllegalStateException(
                "Factory returned incorrect object category"
            );
        }

        if (object.familyId() != factory.familyId()
                || object.familyId() != world.familyId()) {
            throw new IllegalStateException(
                "Factory returned incorrect object family"
            );
        }

        world.enqueueAdd(object);

        return object;
    }

    protected abstract T createObject(SpawnRequest request);
}
