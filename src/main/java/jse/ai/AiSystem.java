package jse.ai;

import java.util.Objects;
import jse.event.BehaviorChanged;
import jse.event.EventBus;
import jse.math.Vec2;
import jse.world.Enemy;
import jse.world.World;
import jse.world.WorldView;

/**
 * Strategy client: applies every enemy's own strategy to its velocity and performs behavior
 * replacement. It never moves anything; positions belong to MotionSystem.
 */
public final class AiSystem {
    private final EventBus events;

    public AiSystem(EventBus events) {
        this.events = Objects.requireNonNull(events, "events");
    }

    /** @param view snapshot taken before this update so all enemies see the same world */
    public void update(World world, WorldView view, double dt) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(view, "view");
        world.forEachActive(object -> {
            if (object instanceof Enemy enemy) {
                Vec2 velocity = enemy.behavior().calculateMovement(enemy, view, dt);
                enemy.setVelocity(Objects.requireNonNull(velocity, "strategy returned null velocity"));
            }
        });
    }

    /**
     * Swaps the strategy of an active enemy at runtime. Id, position and family are untouched.
     *
     * @return true when a replacement happened (and BehaviorChanged was published)
     */
    public boolean changeBehavior(World world, long enemyId, BehaviorStrategy next, long tick) {
        Objects.requireNonNull(next, "next");
        var found = world.findActive(enemyId);
        if (found.isEmpty() || !(found.get() instanceof Enemy enemy) || enemy.behavior() == next)
            return false;
        enemy.setBehavior(next);
        events.publish(new BehaviorChanged(world.runId(), tick, enemyId, next.name()));
        return true;
    }
}
