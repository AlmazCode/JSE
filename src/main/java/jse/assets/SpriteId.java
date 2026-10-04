package jse.assets;

import java.util.Objects;

public record SpriteId(String path) {
    public SpriteId {
        Objects.requireNonNull(path, "path");
        if (path.isBlank() || path.startsWith("/") || path.contains("\\")
                || java.util.Arrays.stream(path.split("/", -1))
                        .anyMatch(part -> part.isEmpty() || part.equals(".") || part.equals("..")))
            throw new IllegalArgumentException("Sprite path must be a relative classpath resource");
    }
}
