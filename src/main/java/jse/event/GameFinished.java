package jse.event;

import jse.demo.ArenaRules;

public record GameFinished(long runId, long tick, ArenaRules.Status result,
        int finalScore) implements GameEvent {

    public GameFinished {
        if (runId <= 0 || tick < 0 || result == null || result == ArenaRules.Status.RUNNING
                || finalScore < 0) {
            throw new IllegalArgumentException("Invalid finish event");
        }
    }
}
