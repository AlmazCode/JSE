package jse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jse.ai.AiSystem;
import jse.ai.BehaviorStrategy;
import jse.ai.ChaseBehavior;
import jse.ai.FleeBehavior;
import jse.ai.PatrolBehavior;
import jse.event.EventBus;
import jse.factory.EnemySpawner;
import jse.factory.FamilySprites;
import jse.factory.ForestFactory;
import jse.factory.GameObjectFamilyFactory;
import jse.factory.PickupSpawner;
import jse.factory.PlayerSpawner;
import jse.factory.SpaceFactory;
import jse.factory.SpawnRequest;
import jse.input.GameAction;
import jse.input.InputState;
import jse.math.Vec2;
import jse.observer.EventLogObserver;
import jse.observer.HudObserver;
import jse.physics.CollisionSystem;
import jse.physics.MotionSystem;
import jse.render.Renderer;
import jse.world.Enemy;
import jse.world.Player;
import jse.world.ThemeId;
import jse.world.World;

/** Owns one arena's simulation, rules and event subscriptions. */
public final class ArenaRun implements AutoCloseable {
    private final EventBus events = new EventBus();
    private final World world;
    private final Player player;
    private final GameSettings game;
    private final AiSystem ai = new AiSystem(events);
    private final HudObserver hud;
    private final ArenaRules rules;
    private final MotionSystem motion = new MotionSystem();
    private final CollisionSystem collisions = new CollisionSystem();
    private EventLogObserver log;
    private long selectedEnemyId;
    private long tick;
    private boolean paused;
    private boolean closed;

    public ArenaRun(long runId, ThemeId theme, LaunchSettings settings, Renderer renderer) {
        this(runId, theme, settings, renderer, true);
    }

    public ArenaRun(long runId, ThemeId theme, LaunchSettings settings, Renderer renderer,
            boolean logEnabled) {
        game = settings.game();
        var factory = createFactory(theme, settings.showcase(), renderer);
        world = new World(runId, game.arena(settings.engine(), settings.showcase().decorations()),
                theme, game.worldCapacity(), events);
        rules = new ArenaRules(game.rules(), events);
        player = spawnEntities(factory);
        hud = new HudObserver(events, player);
        if (logEnabled) {
            log = new EventLogObserver(events, game.logCapacity());
        }
        // Observers must see the initial spawn events as well as later changes.
        world.commitChanges(0);
        assignInitialBehaviors();
    }

    public World world() {
        return world;
    }

    public Player player() {
        return player;
    }

    public ArenaRules rules() {
        return rules;
    }

    public EventBus events() {
        return events;
    }

    public HudObserver hud() {
        return hud;
    }

    public Optional<EventLogObserver> log() {
        return Optional.ofNullable(log);
    }

    public Optional<Enemy> selectedEnemy() {
        return world.findActive(selectedEnemyId).filter(Enemy.class::isInstance)
                .map(Enemy.class::cast);
    }

    public void selectNextEnemy() {
        var enemies = enemies();
        int index = -1;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).id() == selectedEnemyId) {
                index = i;
            }
        }
        selectedEnemyId = enemies.isEmpty() ? 0 : enemies.get((index + 1) % enemies.size()).id();
    }

    public void cycleBehavior() {
        selectedEnemy().ifPresent(
                enemy -> ai.changeBehavior(world, enemy.id(), nextBehavior(enemy), tick));
    }

    public void setLogEnabled(boolean enabled) {
        if (closed) {
            throw new IllegalStateException("Arena run is closed");
        }
        if (enabled && log == null) {
            log = new EventLogObserver(events, game.logCapacity());
        } else if (!enabled && log != null) {
            log.close();
            log = null;
        }
    }

    public boolean paused() {
        return paused;
    }

    public void togglePause() {
        paused = !paused;
    }

    public void onFocusLost() {
        paused = true;
    }

    public void advance(double dt, InputState input) {
        if (closed) {
            throw new IllegalStateException("Arena run is closed");
        }
        if (!Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Arena delta must be finite and positive");
        }
        tick++;
        if (paused || rules.status() != ArenaRules.Status.RUNNING) {
            return;
        }
        updatePlayerVelocity(input);
        ai.update(world, world.view(), dt);
        motion.update(world, dt);
        rules.applyCollisions(world, collisions.findOverlaps(world.view()), dt, tick);
        world.commitChanges(tick);
        rules.evaluateOutcome(world, tick);
    }

    private GameObjectFamilyFactory createFactory(ThemeId theme, ShowcaseSettings showcase,
            Renderer renderer) {
        var paths = theme == ThemeId.FOREST ? showcase.forestSprites() : showcase.spaceSprites();
        var sprites = new FamilySprites(paths.get(0), paths.get(1), paths.get(2));
        return theme == ThemeId.FOREST
                ? new ForestFactory(renderer, game.entities(), sprites)
                : new SpaceFactory(renderer, game.entities(), sprites);
    }

    private Player spawnEntities(GameObjectFamilyFactory factory) {
        var spawnedPlayer = new PlayerSpawner(factory).spawn(world,
                spawnRequest(0, game.entities().player().size()));
        int index = 1;
        for (int i = 0; i < game.enemyCount(); i++) {
            new EnemySpawner(factory).spawn(world,
                    spawnRequest(index++, game.entities().enemy().size()));
        }
        for (int i = 0; i < game.pickupCount(); i++) {
            new PickupSpawner(factory).spawn(world,
                    spawnRequest(index++, game.entities().pickup().size()));
        }
        return spawnedPlayer;
    }

    private SpawnRequest spawnRequest(int index, Vec2 size) {
        return new SpawnRequest(game.position(world.arena(), index, size), size);
    }

    private void assignInitialBehaviors() {
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

    private List<Enemy> enemies() {
        var result = new ArrayList<Enemy>();
        world.forEachActive(object -> {
            if (object instanceof Enemy enemy) {
                result.add(enemy);
            }
        });
        return result;
    }

    private BehaviorStrategy nextBehavior(Enemy enemy) {
        if (enemy.behavior() instanceof PatrolBehavior) {
            return new ChaseBehavior();
        }
        if (enemy.behavior() instanceof ChaseBehavior) {
            return new FleeBehavior(game.fleeRadius());
        }
        return patrol(enemy);
    }

    private PatrolBehavior patrol(Enemy enemy) {
        var offset = new Vec2(game.patrolDistance(), 0);
        var target = world.arena().clampPosition(enemy.position().add(offset), enemy.size());
        if (target.equals(enemy.position())) {
            target = world.arena().clampPosition(enemy.position().subtract(offset), enemy.size());
        }
        return new PatrolBehavior(enemy.position(), target);
    }

    private void updatePlayerVelocity(InputState input) {
        int x = (input.isHeld(GameAction.MOVE_RIGHT) ? 1 : 0)
                - (input.isHeld(GameAction.MOVE_LEFT) ? 1 : 0);
        int y = (input.isHeld(GameAction.MOVE_DOWN) ? 1 : 0)
                - (input.isHeld(GameAction.MOVE_UP) ? 1 : 0);
        player.setVelocity(new Vec2(x, y).normalized().scale(player.speed()));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        hud.close();
        if (log != null) {
            log.close();
        }
        events.close();
        world.close();
    }
}
