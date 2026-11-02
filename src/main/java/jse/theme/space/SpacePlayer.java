package jse.theme.space;

import jse.world.Player;
import jse.world.ThemeId;
import jse.factory.SpawnRequest;
import jse.factory.PlayerSettings;
import jse.render.Graphic;

public final class SpacePlayer extends Player {

    public SpacePlayer(
            SpawnRequest request,
            Graphic graphic,
            PlayerSettings settings) {

        super(ThemeId.SPACE, request, graphic, settings);
    }
}
