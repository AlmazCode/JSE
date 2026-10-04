package jse.math;

public record Vec2(double x, double y) {
    public static final Vec2 ZERO = new Vec2(0, 0);
    public Vec2 {
        if (!Double.isFinite(x) || !Double.isFinite(y))
            throw new IllegalArgumentException("Vector components must be finite");
    }

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 subtract(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 scale(double factor) {
        return new Vec2(x * factor, y * factor);
    }

    public double length() {
        return Math.hypot(x, y);
    }

    public Vec2 normalized() {
        double largest = Math.max(Math.abs(x), Math.abs(y));
        if (largest == 0)
            return ZERO;
        double sx = x / largest, sy = y / largest;
        double length = Math.hypot(sx, sy);
        return new Vec2(sx / length, sy / length);
    }
}
