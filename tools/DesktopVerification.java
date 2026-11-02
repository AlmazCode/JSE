import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import jse.ai.FleeBehavior;
import jse.ai.IdleBehavior;
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
public final class DesktopVerification {
    private JFrame frame;
    private JComponent panel;
    private Scene current;
    private JseEngine engine;

    private Scene tracked(Scene delegate) {
        return new Scene() {
            @Override

            public void onEnter(EngineServices services) {
                current = delegate;
                delegate.onEnter(new EngineServices(services.config(), services.initialRenderer(),
                        new EngineControl() {
                            @Override

                            public void requestScene(Scene next) {
                                services.control().requestScene(tracked(next));
                            }

                            @Override

                            public void setRenderMode(RenderMode mode) {
                                services.control().setRenderMode(mode);
                            }

                            @Override

                            public void stop() {
                                services.control().stop();
                            }
                        }));
            }

            @Override

            public void update(double dt, InputState input) {
                delegate.update(dt, input);
            }

            @Override

            public void render(Renderer renderer) {
                delegate.render(renderer);
            }

            @Override

            public void onFocusLost() {
                delegate.onFocusLost();
            }

            @Override

            public void onExit() {
                delegate.onExit();
            }
        };
    }

    private void edt(Runnable operation) throws Exception {
        SwingUtilities.invokeAndWait(operation);
    }

    private boolean check(BooleanSupplier predicate) throws Exception {
        var result = new AtomicBoolean();
        edt(() -> result.set(predicate.getAsBoolean()));
        return result.get();
    }

    private void await(BooleanSupplier predicate) throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (!check(predicate)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Requested UI/model state was not reached");
            }
            Thread.sleep(20);
        }
    }

    private ArenaRun run() {
        return ((ArenaScene) current).run();
    }

    private void action(int code) throws Exception {
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

    private void save(String name) throws Exception {
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
        new DesktopVerification().verify();
    }

    private void openWindow() throws Exception {
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
    }

    private void verify() throws Exception {
        openWindow();
        try {

            boolean nativeEnter = enterArena();
            verifyArenaControls();
            verifyNavigation();
            action(KeyEvent.VK_ESCAPE);
            await(() -> !frame.isDisplayable());
            writeResult(nativeEnter);
        } finally {
            edt(() -> engine.stop());
        }
    }

    private boolean enterArena() throws Exception {
        boolean nativeEnter;

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
        return nativeEnter;
    }

    private void verifyArenaControls() throws Exception {
        save("arena");
        action(KeyEvent.VK_P);
        await(() -> run().paused());
        action(KeyEvent.VK_TAB);
        await(() -> run().selectedEnemy().orElseThrow().id() == 3);
        action(KeyEvent.VK_B);
        await(() -> run().selectedEnemy().orElseThrow().behavior() instanceof FleeBehavior);

        action(KeyEvent.VK_F1);
        Thread.sleep(70);
        save("wireframe");
        action(KeyEvent.VK_F1);
        Thread.sleep(70);
        action(KeyEvent.VK_L);
        await(() -> run().log().isEmpty());
        edt(() -> {
            var arena = run();
            arena.world().forEachActive(object -> {
                if (object instanceof Enemy enemy) {
                    enemy.setBehavior(new IdleBehavior());
                }
            });
            arena.selectedEnemy().orElseThrow().setPosition(arena.player().position());
        });
        action(KeyEvent.VK_P);
        await(() -> run().hud().health() == 2);
        action(KeyEvent.VK_P);
        await(() -> run().paused());
        save("health-log-off");
        action(KeyEvent.VK_L);
        await(() -> run().log().isPresent());
        if (!check(() -> run().log().orElseThrow().lines().isEmpty())) {
            throw new AssertionError("Reenabled log replayed events");
        }
        action(KeyEvent.VK_B);
        await(() -> run().log().orElseThrow().lines().size() == 1);

    }

    private void verifyNavigation() throws Exception {
        action(KeyEvent.VK_T);
        await(() -> run().world().familyId() == ThemeId.SPACE);
        action(KeyEvent.VK_P);
        await(() -> run().paused());
        save("space");
        action(KeyEvent.VK_ESCAPE);
        await(() -> current instanceof TitleScene);
        action(KeyEvent.VK_H);
        await(() -> current instanceof ShowcaseScene);
        save("showcase");
        action(KeyEvent.VK_F1);
        Thread.sleep(70);

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
    }

    private void writeResult(boolean nativeEnter) throws Exception {
        var jar = Path
                .of(JseEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String jarHash = HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        String json = String.format(Locale.ROOT,
                "{\"success\":true,\"route_cycles\":20,\"native_robot_enter\":%s,"
                        + "\"java\":\"%s\",\"platform\":\"%s\",\"recorded_utc\":\"%s\",\"jar_sha256\":\"%s\"}",
                nativeEnter, System.getProperty("java.version"), System.getProperty("os.name"),
                Instant.now(), jarHash);
        Path output = Path.of("target/verification/desktop.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, json + System.lineSeparator());
        System.out.println(json);
    }
}
