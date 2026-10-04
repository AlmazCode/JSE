package jse.assets;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import javax.imageio.ImageIO;

public final class AssetManager {
    private final ClassLoader loader;
    private final BufferedImage placeholder;
    private final Map<SpriteId, BufferedImage> sprites = new HashMap<>();
    private final Set<SpriteId> warned = new HashSet<>();
    private final System.Logger logger = System.getLogger(AssetManager.class.getName());

    public AssetManager(ClassLoader loader, MissingSpriteStyle style) {
        this.loader = Objects.requireNonNull(loader, "loader");
        placeholder = new BufferedImage(style.width(), style.height(), BufferedImage.TYPE_INT_ARGB);
        var graphics = placeholder.createGraphics();
        try {
            var bg = style.background();
            graphics.setColor(new Color(bg.red(), bg.green(), bg.blue(), bg.alpha()));
            graphics.fillRect(0, 0, style.width(), style.height());
            var mark = style.mark();
            graphics.setColor(new Color(mark.red(), mark.green(), mark.blue(), mark.alpha()));
            graphics.drawLine(0, 0, style.width() - 1, style.height() - 1);
            graphics.drawLine(style.width() - 1, 0, 0, style.height() - 1);
        } finally {
            graphics.dispose();
        }
    }

    public void preload(Collection<SpriteId> ids) {
        for (var id : ids) sprites.computeIfAbsent(Objects.requireNonNull(id, "sprite"), this::load);
    }

    public BufferedImage get(SpriteId id) {
        Objects.requireNonNull(id, "sprite");
        var image = sprites.get(id);
        if (image != null)
            return image;
        return placeholder;
    }

    private BufferedImage load(SpriteId id) {
        try (var stream = loader.getResourceAsStream(id.path())) {
            if (stream != null) {
                var image = ImageIO.read(stream);
                if (image != null)
                    return image;
            }
            warn(id, "Sprite resource is missing or unsupported");
        } catch (IOException exception) {
            warn(id, "Unable to read sprite: " + exception.getMessage());
        }
        return placeholder;
    }

    private void warn(SpriteId id, String message) {
        if (warned.add(id))
            logger.log(System.Logger.Level.WARNING, message + ": " + id.path());
    }
}
