import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import jse.assets.AssetManager;
import jse.core.EngineControl;
import jse.core.EngineServices;
import jse.core.JseEngine;
import jse.core.Scene;
import jse.demo.ArenaRun;
import jse.demo.ArenaScene;
import jse.demo.LaunchArguments;
import jse.demo.LaunchSettings;
import jse.demo.ShowcaseScene;
import jse.demo.TitleScene;
import jse.input.InputState;
import jse.platform.awt.HostSettings;
import jse.platform.awt.SwingHost;
import jse.platform.awt.SwingInputSource;
import jse.render.RenderMode;
import jse.render.Renderer;
import jse.render.awt.FilledRenderer;
import jse.render.awt.WireframeRenderer;
import jse.world.Enemy;
import jse.world.ThemeId;

/**
 * Display-only verification tool; not included in production JAR or headless tests.
 */
class DesktopVerification {

    static JFrame frame;

    static JComponent panel;

    static Scene current;

    static JseEngine engine;

    static Scene tracked(Scene delegate) {
        return new Scene() {

            public void onEnter(EngineServices services) {
                current = delegate;
                delegate.onEnter(new EngineServices(services.config(), services.initialRenderer(),
                        new EngineControl() {

                            public void requestScene(Scene next) {
                                services.control().requestScene(tracked(next));
                            }

                            public void setRenderMode(RenderMode mode) {
                                services.control().setRenderMode(mode);
                            }

                            public void stop() {
                                services.control().stop();
                            }
                        }));
            }

            public void update(double dt, InputState input) {
                delegate.update(dt, input);
            }

            public void render(Renderer renderer) {
                delegate.render(renderer);
            }

            public void onFocusLost() {
                delegate.onFocusLost();
            }

            public void onExit() {
                delegate.onExit();
            }
        };
    }

    static void edt(Runnable operation) throws Exception {
        SwingUtilities.invokeAndWait(operation);
    }

    static boolean check(BooleanSupplier predicate) throws Exception {
        var result = new java.util.concurrent.atomic.AtomicBoolean();
        edt(() -> result.set(predicate.getAsBoolean()));
        return result.get();
    }

    static void await(BooleanSupplier predicate) throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (!check(predicate)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Requested UI/model state was not reached");
            }
            Thread.sleep(20);
        }
    }

    static ArenaRun run() {
        return ((ArenaScene) current).run();
    }

    static void action(int code) throws Exception {
        edt(() -> {
            for (boolean released : new boolean[]{false, true}) {
                var id = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(code, 0, released));
                if (id == null) {
                    throw new AssertionError("Missing key binding: " + code);
                }
                panel.getActionMap().get(id).actionPerformed(null);
            }
        });
    }

    static void save(String name) throws Exception {
        var image = new AtomicReference<BufferedImage>();
        edt(() -> {
            var buffer = new BufferedImage(panel.getWidth(), panel.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            var graphics = buffer.createGraphics();
            try {
                panel.paint(graphics);
            } finally {
                graphics.dispose();
            }
            image.set(buffer);
        });
        ImageIO.write(image.get(), "png", Path.of("docs/screenshots/" + name + ".png").toFile());
    }

    public static void main(String[] args) throws Exception {
        var settings = LaunchSettings.load(null, LaunchArguments.Example.GAME);
        var assets = new AssetManager(DesktopVerification.class.getClassLoader(),
                settings.placeholder());
        assets.preload(settings.showcase().sprites());
        edt(() -> {
            var input = new SwingInputSource(settings.keys());
            var host = new SwingHost(
                    new HostSettings("JSE | Final verification", settings.host().background(),
                            settings.host().initialMode()),
                    input,
                    Map.of(RenderMode.FILLED, new FilledRenderer(assets, settings.render()),
                            RenderMode.WIREFRAME,
                            new WireframeRenderer(assets, settings.render(), false)));
            engine = new JseEngine(settings.engine(), host, input);
            engine.start(tracked(LaunchArguments.parse(new String[]{"--example", "game"})
                    .createScene(settings)));
            for (var candidate : Frame.getFrames()) {
                if (candidate instanceof JFrame window && window.isShowing()
                        && window.getTitle().equals("JSE | Final verification")) {
                    frame = window;
                }
            }
            panel = (JComponent) frame.getContentPane();
            frame.toFront();
            panel.requestFocusInWindow();
        });
        boolean nativeEnter;
        try {
            save("final-title");
            var robot = new Robot();
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            try {
                await(() -> current instanceof ArenaScene);
                nativeEnter = true;
            } catch (AssertionError failure) {
                action(KeyEvent.VK_ENTER);
                await(() -> current instanceof ArenaScene);
                nativeEnter = false;
            }
            save("final-arena");
            action(KeyEvent.VK_P);
            await(() -> run().paused());
            action(KeyEvent.VK_TAB);
            await(() -> run().selectedEnemy().orElseThrow().id() == 3);
            action(KeyEvent.VK_B);
            await(() -> run().selectedEnemy().orElseThrow()
                    .behavior() instanceof jse.ai.FleeBehavior);
            save("final-strategy");
            action(KeyEvent.VK_F1);
            Thread.sleep(70);
            save("final-wireframe");
            action(KeyEvent.VK_F1);
            Thread.sleep(70);
            action(KeyEvent.VK_L);
            await(() -> run().log().isEmpty());
            edt(() -> {
                var arena = run();
                arena.world().forEachActive(object -> {
                    if (object instanceof Enemy enemy) {
                        enemy.setBehavior(new jse.ai.IdleBehavior());
                    }
                });
                arena.selectedEnemy().orElseThrow().setPosition(arena.player().position());
            });
            action(KeyEvent.VK_P);
            await(() -> run().hud().health() == 2);
            action(KeyEvent.VK_P);
            await(() -> run().paused());
            save("final-health-log-off");
            action(KeyEvent.VK_L);
            await(() -> run().log().isPresent());
            if (!check(() -> run().log().orElseThrow().lines().isEmpty())) {
                throw new AssertionError("Reenabled log replayed events");
            }
            action(KeyEvent.VK_B);
            await(() -> run().log().orElseThrow().lines().size() == 1);
            save("final-observers");
            action(KeyEvent.VK_T);
            await(() -> run().world().familyId() == ThemeId.SPACE);
            action(KeyEvent.VK_P);
            await(() -> run().paused());
            save("final-space");
            action(KeyEvent.VK_ESCAPE);
            await(() -> current instanceof TitleScene);
            action(KeyEvent.VK_H);
            await(() -> current instanceof ShowcaseScene);
            save("final-showcase");
            action(KeyEvent.VK_F1);
            Thread.sleep(70);
            save("final-showcase-wireframe");
            action(KeyEvent.VK_F1);
            Thread.sleep(70);
            action(KeyEvent.VK_ESCAPE);
            await(() -> current instanceof TitleScene);
            for (int i = 0; i < 20; i++) {
                action(KeyEvent.VK_ENTER);
                await(() -> current instanceof ArenaScene);
                var previous = new AtomicReference<ArenaRun>();
                edt(() -> previous.set(run()));
                action(KeyEvent.VK_R);
                await(() -> current instanceof ArenaScene && run() != previous.get());
                if (!previous.get().events().isClosed()) {
                    throw new AssertionError("Reset leaked a run");
                }
                edt(() -> previous.set(run()));
                action(KeyEvent.VK_T);
                await(() -> current instanceof ArenaScene && run() != previous.get());
                if (!previous.get().events().isClosed()) {
                    throw new AssertionError("Theme switch leaked a run");
                }
                action(KeyEvent.VK_ESCAPE);
                await(() -> current instanceof TitleScene);
                action(KeyEvent.VK_H);
                await(() -> current instanceof ShowcaseScene);
                action(KeyEvent.VK_ESCAPE);
                await(() -> current instanceof TitleScene);
            }
            action(KeyEvent.VK_ESCAPE);
            await(() -> !frame.isDisplayable());
            String json = String.format(java.util.Locale.ROOT,
                    "{\"success\":true,\"route_cycles\":20,\"native_robot_enter\":%s,\"java\":\"%s\",\"platform\":\"%s\"}",
                    nativeEnter, System.getProperty("java.version"), System.getProperty("os.name"));
            Files.createDirectories(Path.of(".superpowers/sdd/2026-11-02-unify-and-report"));
            Files.writeString(
                    Path.of(".superpowers/sdd/2026-11-02-unify-and-report/desktop-results.json"),
                    json);
            System.out.println(json);
        } finally {
            edt(() -> engine.stop());
        }
    }
}
