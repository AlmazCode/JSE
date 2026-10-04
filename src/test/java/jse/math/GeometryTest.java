package jse.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    @Test
    void normalizationHandlesZeroAndExtremeFiniteVectors() {
        assertEquals(Vec2.ZERO, Vec2.ZERO.normalized());
        Vec2 direction = new Vec2(Double.MAX_VALUE, Double.MAX_VALUE).normalized();
        assertEquals(1, direction.length(), 1e-15);
        assertEquals(Math.sqrt(0.5), direction.x(), 1e-15);
        assertThrows(IllegalArgumentException.class, () -> new Vec2(Double.NaN, 1));
    }
    @Test
    void rectanglesRejectOverflowAndDistinguishTouchingFromOverlap() {
        Rect area = new Rect(0, 0, 100, 80);
        assertFalse(area.overlaps(new Rect(100, 0, 10, 10)));
        assertTrue(area.overlaps(new Rect(99, 0, 10, 10)));
        assertEquals(new Vec2(90, 0), area.clampPosition(new Vec2(200, -2), new Vec2(10, 10)));
        assertThrows(IllegalArgumentException.class, () -> new Rect(Double.MAX_VALUE, 0, Double.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> area.clampPosition(Vec2.ZERO, new Vec2(101, 10)));
    }
}
