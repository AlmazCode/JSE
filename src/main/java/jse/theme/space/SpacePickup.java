package jse.theme.space;

import jse.factory.PickupSettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;
import jse.world.Pickup;
import jse.world.ThemeId;

public final class SpacePickup extends Pickup {
    public SpacePickup(SpawnRequest request, Graphic graphic, PickupSettings settings) {
        super(ThemeId.SPACE, request, graphic, settings);
    }
}
