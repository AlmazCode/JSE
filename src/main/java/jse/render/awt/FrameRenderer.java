package jse.render.awt;

import java.awt.Graphics2D;
import jse.render.Renderer;

public interface FrameRenderer extends Renderer {
    void beginFrame(Graphics2D graphics, int width, int height);
    void endFrame();
}
