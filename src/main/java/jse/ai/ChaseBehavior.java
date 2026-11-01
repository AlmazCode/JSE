package jse.ai;

import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.WorldView;

/** Moves straight towards the player; stands still without a player or on top of it. */
public final class ChaseBehavior implements BehaviorStrategy {
    @Override
    public Vec2 calculateMovement(Enemy enemy, WorldView world, double dt) {
        Behaviors.require(enemy, world, dt);
        if (world.playerPosition().isEmpty())
            return Vec2.ZERO;
        Vec2 toPlayer = world.playerPosition().get().subtract(enemy.position());
        if (toPlayer.length() == 0)
            return Vec2.ZERO;
        return toPlayer.normalized().scale(enemy.speed());
    }

    @Override
    public String name() {
        return "Chase";
    }
}
