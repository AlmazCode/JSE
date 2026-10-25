
package jse.factory;

import jse.world.ThemeId;
import jse.world.Player;
import jse.world.Enemy;
import jse.world.Pickup;

public interface GameObjectFamilyFactory {

    ThemeId familyId();

    Player createPlayer(SpawnRequest request);

    Enemy createEnemy(SpawnRequest request);

    Pickup createPickup(SpawnRequest request);
}
