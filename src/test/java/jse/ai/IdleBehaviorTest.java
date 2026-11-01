package jse.ai;

import java.util.List;
import java.util.Optional;
import jse.GameFixtures;
import jse.factory.*;
import jse.math.*;
import jse.world.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdleBehaviorTest {
    @Test void defaultBehaviorUsesTheActualEnemyTypeAndReturnsFiniteStationaryIntent() {
        var enemy=new ForestFactory(new GameFixtures.CaptureRenderer(),GameFixtures.entities(),GameFixtures.sprites("forest"))
                .createEnemy(new SpawnRequest(new Vec2(10,20),new Vec2(24,24)));
        var view=new WorldView(List.of(),Optional.empty(),new Rect(0,0,200,160));
        assertEquals(Vec2.ZERO,enemy.behavior().desiredVelocity(enemy,view,0.1));
        assertEquals(Vec2.ZERO,enemy.behavior().calculateMovement(enemy,view,0.1));
        assertThrows(IllegalArgumentException.class,()->enemy.behavior().desiredVelocity(enemy,view,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->enemy.behavior().desiredVelocity(enemy,view,0));
    }
}
