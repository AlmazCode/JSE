package jse.theme.forest;

import jse.world.Pickup;
import jse.world.ThemeId;
import jse.factory.SpawnRequest;
import jse.factory.PickupSettings;
import jse.render.Graphic;

public final class ForestPickup extends Pickup {

    public ForestPickup(
            SpawnRequest request,
            Graphic graphic,
            PickupSettings settings) {

        super(ThemeId.FOREST, request, graphic, settings);
    }
}
