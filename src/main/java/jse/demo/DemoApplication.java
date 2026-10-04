package jse.demo;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.swing.SwingUtilities;
import jse.assets.AssetManager;
import jse.core.JseEngine;
import jse.platform.awt.SwingInputSource;
import jse.platform.awt.SwingHost;
import jse.render.RenderMode;
import jse.render.awt.FilledRenderer;
import jse.render.awt.WireframeRenderer;

public final class DemoApplication {
    private DemoApplication() {}
    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--help")) {
                System.out.println("Usage: java -jar jse-demo.jar [--config FILE]\n"
                        + "Launch the JSE runtime preview. FILE is a UTF-8 properties override.\n"
                        + "Default controls: WASD / arrows, P pause, R reset, F1 renderer, Escape close.");
                return;
            }
            Path overrides = null;
            if (args.length != 0) {
                if (args.length != 2 || !args[0].equals("--config"))
                    throw new IllegalArgumentException("Use --help or --config FILE");
                overrides = Path.of(args[1]);
            }
            var settings = LaunchSettings.load(overrides);
            if (GraphicsEnvironment.isHeadless())
                throw new IllegalStateException("The runtime preview requires a desktop display");
            var assets = new AssetManager(DemoApplication.class.getClassLoader(), settings.placeholder());
            assets.preload(settings.preload());
            SwingUtilities.invokeLater(() -> launch(settings, assets));
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            System.err.println("Unable to start JSE: " + exception.getMessage());
            System.exit(2);
        }
    }

    private static void launch(LaunchSettings settings, AssetManager assets) {
        var input = new SwingInputSource(settings.keys());
        var host = new SwingHost(settings.host(), input,
                Map.of(RenderMode.FILLED, new FilledRenderer(assets, settings.render()), RenderMode.WIREFRAME,
                        new WireframeRenderer(assets, settings.render())));
        var engine = new JseEngine(settings.engine(), host, input);
        engine.start(new PreviewScene(settings.preview(), settings.keys(), settings.host().initialMode()));
    }
}
