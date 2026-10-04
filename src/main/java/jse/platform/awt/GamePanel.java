package jse.platform.awt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JPanel;
import jse.core.EngineConfig;
import jse.math.Rgba;
import jse.render.Renderer;
import jse.render.awt.FrameRenderer;

public final class GamePanel extends JPanel {
    private final Supplier<FrameRenderer> renderer;
    private final Consumer<Renderer> paint;
    private final Consumer<RuntimeException> failed;

    public GamePanel(EngineConfig config, Rgba background, Supplier<FrameRenderer> renderer, Consumer<Renderer> paint,
            Consumer<RuntimeException> failed) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.paint = Objects.requireNonNull(paint, "paint");
        this.failed = Objects.requireNonNull(failed, "failed");
        setPreferredSize(new Dimension(config.width(), config.height()));
        setBackground(new Color(background.red(), background.green(), background.blue(), background.alpha()));
        setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics original) {
        var graphics = (Graphics2D) original.create();
        FrameRenderer active = null;
        RuntimeException failure = null;
        try {
            super.paintComponent(graphics);
            active = renderer.get();
            active.beginFrame(graphics, getWidth(), getHeight());
            paint.accept(active);
        } catch (RuntimeException ex) {
            failure = ex;
        } finally {
            try {
                if (active != null)
                    active.endFrame();
            } catch (RuntimeException ex) {
                if (failure == null)
                    failure = ex;
                else if (failure != ex)
                    failure.addSuppressed(ex);
            } finally {
                graphics.dispose();
            }
        }
        if (failure != null)
            failed.accept(failure);
    }
}
