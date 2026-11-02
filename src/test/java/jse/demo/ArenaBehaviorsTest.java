package jse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jse.GameFixtures;
import jse.ai.*;
import jse.core.EngineServices;
import jse.input.*;
import jse.math.Vec2;
import jse.observer.*;
import jse.world.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaBehaviorsTest {
    private ArenaRun run() throws Exception {
        return new ArenaRun(1,ThemeId.FOREST,LaunchSettings.load(null,LaunchArguments.Example.GAME),new GameFixtures.CaptureRenderer());
    }
    private List<Enemy> enemies(ArenaRun run) {
        var result=new ArrayList<Enemy>();run.world().forEachActive(object->{if(object instanceof Enemy enemy)result.add(enemy);});return result;
    }
    private static InputState press(GameAction... keys) {return new InputState(Set.of(),Set.of(keys));}
    @Test void arenaAssignsThreeStrategiesAndAppliesThemBeforeMotion() throws Exception {
        try(var run=run()) {
            var enemies=enemies(run);
            assertInstanceOf(PatrolBehavior.class,enemies.get(0).behavior());
            assertInstanceOf(ChaseBehavior.class,enemies.get(1).behavior());
            assertInstanceOf(FleeBehavior.class,enemies.get(2).behavior());
            var before=enemies.get(0).position();
            var chaseBefore=enemies.get(1).position().subtract(run.player().position()).length();
            run.advance(0.1,press());
            assertTrue(enemies.get(0).position().x()>before.x());
            assertTrue(enemies.get(1).position().subtract(run.player().position()).length()<chaseBefore);
            assertEquals(3,run.hud().health());
        }
    }
    @Test void selectionAndRuntimeSwapPreserveEnemyIdentityAndWorkDuringPause() throws Exception {
        try(var run=run()) {
            var first=run.selectedEnemy().orElseThrow();long id=first.id();var position=first.position();
            run.togglePause();run.cycleBehavior();
            assertInstanceOf(ChaseBehavior.class,first.behavior());
            assertEquals(id,first.id());assertEquals(position,first.position());
            assertTrue(run.log().orElseThrow().lines().get(run.log().orElseThrow().lines().size()-1).contains("behavior -> Chase"));
            run.selectNextEnemy();assertNotEquals(id,run.selectedEnemy().orElseThrow().id());
            run.cycleBehavior();
            assertInstanceOf(FleeBehavior.class,run.selectedEnemy().orElseThrow().behavior());
        }
    }
    @Test void realRuleEventsFeedHudAndIndependentLogCanBeRemovedAndRecreated() throws Exception {
        try(var run=run()) {
            var old=run.log().orElseThrow();int initial=old.lines().size();
            run.setLogEnabled(false);run.cycleBehavior();
            assertEquals(initial,old.lines().size());assertTrue(run.log().isEmpty());
            var enemy=enemies(run).get(0);enemy.setBehavior(new IdleBehavior());enemy.setPosition(run.player().position());
            run.advance(0.1,press());
            assertEquals(2,run.player().currentHealth());assertEquals(2,run.hud().health());
            run.setLogEnabled(true);assertTrue(run.log().orElseThrow().lines().isEmpty());
            run.cycleBehavior();assertEquals(1,run.log().orElseThrow().lines().size());
            assertEquals(initial,old.lines().size());
        }
    }
    @Test void twentyRunLifetimesDoNotRetainObserverSubscriptions() throws Exception {
        var closed=new ArrayList<EventLogObserver>();var hud=new ArrayList<HudObserver>();
        var events=new ArrayList<jse.event.EventBus>();
        for(int i=0;i<20;i++) {
            var run=run();closed.add(run.log().orElseThrow());hud.add(run.hud());events.add(run.events());run.close();run.close();
        }
        for(int i=0;i<20;i++) {
            int old=closed.get(i).lines().size();
            events.get(i).publish(new jse.event.ItemCollected(1,1,1,99,5));
            assertEquals(old,closed.get(i).lines().size());assertEquals(0,hud.get(i).score());assertTrue(events.get(i).isClosed());
        }
    }
    @Test void sceneControlsExposeAiAndLogAndKeepPreferencesAcrossReset() throws Exception {
        var settings=LaunchSettings.load(null,LaunchArguments.Example.GAME);var scenes=new GameScenes(settings);
        var scene=scenes.arena();var control=new ArenaIntegrationTest.Control();var renderer=new GameFixtures.CaptureRenderer();
        var services=new EngineServices(settings.engine(),renderer,control);scene.onEnter(services);
        scene.update(0.1,press(GameAction.PAUSE));scene.update(0.1,press(GameAction.SELECT_ENEMY,GameAction.SWITCH_BEHAVIOR,GameAction.TOGGLE_LOG));
        scene.render(renderer);
        assertTrue(renderer.text.stream().anyMatch(text->text.contains("Flee")));
        assertTrue(renderer.text.stream().anyMatch(text->text.contains("log OFF")));
        assertFalse(scenes.logEnabled());
        scene.update(0.1,press(GameAction.RESTART));scene.onExit();
        var next=(ArenaScene)control.next;next.onEnter(services);assertTrue(next.run().log().isEmpty());next.onExit();
    }
}
