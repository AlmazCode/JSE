package jse.demo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import jse.render.DecorationSettings;
import jse.render.Graphic;
import jse.render.GraphicLayers;
import jse.render.Renderer;
import jse.render.VisualPreferences;
import jse.world.GameObject;
import jse.world.Player;

/**
 * Bridges the game model to graphics without retaining a World or a frame context.
 */
public final class EntityGraphics implements AutoCloseable {

    private record Applied(boolean outline, boolean healthBar, Graphic graphic) {
    }

    private final DecorationSettings settings;
    private final Map<GameObject, Applied> applied = new IdentityHashMap<>();

    public EntityGraphics(DecorationSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public void render(Iterable<? extends GameObject> objects, Renderer renderer,
            VisualPreferences preferences) {
        Objects.requireNonNull(objects, "objects");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(preferences, "preferences");
        var seen = Collections.newSetFromMap(new IdentityHashMap<GameObject, Boolean>());
        for (var object : objects) {
            seen.add(object);
            var previous = applied.get(object);
            if (previous == null || previous.outline() != preferences.outline()
                    || previous.healthBar() != preferences.healthBar()
                    || object.graphic() != previous.graphic()) {
                var graphic = GraphicLayers.compose(object.baseGraphic(),
                        object instanceof Player player ? player : null, preferences.outline(),
                        preferences.healthBar(), settings);
                object.setGraphic(graphic);
                applied.put(object,
                        new Applied(preferences.outline(), preferences.healthBar(), graphic));
            }
            object.graphic().setRenderer(renderer);
            object.graphic().draw(object.bounds());
        }
        applied.keySet().removeIf(object -> {
            if (seen.contains(object)) {
                return false;
            }
            object.setGraphic(object.baseGraphic());
            return true;
        });
    }

    @Override
    public void close() {
        for (var object : applied.keySet()) {
            object.setGraphic(object.baseGraphic());
        }
        applied.clear();
    }
}
