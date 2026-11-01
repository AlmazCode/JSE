package jse.demo;

import java.awt.image.BufferedImage;
import java.util.List;
import jse.GameFixtures;
import jse.assets.AssetManager;
import jse.event.EventBus;
import jse.factory.*;
import jse.math.*;
import jse.render.*;
import jse.render.awt.*;
import jse.world.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntityGraphicsTest {
    @Test void livePlayerHealthAndRendererReplacementChangePixelsWithoutChangingTheModel() throws Exception {
        var settings=LaunchSettings.load(null);
        var assets=new AssetManager(getClass().getClassLoader(),settings.placeholder());
        assets.preload(settings.showcase().sprites());
        var style=new RenderStyle("Dialog",12,1,Rgba.parse("#FFFFFF"),false);
        var filled=new FilledRenderer(assets,style); var wire=new WireframeRenderer(assets,style);
        var player=new ForestFactory(filled,GameFixtures.entities(),GameFixtures.sprites("forest"))
                .createPlayer(new SpawnRequest(new Vec2(20,30),new Vec2(24,24)));
        var world=new World(1,new Rect(0,0,100,100),ThemeId.FOREST,4,new EventBus());
        world.enqueueAdd(player);world.commitChanges(0);
        var preferences=new VisualPreferences(RenderMode.FILLED);
        var graphics=new EntityGraphics(settings.showcase().decorations());
        var bounds=player.bounds();
        player.takeDamage(1);
        var image=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        var context=image.createGraphics();filled.beginFrame(context,100,100);
        graphics.render(List.of(player),filled,preferences);
        filled.endFrame();context.dispose();
        assertEquals(0xff4ade80,image.getRGB(21,21));
        assertEquals(0xff334155,image.getRGB(42,21));
        preferences.toggleRenderer();
        image=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        context=image.createGraphics();wire.beginFrame(context,100,100);
        graphics.render(List.of(player),wire,preferences);
        wire.endFrame();context.dispose();
        assertEquals(0xff4ade80,image.getRGB(21,20));
        assertEquals(1,player.id());assertEquals(2,player.currentHealth());assertEquals(bounds,player.bounds());
        assertEquals(RenderMode.WIREFRAME,preferences.mode());
    }
    @Test void repeatedTogglesHaveBoundedOutputAndOnlyPlayerReceivesHealth() throws Exception {
        var settings=LaunchSettings.load(null);var renderer=new GameFixtures.CaptureRenderer();
        var factory=new ForestFactory(renderer,GameFixtures.entities(),GameFixtures.sprites("forest"));
        var player=factory.createPlayer(new SpawnRequest(new Vec2(20,30),new Vec2(24,24)));
        var enemy=factory.createEnemy(new SpawnRequest(new Vec2(70,30),new Vec2(24,24)));
        var graphics=new EntityGraphics(settings.showcase().decorations());
        var preferences=new VisualPreferences(RenderMode.FILLED);
        for(int i=0;i<200;i++) {
            preferences.toggleOutline();preferences.toggleHealthBar();renderer.clear();
            graphics.render(List.of(player,enemy),renderer,preferences);
            assertEquals(i%2==0?0:10,renderer.rectangles.size());
            assertEquals(2,renderer.sprites.size());
        }
        preferences.toggleOutline();renderer.clear();
        graphics.render(List.of(player,enemy),renderer,preferences);
        assertEquals(2,renderer.rectangles.size());
        assertTrue(renderer.colors.stream().allMatch(c->c.equals(settings.showcase().decorations().barFill())
                ||c.equals(settings.showcase().decorations().barBackground())));
    }
    @Test void retiredEntitiesAndSceneExitReleaseAppliedGraphics() throws Exception {
        var settings=LaunchSettings.load(null);var renderer=new GameFixtures.CaptureRenderer();
        var factory=new SpaceFactory(renderer,GameFixtures.entities(),GameFixtures.sprites("space"));
        var player=factory.createPlayer(new SpawnRequest(new Vec2(20,30),new Vec2(24,24)));
        var enemy=factory.createEnemy(new SpawnRequest(new Vec2(70,30),new Vec2(24,24)));
        var graphics=new EntityGraphics(settings.showcase().decorations());
        var preferences=new VisualPreferences(RenderMode.FILLED);
        graphics.render(List.of(player,enemy),renderer,preferences);
        assertNotSame(enemy.baseGraphic(),enemy.graphic());
        graphics.render(List.of(player),renderer,preferences);
        assertSame(enemy.baseGraphic(),enemy.graphic());
        graphics.close();graphics.close();
        assertSame(player.baseGraphic(),player.graphic());
    }
}
