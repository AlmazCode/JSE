package jse.core;

import java.util.function.Consumer;
import java.util.function.LongConsumer;
import jse.render.RenderMode;
import jse.render.Renderer;

public interface EngineHost {
    void open(EngineConfig config, LongConsumer pump, Consumer<Renderer> paint, Runnable focusLost,
            Runnable closeRequested, Consumer<RuntimeException> failed);

    Renderer currentRenderer();

    void setRenderMode(RenderMode mode);

    void startTimer();

    void requestRepaint();

    void showError(String message, Throwable cause);

    void close();
}
