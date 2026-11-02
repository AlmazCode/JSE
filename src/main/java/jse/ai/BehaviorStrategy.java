package jse.ai;

import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.WorldView;

/** Chooses an enemy's velocity from the same pre-motion world snapshot. */
@FunctionalInterface
public interface BehaviorStrategy {
    /**
     * Returns velocity in world units per second. The motion system applies it after all strategies
     * have run; this method must not move the enemy itself.
     */
    Vec2 desiredVelocity(Enemy enemy, WorldView world, double dt);

    /** Name included in the HUD and behavior-change events. */
    default String name() {
        return getClass().getSimpleName().replace("Behavior", "");
    }
}
