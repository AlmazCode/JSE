
package jse.world;

import java.util.Objects;
import jse.factory.PickupSettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;

public abstract class Pickup extends GameObject {

    private final int value;

    protected Pickup(
            ThemeId familyId,
            SpawnRequest request,
            Graphic graphic,
            PickupSettings settings) {

        super(ObjectKind.PICKUP, familyId, request, graphic);

        Objects.requireNonNull(settings, "Settings cannot be null");
        this.value = settings.value();
    }

    public int value() {
        return value;
    }
}
