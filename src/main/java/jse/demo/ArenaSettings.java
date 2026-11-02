package jse.demo;

public record ArenaSettings(int targetScore, double damageCooldown, int contactDamage) {
    public ArenaSettings {
        if (targetScore <= 0 || !Double.isFinite(damageCooldown) || damageCooldown <= 0
                || contactDamage <= 0) {
            throw new IllegalArgumentException("Arena rule settings must be finite and positive");
        }
    }
}
