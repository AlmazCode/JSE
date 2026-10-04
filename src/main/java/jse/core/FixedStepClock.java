package jse.core;

final class FixedStepClock {
    record FramePlan(int steps, double dt) {}
    private final EngineConfig config;
    private boolean initialized;
    private long previous;
    private double pending;
    private double dropped;

    FixedStepClock(EngineConfig config) {
        this.config = config;
    }
    FramePlan advance(long now) {
        double dt = config.fixedDeltaSeconds();
        if (!initialized) {
            initialized = true;
            previous = now;
            return new FramePlan(0, dt);
        }
        long elapsed = now - previous;
        if (elapsed < 0)
            throw new IllegalArgumentException("Clock must advance monotonically");
        previous = now;
        double seconds = elapsed / 1_000_000_000.0;
        double accepted = Math.min(seconds, config.maxFrameDeltaSeconds());
        dropped += seconds - accepted;
        pending += accepted;
        int available = (int) Math.floor(Math.nextUp(pending / dt));
        int steps = Math.min(available, config.maxUpdatesPerPump());
        dropped += (available - steps) * dt;
        pending = Math.max(0, pending - available * dt);
        return new FramePlan(steps, dt);
    }
    void resetAccumulator() {
        pending = 0;
    }
    double pendingSeconds() {
        return pending;
    }
    double droppedSeconds() {
        return dropped;
    }
}
