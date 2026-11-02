package jse.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import jse.event.BehaviorChanged;
import jse.event.EventBus;
import jse.math.Vec2;
import jse.physics.MotionSystem;
import jse.testing.Fixtures;
import jse.world.Enemy;
import jse.world.World;
import jse.world.WorldView;
import org.junit.jupiter.api.Test;

class BehaviorTest {

    private static final double DT = 1.0 / 60;
    private final EventBus bus = new EventBus();
    private final World world = Fixtures.world(bus);

    private Fixtures.TestEnemy addEnemy(double x, double y) {
        Fixtures.TestEnemy enemy = Fixtures.enemy(x, y);
        world.enqueueAdd(enemy);
        world.commitChanges(0);
        return enemy;
    }

    private Fixtures.TestPlayer addPlayer(double x, double y) {
        Fixtures.TestPlayer player = Fixtures.player(x, y);
        world.enqueueAdd(player);
        world.commitChanges(0);
        return player;
    }

    @Test
    void idleReturnsZeroAndIsTheDefault() {
        Fixtures.TestEnemy enemy = addEnemy(10, 10);
        assertInstanceOf(IdleBehavior.class, enemy.behavior());
        assertEquals(Vec2.ZERO, enemy.behavior().desiredVelocity(enemy, world.view(), DT));
    }

    @Test
    void chaseHeadsForPlayerAtEnemySpeed() {
        Fixtures.TestEnemy enemy = addEnemy(100, 100);
        addPlayer(100, 200);
        Vec2 velocity = new ChaseBehavior().desiredVelocity(enemy, world.view(), DT);
        assertEquals(0, velocity.x(), 1e-9);
        assertEquals(100, velocity.y(), 1e-9);
    }

    @Test
    void chaseStandsStillWithoutPlayerOrOnTopOfIt() {
        Fixtures.TestEnemy enemy = addEnemy(100, 100);
        assertEquals(Vec2.ZERO, new ChaseBehavior().desiredVelocity(enemy, world.view(), DT));
        addPlayer(100, 100);
        assertEquals(Vec2.ZERO, new ChaseBehavior().desiredVelocity(enemy, world.view(), DT));
    }

    @Test
    void fleeRunsAwayInsideRadiusAndStopsOutsideIt() {
        Fixtures.TestEnemy enemy = addEnemy(200, 100);
        addPlayer(100, 100);
        FleeBehavior flee = new FleeBehavior(180);
        Vec2 velocity = flee.desiredVelocity(enemy, world.view(), DT);
        assertEquals(100, velocity.x(), 1e-9);
        assertEquals(0, velocity.y(), 1e-9);
        enemy.setPosition(new Vec2(300, 100));
        assertEquals(Vec2.ZERO, flee.desiredVelocity(enemy, world.view(), DT),
                "distance 200 > radius 180");
    }

    @Test
    void fleeOnExactOverlapChoosesStableRightwardDirection() {
        Fixtures.TestEnemy enemy = addEnemy(100, 100);
        addPlayer(100, 100);
        assertEquals(new Vec2(100, 0), new FleeBehavior().desiredVelocity(enemy, world.view(), DT));
    }

    @Test
    void fleeAndPatrolRejectInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new FleeBehavior(0));
        assertThrows(IllegalArgumentException.class, () -> new FleeBehavior(Double.NaN));
        assertThrows(NullPointerException.class, () -> new PatrolBehavior(Vec2.ZERO, null));
    }

    @Test
    void everyStrategyRejectsInvalidDt() {
        Fixtures.TestEnemy enemy = addEnemy(10, 10);
        WorldView view = world.view();
        for (BehaviorStrategy strategy : List.of(new IdleBehavior(), new ChaseBehavior(),
                new FleeBehavior(), new PatrolBehavior(Vec2.ZERO, new Vec2(5, 5)))) {
            assertThrows(IllegalArgumentException.class,
                    () -> strategy.desiredVelocity(enemy, view, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> strategy.desiredVelocity(enemy, view, Double.NaN));
        }
    }

    @Test
    void patrolSlowsDownOnArrivalAndTurnsAround() {
        Fixtures.TestEnemy enemy = addEnemy(100, 100);
        PatrolBehavior patrol = new PatrolBehavior(new Vec2(100, 100), new Vec2(100.5, 100));
        enemy.setBehavior(patrol);
        Vec2 arrival = patrol.desiredVelocity(enemy, world.view(), DT);
        assertEquals(0.5 / DT, arrival.x(), 1e-6,
                "must not overshoot: distance / dt, not full speed");
        enemy.setPosition(new Vec2(100.5, 100));
        Vec2 back = patrol.desiredVelocity(enemy, world.view(), DT);
        assertTrue(back.x() < 0, "target switches for the following update");
    }

    @Test
    void patrolWalksBetweenWaypointsWithoutOvershootingOverManySteps() {
        Fixtures.TestEnemy enemy = addEnemy(50, 50);
        Vec2 a = new Vec2(50, 50);
        Vec2 b = new Vec2(250, 50);
        enemy.setBehavior(new PatrolBehavior(a, b));
        AiSystem ai = new AiSystem(bus);
        MotionSystem motion = new MotionSystem();
        boolean reachedB = false;
        boolean returnedToA = false;
        for (int step = 0; step < 1200; step++) {
            ai.update(world, world.view(), DT);
            motion.update(world, DT);
            assertTrue(enemy.position().x() >= 50 - 1e-9 && enemy.position().x() <= 250 + 1e-9,
                    "never leaves the waypoint segment, step " + step);
            if (Math.abs(enemy.position().x() - 250) < 1e-6) {
                reachedB = true;
            }
            if (reachedB && Math.abs(enemy.position().x() - 50) < 1e-6) {
                returnedToA = true;
            }
        }
        assertTrue(reachedB && returnedToA);
    }

    @Test
    void eachEnemyNeedsItsOwnPatrolStateAndKeepsIt() {
        Fixtures.TestEnemy first = addEnemy(50, 50);
        Fixtures.TestEnemy second = addEnemy(50, 150);
        PatrolBehavior p1 = new PatrolBehavior(new Vec2(50, 50), new Vec2(60, 50));
        PatrolBehavior p2 = new PatrolBehavior(new Vec2(50, 150), new Vec2(60, 150));
        first.setBehavior(p1);
        second.setBehavior(p2);
        // first reaches its target and turns around
        first.setPosition(new Vec2(60, 50));
        p1.desiredVelocity(first, world.view(), 1);
        assertTrue(p2.desiredVelocity(second, world.view(), DT).x() > 0,
                "second patrol is unaffected");
    }

    @Test
    void aiSystemAppliesEachEnemysOwnStrategyToItsVelocity() {
        Fixtures.TestEnemy chaser = addEnemy(100, 100);
        Fixtures.TestEnemy sleeper = addEnemy(300, 100);
        addPlayer(100, 200);
        chaser.setBehavior(new ChaseBehavior());
        new AiSystem(bus).update(world, world.view(), DT);
        assertEquals(100, chaser.velocity().y(), 1e-9);
        assertEquals(Vec2.ZERO, sleeper.velocity());
    }

    @Test
    void replacingTheStrategyAtRuntimeChangesBehaviorButNotIdentity() {
        Fixtures.TestEnemy enemy = addEnemy(100, 100);
        addPlayer(100, 200);
        AiSystem ai = new AiSystem(bus);
        long id = enemy.id();
        Vec2 position = enemy.position();
        assertTrue(ai.changeBehavior(world, id, new ChaseBehavior(), 3));
        ai.update(world, world.view(), DT);
        assertTrue(enemy.velocity().y() > 0, "chasing: towards the player below");
        assertTrue(ai.changeBehavior(world, id, new FleeBehavior(), 4));
        ai.update(world, world.view(), DT);
        assertTrue(enemy.velocity().y() < 0, "fleeing: away from the player");
        assertEquals(id, enemy.id());
        assertEquals(position, enemy.position());
        assertEquals(world.familyId(), enemy.familyId());
    }

    @Test
    void behaviorChangedIsPublishedOnlyForRealReplacements() {
        Fixtures.TestEnemy enemy = addEnemy(100, 100);
        List<BehaviorChanged> seen = new ArrayList<>();
        bus.subscribe(BehaviorChanged.class, seen::add);
        AiSystem ai = new AiSystem(bus);
        BehaviorStrategy chase = new ChaseBehavior();
        assertTrue(ai.changeBehavior(world, enemy.id(), chase, 7));
        assertFalse(ai.changeBehavior(world, enemy.id(), chase, 8),
                "same instance: no replacement");
        assertFalse(ai.changeBehavior(world, 999, new FleeBehavior(), 9), "unknown enemy");
        assertEquals(1, seen.size());
        assertEquals("Chase", seen.get(0).behaviorName());
        assertEquals(enemy.id(), seen.get(0).enemyId());
        assertEquals(7, seen.get(0).tick());
    }

    @Test
    void behaviorCannotBeChangedOnNonEnemies() {
        Fixtures.TestPlayer player = addPlayer(10, 10);
        assertFalse(new AiSystem(bus).changeBehavior(world, player.id(), new ChaseBehavior(), 1));
    }

    @Test
    void enemyRejectsNullBehavior() {
        Enemy enemy = addEnemy(10, 10);
        assertThrows(NullPointerException.class, () -> enemy.setBehavior(null));
    }
}
