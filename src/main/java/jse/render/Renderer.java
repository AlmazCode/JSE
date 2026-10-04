package jse.render;

import jse.assets.SpriteId;
import jse.math.Vec2;
import jse.math.Rect;
import jse.math.Rgba;

public interface Renderer {
    void drawSprite(SpriteId sprite, Rect bounds);
    void drawRectangle(Rect bounds, Rgba color);
    void drawOutline(Rect bounds, Rgba color);
    void drawText(String text, Vec2 position, Rgba color);
}
