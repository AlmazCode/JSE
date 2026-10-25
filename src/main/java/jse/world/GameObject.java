
package jse.world;

import java.util.Objects;
import jse.factory.SpawnRequest;
import jse.math.Vec2;
import jse.math.Rect;
import jse.render.Graphic;

public abstract class GameObject {

    private final ObjectKind category;
    private final ThemeId familyId;
    private Vec2 position;
    private final Vec2 size;
    private Vec2 velocity;

    private long id;
    private boolean active;
    private boolean pendingRemoval;

    private final Graphic baseGraphic;
    private Graphic graphic;

    protected GameObject(
            ObjectKind category,
            ThemeId familyId,
            SpawnRequest request,
            Graphic baseGraphic) {

        this.category = Objects.requireNonNull(category);
        this.familyId = Objects.requireNonNull(familyId);
        Objects.requireNonNull(request);

        this.position = request.position();
        this.size = request.size();
        this.velocity = new Vec2(0, 0);

        this.baseGraphic = Objects.requireNonNull(baseGraphic);
        this.graphic = baseGraphic;

        this.id = 0;
        this.active = false;
        this.pendingRemoval = false;
    }

    public long id() {
        return id;
    }

    public ObjectKind category() {
        return category;
    }

    public ThemeId familyId() {
        return familyId;
    }

    public Vec2 position() {
        return position;
    }

    public Vec2 size() {
        return size;
    }

    public Vec2 velocity() {
        return velocity;
    }

    public Rect bounds() {
        return new Rect(position.x(), position.y(),
                        size.x(), size.y());
    }

    public boolean active() {
        return active;
    }

    public boolean pendingRemoval() {
        return pendingRemoval;
    }

    public Graphic baseGraphic() {
        return baseGraphic;
    }

    public Graphic graphic() {
        return graphic;
    }

    public void setPosition(Vec2 position) {
        this.position = Objects.requireNonNull(position);
    }

    public void setVelocity(Vec2 velocity) {
        this.velocity = Objects.requireNonNull(velocity);
    }

    public void setGraphic(Graphic graphic) {
        this.graphic = Objects.requireNonNull(graphic);
    }

    void assignId(long id) {
        if (id <= 0 || this.id != 0) {
            throw new IllegalStateException("Invalid ID assignment");
        }
        this.id = id;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    void markPendingRemoval() {
        this.pendingRemoval = true;
    }
}
