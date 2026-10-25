
package jse.world;

import java.util.Objects;
import jse.ai.BehaviorStrategy;
import jse.ai.IdleBehavior;
import jse.factory.EnemySettings;
import jse.factory.SpawnRequest;
import jse.render.Graphic;

public abstract class Enemy extends GameObject {

    private final double speed;
    private BehaviorStrategy behavior;

    protected Enemy(
            ThemeId familyId,
            SpawnRequest request,
            Graphic graphic,
            EnemySettings settings) {

        super(ObjectKind.ENEMY, familyId, request, graphic);

        Objects.requireNonNull(settings, "Settings cannot be null");

        this.speed = settings.speed();
        this.behavior = new IdleBehavior();
    }

    public double speed() {
        return speed;
    }

    public BehaviorStrategy behavior() {
        return behavior;
    }

    public void setBehavior(BehaviorStrategy behavior) {
        this.behavior = Objects.requireNonNull(
            behavior, "Behavior cannot be null"
        );
    }
}
