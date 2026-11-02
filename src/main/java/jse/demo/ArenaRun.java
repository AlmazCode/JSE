package jse.demo;

import jse.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jse.ai.*;
import jse.observer.HudObserver;
import jse.observer.EventLogObserver;
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
    private final GameSettings game;
    private final AiSystem ai = new AiSystem(events);
    private final HudObserver hud;
    private EventLogObserver log;
    private long selectedEnemyId;
    private final ArenaRules rules;
    private final MotionSystem motion = new MotionSystem();
    private final CollisionSystem collisions = new CollisionSystem();
    private long tick;
    private boolean paused, closed;

    public ArenaRun(long runId, ThemeId theme, LaunchSettings settings, Renderer renderer) {
        this(runId, theme, settings, renderer, true);
    }
    public ArenaRun(long runId, ThemeId theme, LaunchSettings settings, Renderer renderer, boolean logEnabled) {
        game = settings.game();
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
        hud = new HudObserver(events, player);
        if (logEnabled) log = new EventLogObserver(events, game.logCapacity());
        world.commitChanges(0);
        var enemies = enemies();
        for (int i = 0; i < enemies.size(); i++) {
            var enemy = enemies.get(i);
            BehaviorStrategy behavior = switch (i % 3) {
                case 0 -> patrol(enemy);
                case 1 -> new ChaseBehavior();
                default -> new FleeBehavior(game.fleeRadius());
            };
            ai.changeBehavior(world, enemy.id(), behavior, 0);
        }
        selectedEnemyId = enemies.isEmpty() ? 0 : enemies.get(0).id();
    }
    public World world() { return world; }
    public Player player() { return player; }
    public ArenaRules rules() { return rules; }
    public EventBus events() { return events; }
    public HudObserver hud() { return hud; }
    public Optional<EventLogObserver> log() { return Optional.ofNullable(log); }
    private List<Enemy> enemies() {
        var result = new ArrayList<Enemy>();
        world.forEachActive(object -> { if (object instanceof Enemy enemy) result.add(enemy); });
        return result;
    }
    private BehaviorStrategy patrol(Enemy enemy) {
        var target = world.arena().clampPosition(enemy.position().add(new Vec2(game.patrolDistance(), 0)), enemy.size());
        if (target.equals(enemy.position()))
            target = world.arena().clampPosition(enemy.position().subtract(new Vec2(game.patrolDistance(), 0)), enemy.size());
        return new PatrolBehavior(enemy.position(), target);
    }
    public Optional<Enemy> selectedEnemy() {
        return world.findActive(selectedEnemyId).filter(Enemy.class::isInstance).map(Enemy.class::cast);
    }
    public void selectNextEnemy() {
        var enemies = enemies();
        int index = -1;
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).id() == selectedEnemyId) index = i;
        selectedEnemyId = enemies.isEmpty() ? 0 : enemies.get((index + 1) % enemies.size()).id();
    }
    public void cycleBehavior() {
        selectedEnemy().ifPresent(enemy -> {
            BehaviorStrategy next = enemy.behavior() instanceof PatrolBehavior ? new ChaseBehavior()
                    : enemy.behavior() instanceof ChaseBehavior ? new FleeBehavior(game.fleeRadius()) : patrol(enemy);
            ai.changeBehavior(world, enemy.id(), next, tick);
        });
    }
    public void setLogEnabled(boolean enabled) {
        if (closed) throw new IllegalStateException("Arena run is closed");
        if (enabled && log == null) log = new EventLogObserver(events, game.logCapacity());
        else if (!enabled && log != null) { log.close(); log = null; }
    }
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
        ai.update(world, world.view(), dt);
        motion.update(world, dt);
        rules.applyCollisions(world, collisions.findOverlaps(world.view()), dt, tick);
        world.commitChanges(tick);
        rules.evaluateOutcome(world, tick);
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        hud.close();
        if (log != null) log.close();
        events.close();
        world.close();
    }
}
