package jse.physics;

import jse.world.World;

public final class MotionSystem {

    public void update(World world, double dt) {
        if (!Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Motion delta must be finite and positive");
        }
        world.forEachActive(object -> object.setPosition(world.arena()
                .clampPosition(object.position().add(object.velocity().scale(dt)), object.size())));
    }
}
