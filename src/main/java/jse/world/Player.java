
package jse.world;

import jse.factory.SpawnRequest;
import jse.factory.PlayerSettings;
import jse.render.Graphic;
import jse.render.HealthView;

public abstract class Player extends GameObject implements HealthView {

    private final double speed;
    private final int maxHealth;
    private int currentHealth;

    protected Player(
            ThemeId familyId,
            SpawnRequest request,
            Graphic graphic,
            PlayerSettings settings) {

        super(ObjectKind.PLAYER, familyId, request, graphic);

        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }

        if (settings.speed() <= 0 || !Double.isFinite(settings.speed())) {
            throw new IllegalArgumentException("Speed must be positive");
        }

        if (settings.maxHealth() <= 0) {
            throw new IllegalArgumentException("Max health must be positive");
        }

        this.speed = settings.speed();
        this.maxHealth = settings.maxHealth();
        this.currentHealth = maxHealth;
    }

    public double speed() {
        return speed;
    }

    @Override
    public int currentHealth() {
        return currentHealth;
    }

    @Override
    public int maxHealth() {
        return maxHealth;
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Damage must be positive");
        }

        currentHealth = Math.max(0, currentHealth - amount);
    }
}
