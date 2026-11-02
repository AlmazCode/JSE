package jse.theme.space;

import jse.factory.PlayerSettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;
import jse.world.Player;
import jse.world.ThemeId;

public final class SpacePlayer extends Player {

    public SpacePlayer(SpawnRequest request, Graphic graphic, PlayerSettings settings) {
        super(ThemeId.SPACE, request, graphic, settings);
    }
}
