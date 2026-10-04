package jse.math;

public record Rect(double x, double y, double width, double height) {
    public Rect {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
                || width <= 0 || height <= 0 || !Double.isFinite(x + width) || !Double.isFinite(y + height))
            throw new IllegalArgumentException("Rectangle must have finite coordinates and positive dimensions");
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public Vec2 position() {
        return new Vec2(x, y);
    }

    public Vec2 size() {
        return new Vec2(width, height);
    }

    public boolean overlaps(Rect other) {
        return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
    }

    public boolean contains(Vec2 point) {
        return point.x() >= x && point.x() <= right() && point.y() >= y && point.y() <= bottom();
    }

    public Vec2 clampPosition(Vec2 position, Vec2 size) {
        if (size.x() <= 0 || size.y() <= 0 || size.x() > width || size.y() > height)
            throw new IllegalArgumentException("Object dimensions must fit inside the rectangle");
        return new Vec2(Math.max(x, Math.min(right() - size.x(), position.x())),
                Math.max(y, Math.min(bottom() - size.y(), position.y())));
    }
}
