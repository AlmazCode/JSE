package jse.core;

import jse.render.RenderMode;

public interface EngineControl {
    void requestScene(Scene scene);
    void setRenderMode(RenderMode mode);
    void stop();
}
