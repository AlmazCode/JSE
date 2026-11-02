package jse.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import jse.factory.EntitySettings;
import jse.factory.PlayerSettings;
import jse.math.Vec2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameSettingsTest {
    @TempDir
    Path directory;

    @Test
    void gameLaunchHasMenuAndBackBindingsWithoutChangingStandaloneEscape() throws Exception {
        var args = LaunchArguments.parse(new String[]{"--example", "game"});
        var game = LaunchSettings.load(null, args.example());
        assertInstanceOf(TitleScene.class, args.createScene(game));
        assertFalse(game.keys().label(jse.input.GameAction.BACK_TO_TITLE).isBlank());
        assertTrue(game.keys().label(jse.input.GameAction.QUIT).isBlank());
        var standalone = LaunchSettings.load(null, LaunchArguments.Example.SHOWCASE);
        assertFalse(standalone.keys().label(jse.input.GameAction.QUIT).isBlank());
        assertTrue(standalone.keys().label(jse.input.GameAction.BACK_TO_TITLE).isBlank());
    }

    @Test
    void invalidGameValuesAndClippedHelpFailBeforeAWindowOpens() throws Exception {
        var file = directory.resolve("invalid.properties");
        for (String property : new String[]{"game.player_health=0", "game.damage_cooldown=NaN",
                "game.enemy_count=17", "game.pickup_count=0", "game.world_capacity=1",
                "game.player_size=900", "window.width=400", "game.header_height=20",
                "game.padding=0", "game.line_height=70\ngame.header_height=374"}) {
            Files.writeString(file, property);
            assertThrows(IllegalArgumentException.class, () -> {
                var settings = LaunchSettings.load(file, LaunchArguments.Example.GAME);
                LaunchArguments.parse(new String[]{"--example", "game"}).createScene(settings);
            }, property);
        }
    }

    @Test
    void rectangularEntitiesUseTheirOwnHeightForGridValidation() throws Exception {
        var defaults = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var tall = withPlayerSize(defaults, new Vec2(24, 300));
        assertThrows(IllegalArgumentException.class, () -> tall.game().validateLayout(tall));

        var wide = withPlayerSize(defaults, new Vec2(100, 24));
        assertDoesNotThrow(() -> wide.game().validateLayout(wide));
    }

    private LaunchSettings withPlayerSize(LaunchSettings defaults, Vec2 size) {
        var game = defaults.game();
        var entities = game.entities();
        var player = new PlayerSettings(size, entities.player().speed(),
                entities.player().maxHealth());
        var rectangular = new GameSettings(
                new EntitySettings(player, entities.enemy(), entities.pickup()), game.rules(),
                game.worldCapacity(), game.enemyCount(), game.pickupCount(), game.padding(),
                game.headerHeight(), game.lineHeight(), game.patrolDistance(), game.fleeRadius(),
                game.logCapacity(), game.logRows());
        return new LaunchSettings(defaults.engine(), defaults.host(), defaults.keys(),
                defaults.render(), defaults.placeholder(), defaults.preload(), defaults.preview(),
                defaults.showcase(), rectangular);
    }
}
