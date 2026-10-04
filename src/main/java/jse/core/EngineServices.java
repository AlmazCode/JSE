package jse.core;

import java.util.Objects;
import jse.render.Renderer;

public record EngineServices(EngineConfig config, Renderer initialRenderer, EngineControl control) {
    public EngineServices {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(initialRenderer, "initialRenderer");
        Objects.requireNonNull(control, "control");
    }
}
