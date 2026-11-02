package jse.ai;

import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.WorldView;

public final class IdleBehavior implements BehaviorStrategy {
    @Override
    public Vec2 desiredVelocity(Enemy enemy, WorldView world, double dt) {
        Behaviors.require(enemy, world, dt);
        return Vec2.ZERO;
    }
}
