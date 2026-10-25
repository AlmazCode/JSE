
package jse.factory;

import java.util.Objects;
import jse.assets.SpriteId;

public record FamilySprites(
        SpriteId player,
        SpriteId enemy,
        SpriteId pickup) {

    public FamilySprites {
        Objects.requireNonNull(player, "Player sprite cannot be null");
        Objects.requireNonNull(enemy, "Enemy sprite cannot be null");
        Objects.requireNonNull(pickup, "Pickup sprite cannot be null");
    }
}
