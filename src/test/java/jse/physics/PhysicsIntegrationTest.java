package jse.physics;

import java.util.List;
import java.util.Optional;
import jse.GameFixtures;
import jse.event.EventBus;
import jse.factory.*;
import jse.math.*;
import jse.world.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhysicsIntegrationTest {
    @Test void collisionsAreSortedDistinctAndExcludeEdgeTouching() {
        var view=new WorldView(List.of(
                new EntityView(3,ObjectKind.ENEMY,ThemeId.FOREST,new Vec2(5,5),new Rect(5,5,10,10)),
                new EntityView(1,ObjectKind.PLAYER,ThemeId.FOREST,Vec2.ZERO,new Rect(0,0,10,10)),
                new EntityView(2,ObjectKind.PICKUP,ThemeId.FOREST,new Vec2(10,0),new Rect(10,0,10,10))),
                Optional.of(Vec2.ZERO),new Rect(0,0,100,100));
        assertEquals(List.of(new CollisionPair(1,3),new CollisionPair(2,3)),new CollisionSystem().findOverlaps(view));
    }
    @Test void motionClampsTheWholeEntityAndRejectsInvalidTiming() {
        var world=new World(1,new Rect(0,0,200,160),ThemeId.FOREST,4,new EventBus());
        var player=new ForestFactory(new GameFixtures.CaptureRenderer(),GameFixtures.entities(),GameFixtures.sprites("forest"))
                .createPlayer(new SpawnRequest(new Vec2(170,20),new Vec2(24,24)));
        world.enqueueAdd(player);world.commitChanges(0);player.setVelocity(new Vec2(1000,-1000));
        var motion=new MotionSystem();motion.update(world,1);
        assertEquals(new Vec2(176,0),player.position());
        assertThrows(IllegalArgumentException.class,()->motion.update(world,0));
        assertThrows(IllegalArgumentException.class,()->motion.update(world,Double.NaN));
    }
}
