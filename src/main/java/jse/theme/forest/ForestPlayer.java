package jse.theme.forest;

import jse.world.Player;
import jse.world.ThemeId;
import jse.factory.SpawnRequest;
import jse.factory.PlayerSettings;
import jse.render.Graphic;

public final class ForestPlayer extends Player {

    public ForestPlayer(
            SpawnRequest request,
            Graphic graphic,
            PlayerSettings settings) {

        super(ThemeId.FOREST, request, graphic, settings);
    }
}
