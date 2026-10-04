package jse.core;

import jse.input.InputState;
import jse.render.Renderer;

public interface Scene {
    void onEnter(EngineServices services);
    void update(double dt, InputState input);
    void render(Renderer renderer);
    void onFocusLost();
    void onExit();
}
