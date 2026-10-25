package jse.ai;

import jse.factory.Enemy;
import jse.world.WorldView;
import jse.math.Vec2;

/**
 * Strategy pattern interface for controlling entity AI decision making.
 */
@FunctionalInterface
public interface BehaviorStrategy {

    /**
     * Calculates the desired movement or directional velocity vector for the enemy.
     *
     * @param enemy the enemy entity executing this strategy
     * @param world read-only snapshot view of the world to query other entities
     * @param dt delta time for current fixed update step
     * @return directional velocity vector (Vec2)
     */
    Vec2 calculateMovement(Enemy enemy, WorldView world, double dt);
}
