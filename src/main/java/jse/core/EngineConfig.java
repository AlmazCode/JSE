package jse.core;

public record EngineConfig(int width, int height, int targetUps, int maxUpdatesPerPump, double maxFrameDeltaSeconds,
        int timerDelayMillis) {
    public EngineConfig {
        if (width <= 0 || height <= 0 || targetUps <= 0 || maxUpdatesPerPump <= 0 || timerDelayMillis <= 0
                || !Double.isFinite(maxFrameDeltaSeconds) || maxFrameDeltaSeconds <= 0
                || maxFrameDeltaSeconds * targetUps > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Engine dimensions and timing must be positive and representable");
    }

    public double fixedDeltaSeconds() {
        return 1.0 / targetUps;
    }
}
