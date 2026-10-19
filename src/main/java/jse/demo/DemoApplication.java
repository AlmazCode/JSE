package jse.demo;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.SwingUtilities;
import jse.assets.AssetManager;
import jse.core.JseEngine;
import jse.core.Scene;
import jse.platform.awt.SwingInputSource;
import jse.platform.awt.SwingHost;
import jse.render.RenderMode;
import jse.render.awt.FilledRenderer;
import jse.render.awt.WireframeRenderer;

public final class DemoApplication {
    private DemoApplication() {}
    public static void main(String[] args) {
        try {
            var arguments = LaunchArguments.parse(args);
            if (arguments.help()) {
                System.out.println("Usage: java -jar jse-demo.jar [--example preview|showcase] [--config FILE]\n"
                        + "Launch the JSE runtime preview (default) or rendering showcase.\n"
                        + "FILE is a UTF-8 properties override.\n"
                        + "Preview: WASD / arrows, P pause, R reset, F1 renderer, Escape close.\n"
                        + "Showcase: F1 renderer, F2 outline, F3 health, T theme, Escape close.");
                return;
            }
            var settings = LaunchSettings.load(arguments.config(), arguments.example());
            var scene = arguments.createScene(settings);
            if (GraphicsEnvironment.isHeadless())
                throw new IllegalStateException("The selected example requires a desktop display");
            var assets = new AssetManager(DemoApplication.class.getClassLoader(), settings.placeholder());
            var preload = new ArrayList<>(settings.preload());
            if (arguments.example() == LaunchArguments.Example.SHOWCASE)
                preload.addAll(settings.showcase().sprites());
            assets.preload(preload);
            SwingUtilities.invokeLater(() -> launch(settings, assets, scene, arguments.example()));
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            System.err.println("Unable to start JSE: " + exception.getMessage());
            System.exit(2);
        }
    }

    private static void launch(LaunchSettings settings, AssetManager assets, Scene scene, LaunchArguments.Example example) {
        var input = new SwingInputSource(settings.keys());
        var hostSettings = settings.host();
        if (example == LaunchArguments.Example.SHOWCASE)
            hostSettings = new jse.platform.awt.HostSettings("JSE | Rendering Showcase",
                    hostSettings.background(), hostSettings.initialMode());
        var host = new SwingHost(hostSettings, input,
                Map.of(RenderMode.FILLED, new FilledRenderer(assets, settings.render()), RenderMode.WIREFRAME,
                        new WireframeRenderer(assets, settings.render())));
        var engine = new JseEngine(settings.engine(), host, input);
        engine.start(scene);
    }
}
