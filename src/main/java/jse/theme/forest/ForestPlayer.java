package jse.theme.forest;

import jse.factory.PlayerSettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;
import jse.world.Player;
import jse.world.ThemeId;

public final class ForestPlayer extends Player {

    public ForestPlayer(SpawnRequest request, Graphic graphic, PlayerSettings settings) {
        super(ThemeId.FOREST, request, graphic, settings);
    }
}
