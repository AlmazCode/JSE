package jse.demo;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.SwingUtilities;
import jse.assets.AssetManager;
import jse.core.JseEngine;
import jse.core.Scene;
import jse.platform.awt.HostSettings;
import jse.platform.awt.SwingHost;
import jse.platform.awt.SwingInputSource;
import jse.render.RenderMode;
import jse.render.awt.FilledRenderer;
import jse.render.awt.WireframeRenderer;

public final class DemoApplication {
    private static final String HELP = """
            Usage: java -jar jse-demo.jar [--example preview|showcase|game] [--config FILE]
            Examples: preview (default), showcase, game (Arena and rendering showcase).
            FILE is a UTF-8 properties override.

            Preview: WASD/arrows move, P pause, R reset, F1 renderer, Escape close.
            Showcase: F1 renderer, F2 outline, F3 health bars, T theme, Escape back/close.
            Game menu: Enter Arena, H Showcase, Escape close.
            Arena: WASD/arrows move, P pause, R reset, T theme, Escape menu.
            Arena graphics: F1 renderer, F2 outline, F3 health bars.
            Arena AI/events: Tab select enemy, B change behavior, L toggle event log.
            """;

    private DemoApplication() {
    }

    public static void main(String[] args) {
        try {
            var arguments = LaunchArguments.parse(args);
            if (arguments.help()) {
                System.out.print(HELP);
                return;
            }
            var settings = LaunchSettings.load(arguments.config(), arguments.example());
            var scene = arguments.createScene(settings);
            if (GraphicsEnvironment.isHeadless()) {
                throw new IllegalStateException("The selected example requires a desktop display");
            }
            var assets = new AssetManager(DemoApplication.class.getClassLoader(),
                    settings.placeholder());
            var preload = new ArrayList<>(settings.preload());
            if (arguments.example() != LaunchArguments.Example.PREVIEW) {
                preload.addAll(settings.showcase().sprites());
            }
            assets.preload(preload);
            SwingUtilities.invokeLater(() -> launch(settings, assets, scene, arguments.example()));
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            System.err.println("Unable to start JSE: " + exception.getMessage());
            System.exit(2);
        }
    }

    private static void launch(LaunchSettings settings, AssetManager assets, Scene scene,
            LaunchArguments.Example example) {
        var input = new SwingInputSource(settings.keys());
        var hostSettings = settings.host();
        if (example == LaunchArguments.Example.SHOWCASE) {
            hostSettings = new HostSettings("JSE | Rendering Showcase", hostSettings.background(),
                    hostSettings.initialMode());
        }
        if (example == LaunchArguments.Example.GAME) {
            hostSettings = new HostSettings("JSE | Game", hostSettings.background(),
                    hostSettings.initialMode());
        }
        var host = new SwingHost(hostSettings, input,
                Map.of(RenderMode.FILLED, new FilledRenderer(assets, settings.render()),
                        RenderMode.WIREFRAME, new WireframeRenderer(assets, settings.render(),
                                example != LaunchArguments.Example.GAME)));
        var engine = new JseEngine(settings.engine(), host, input);
        engine.start(scene);
    }
}
