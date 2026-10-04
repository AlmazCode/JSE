package jse.platform.awt;

import java.util.Objects;
import jse.math.Rgba;
import jse.render.RenderMode;

public record HostSettings(String title, Rgba background, RenderMode initialMode) {
    public HostSettings {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(background, "background");
        Objects.requireNonNull(initialMode, "initialMode");
        if (title.isBlank())
            throw new IllegalArgumentException("Window title must not be blank");
    }
}
