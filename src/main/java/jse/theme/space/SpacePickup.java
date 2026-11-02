package jse.theme.space;

import jse.world.Pickup;
import jse.world.ThemeId;
import jse.factory.SpawnRequest;
import jse.factory.PickupSettings;
import jse.render.Graphic;

public final class SpacePickup extends Pickup {

    public SpacePickup(
            SpawnRequest request,
            Graphic graphic,
            PickupSettings settings) {

        super(ThemeId.SPACE, request, graphic, settings);
    }
}
