package jse.demo;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GameSettingsTest {
    @TempDir Path directory;
    @Test void gameLaunchHasMenuAndBackBindingsWithoutChangingStandaloneEscape() throws Exception {
        var args=LaunchArguments.parse(new String[]{"--example","game"});
        var game=LaunchSettings.load(null,args.example());
        assertInstanceOf(TitleScene.class,args.createScene(game));
        assertFalse(game.keys().label(jse.input.GameAction.BACK_TO_TITLE).isBlank());
        assertTrue(game.keys().label(jse.input.GameAction.QUIT).isBlank());
        var standalone=LaunchSettings.load(null,LaunchArguments.Example.SHOWCASE);
        assertFalse(standalone.keys().label(jse.input.GameAction.QUIT).isBlank());
        assertTrue(standalone.keys().label(jse.input.GameAction.BACK_TO_TITLE).isBlank());
    }
    @Test void invalidGameValuesAndClippedHelpFailBeforeAWindowOpens() throws Exception {
        var file=directory.resolve("invalid.properties");
        for(String property:new String[]{"game.player_health=0", "game.damage_cooldown=NaN", "game.enemy_count=17",
                "game.pickup_count=0", "game.world_capacity=1", "game.player_size=900", "window.width=400",
                "game.header_height=20", "game.padding=0", "game.line_height=70\ngame.header_height=374"}) {
            Files.writeString(file,property);
            assertThrows(IllegalArgumentException.class,()->{
                var settings=LaunchSettings.load(file,LaunchArguments.Example.GAME);
                LaunchArguments.parse(new String[]{"--example","game"}).createScene(settings);
            },property);
        }
    }
}
