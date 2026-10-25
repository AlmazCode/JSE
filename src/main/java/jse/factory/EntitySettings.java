
package jse.factory;

import java.util.Objects;

public record EntitySettings(
        PlayerSettings player,
        EnemySettings enemy,
        PickupSettings pickup) {

    public EntitySettings {
        Objects.requireNonNull(player, "Player settings cannot be null");
        Objects.requireNonNull(enemy, "Enemy settings cannot be null");
        Objects.requireNonNull(pickup, "Pickup settings cannot be null");
    }
}
