package jse.world;

import jse.math.Vec2;

/**
 * Read-only view of the game world for AI strategies and observers.
 */
public interface WorldView {

    /**
     * Gets the current position of the player, if present.
     */
    Vec2 getPlayerPosition();

    /**
     * Checks if a point is within world boundaries.
     */
    boolean isWithinBounds(Vec2 position);
}
