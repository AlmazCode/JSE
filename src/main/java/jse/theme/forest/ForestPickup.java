package jse.theme.forest;

import jse.factory.PickupSettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;
import jse.world.Pickup;
import jse.world.ThemeId;

public final class ForestPickup extends Pickup {

    public ForestPickup(SpawnRequest request, Graphic graphic, PickupSettings settings) {
        super(ThemeId.FOREST, request, graphic, settings);
    }
}
