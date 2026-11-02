package jse.ai;

import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.WorldView;

/**
 * Runs away from the player while it is within the radius; stands still outside it.
 */
public final class FleeBehavior implements BehaviorStrategy {

    public static final double DEFAULT_RADIUS = 180;
    private final double radius;

    public FleeBehavior() {
        this(DEFAULT_RADIUS);
    }

    public FleeBehavior(double radius) {
        if (!(radius > 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be positive and finite");
        }
        this.radius = radius;
    }

    public double radius() {
        return radius;
    }

    @Override
    public Vec2 calculateMovement(Enemy enemy, WorldView world, double dt) {
        Behaviors.require(enemy, world, dt);
        if (world.playerPosition().isEmpty()) {
            return Vec2.ZERO;
        }
        Vec2 away = enemy.position().subtract(world.playerPosition().get());
        double distance = away.length();
        if (distance > radius) {
            return Vec2.ZERO;
        }
        if (distance == 0) {
            // exact overlap: stable rightward escape
            return new Vec2(enemy.speed(), 0);
        }
        return away.normalized().scale(enemy.speed());
    }

    @Override
    public String name() {
        return "Flee";
    }
}
