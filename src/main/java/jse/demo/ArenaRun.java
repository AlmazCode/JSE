package jse.demo;

import jse.event.EventBus;
import jse.factory.*;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Vec2;
import jse.physics.CollisionSystem;
import jse.physics.MotionSystem;
import jse.render.Renderer;
import jse.world.*;

/** Scene-owned model lifetime. Rendering and navigation have no simulation responsibilities. */
public final class ArenaRun implements AutoCloseable {
    private final EventBus events = new EventBus();
    private final World world;
    private final Player player;
    private final ArenaRules rules;
    private final MotionSystem motion = new MotionSystem();
    private final CollisionSystem collisions = new CollisionSystem();
    private long tick;
    private boolean paused, closed;

    public ArenaRun(long runId, ThemeId theme, LaunchSettings settings, Renderer renderer) {
        var game = settings.game();
        var paths = theme == ThemeId.FOREST ? settings.showcase().forestSprites() : settings.showcase().spaceSprites();
        var sprites = new FamilySprites(paths.get(0), paths.get(1), paths.get(2));
        GameObjectFamilyFactory factory = theme == ThemeId.FOREST
                ? new ForestFactory(renderer, game.entities(), sprites) : new SpaceFactory(renderer, game.entities(), sprites);
        world = new World(runId, game.arena(settings.engine(), settings.showcase().decorations()), theme, game.worldCapacity(), events);
        rules = new ArenaRules(game.rules(), events);
        player = new PlayerSpawner(factory).spawn(world, new SpawnRequest(
                game.position(world.arena(), 0, game.entities().player().size()), game.entities().player().size()));
        int index = 1;
        for (int i = 0; i < game.enemyCount(); i++) new EnemySpawner(factory).spawn(world, new SpawnRequest(
                game.position(world.arena(), index++, game.entities().enemy().size()), game.entities().enemy().size()));
        for (int i = 0; i < game.pickupCount(); i++) new PickupSpawner(factory).spawn(world, new SpawnRequest(
                game.position(world.arena(), index++, game.entities().pickup().size()), game.entities().pickup().size()));
        world.commitChanges(0);
    }
    public World world() { return world; }
    public Player player() { return player; }
    public ArenaRules rules() { return rules; }
    public EventBus events() { return events; }
    public boolean paused() { return paused; }
    public void togglePause() { paused = !paused; }
    public void onFocusLost() { paused = true; }
    public void advance(double dt, InputState input) {
        if (closed) throw new IllegalStateException("Arena run is closed");
        if (!Double.isFinite(dt) || dt <= 0) throw new IllegalArgumentException("Arena delta must be finite and positive");
        tick++;
        if (paused || rules.status() != ArenaRules.Status.RUNNING) return;
        int x = (input.isHeld(GameAction.MOVE_RIGHT) ? 1 : 0) - (input.isHeld(GameAction.MOVE_LEFT) ? 1 : 0);
        int y = (input.isHeld(GameAction.MOVE_DOWN) ? 1 : 0) - (input.isHeld(GameAction.MOVE_UP) ? 1 : 0);
        player.setVelocity(new Vec2(x, y).normalized().scale(player.speed()));
        motion.update(world, dt);
        rules.applyCollisions(world, collisions.findOverlaps(world.view()), dt, tick);
        world.commitChanges(tick);
        rules.evaluateOutcome(world, tick);
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        events.close();
        world.close();
    }
}
