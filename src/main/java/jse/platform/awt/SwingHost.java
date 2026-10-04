package jse.platform.awt;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import jse.core.EngineConfig;
import jse.core.EngineHost;
import jse.render.Renderer;
import jse.render.RenderMode;
import jse.render.awt.FrameRenderer;

public final class SwingHost implements EngineHost {
    private final HostSettings settings;
    private final SwingInputSource input;
    private final Map<RenderMode, FrameRenderer> renderers;
    private RenderMode mode;
    private JFrame frame;
    private GamePanel panel;
    private javax.swing.Timer timer;
    private boolean opened;
    private boolean closed;

    public SwingHost(HostSettings settings, SwingInputSource input, Map<RenderMode, FrameRenderer> renderers) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.input = Objects.requireNonNull(input, "input");
        this.renderers = Map.copyOf(renderers);
        for (var candidate : RenderMode.values()) {
            if (!this.renderers.containsKey(candidate))
                throw new IllegalArgumentException("Missing renderer: " + candidate);
        }
        mode = settings.initialMode();
    }

    @Override
    public void open(EngineConfig config, LongConsumer pump, Consumer<Renderer> paint, Runnable focusLost,
            Runnable closeRequested, Consumer<RuntimeException> failed) {
        SwingInputSource.requireEdt();
        if (opened || closed)
            throw new IllegalStateException("Host can only open once");
        opened = true;
        frame = new JFrame(settings.title());
        panel = new GamePanel(config, settings.background(), () -> renderers.get(mode), paint, failed);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeRequested.run();
            }
            @Override
            public void windowDeactivated(WindowEvent event) {
                focusLost.run();
            }
        });
        frame.setContentPane(panel);
        input.bind(panel);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        timer = new javax.swing.Timer(config.timerDelayMillis(), event -> pump.accept(System.nanoTime()));
        timer.setCoalesce(true);
    }

    @Override
    public Renderer currentRenderer() {
        SwingInputSource.requireEdt();
        return renderers.get(mode);
    }

    @Override
    public void setRenderMode(RenderMode mode) {
        SwingInputSource.requireEdt();
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public void startTimer() {
        SwingInputSource.requireEdt();
        if (!opened || closed || timer == null)
            throw new IllegalStateException("Host is not open");
        frame.setVisible(true);
        panel.requestFocusInWindow();
        timer.start();
    }

    @Override
    public void requestRepaint() {
        SwingInputSource.requireEdt();
        if (!closed && panel != null)
            panel.repaint();
    }

    @Override
    public void showError(String message, Throwable cause) {
        SwingInputSource.requireEdt();
        if (timer != null)
            timer.stop();
        input.clear();
        System.getLogger(SwingHost.class.getName()).log(System.Logger.Level.ERROR, message, cause);
        SwingUtilities.invokeLater(() -> {
            if (closed || frame == null)
                return;
            input.unbind();
            var error = new JPanel(new BorderLayout());
            var text = new JTextArea(message + "\n\n" + cause.getClass().getSimpleName() + ": " + cause.getMessage()
                    + "\n\nClose this window and check the terminal for details.");
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            error.add(new JScrollPane(text), BorderLayout.CENTER);
            frame.setContentPane(error);
            frame.revalidate();
            frame.repaint();
            frame.setVisible(true);
        });
    }

    @Override
    public void close() {
        SwingInputSource.requireEdt();
        if (closed)
            return;
        closed = true;
        if (timer != null)
            timer.stop();
        try {
            input.unbind();
        } finally {
            if (frame != null)
                frame.dispose();
        }
    }
}
