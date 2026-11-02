package jse.physics;

public record CollisionPair(long firstId, long secondId) {
    public CollisionPair {
        if (firstId <= 0 || secondId <= firstId) {
            throw new IllegalArgumentException("Collision IDs must be positive and ordered");
        }
    }
}
