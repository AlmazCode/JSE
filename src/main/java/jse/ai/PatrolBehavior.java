package jse.ai;

import java.util.Objects;
import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.WorldView;

/**
 * Walks back and forth between two waypoints. On the last step it slows down so it never
 * overshoots, and it switches to the other waypoint on the following update. Each enemy needs
 * its own instance because the current target is state.
 */
public final class PatrolBehavior implements BehaviorStrategy {
    private final Vec2 first;
    private final Vec2 second;
    private boolean headingToSecond = true;

    public PatrolBehavior(Vec2 first, Vec2 second) {
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
    }

    @Override
    public Vec2 calculateMovement(Enemy enemy, WorldView world, double dt) {
        Behaviors.require(enemy, world, dt);
        Vec2 toTarget = (headingToSecond ? second : first).subtract(enemy.position());
        double distance = toTarget.length();
        if (distance == 0) {
            headingToSecond = !headingToSecond;
            return Vec2.ZERO;
        }
        double step = Math.min(enemy.speed(), distance / dt);
        if (distance <= enemy.speed() * dt)
            headingToSecond = !headingToSecond;
        return toTarget.normalized().scale(step);
    }

    @Override
    public String name() {
        return "Patrol";
    }
}
