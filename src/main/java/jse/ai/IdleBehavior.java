package jse.ai;

import java.util.Objects;
import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.WorldView;

public final class IdleBehavior implements BehaviorStrategy {
    @Override public Vec2 calculateMovement(Enemy enemy, WorldView world, double dt) {
        Objects.requireNonNull(enemy, "enemy");
        Objects.requireNonNull(world, "world");
        if (!Double.isFinite(dt) || dt <= 0) throw new IllegalArgumentException("AI delta must be finite and positive");
        return Vec2.ZERO;
    }
}
