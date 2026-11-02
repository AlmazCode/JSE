package jse.factory;

import jse.world.Enemy;
import jse.world.Pickup;
import jse.world.Player;
import jse.world.ThemeId;

public interface GameObjectFamilyFactory {
    ThemeId familyId();

    Player createPlayer(SpawnRequest request);

    Enemy createEnemy(SpawnRequest request);

    Pickup createPickup(SpawnRequest request);
}
