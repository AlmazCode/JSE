package jse.ai;

import java.util.Objects;
import jse.world.Enemy;
import jse.world.WorldView;

/** Argument checks shared by the concrete strategies. */
final class Behaviors {
    private Behaviors() {
    }

    static void require(Enemy enemy, WorldView world, double dt) {
        Objects.requireNonNull(enemy, "enemy");
        Objects.requireNonNull(world, "world");
        if (!(dt > 0) || !Double.isFinite(dt))
            throw new IllegalArgumentException("AI delta must be finite and positive");
    }
}
