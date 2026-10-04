# JSE Technical Specification

**Product:** Java Simple Engine, a small 2D desktop runtime with example applications.

**Current milestone:** Runtime foundation, version 0.1.0. **Target release:** Engine and Arena example, version 1.0.0.

**Contributors:** Sabirzhanov Emil; Baktiyarova Aruzhan; Roziyeva Yasmina.

This document defines implementation and acceptance contracts. Sections marked **Implemented** describe the current runtime. Sections marked **Planned** describe the game integration; they must not be read as claims about existing code.

## Product scope

JSE supplies fixed updates, scene lifecycle, physical-to-logical keyboard input, immutable geometry, preloaded images and interchangeable Java2D rendering. Its first example is a movable rectangle preview. The target Arena example combines themed entities, interchangeable enemy behavior, event-driven HUD/logging and composable graphics.

The runtime must build and run before the game modules are available. Game-specific health, score, themes, AI and events remain outside core. Public project text is English.

The target release includes two renderer styles, two coherent entity families, three active enemy behaviors, two independently removable observers, a playable Arena and a rendering showcase. It excludes 3D, OpenGL, rigid-body simulation, networking, a level editor, ECS, plugins, scripting, saves and audio.

## Build and package — Implemented

| Component | Project setting |
| --- | --- |
| Language / bytecode | Java 17, UTF-8 |
| Maven Wrapper | Maven 3.9.9; wrapper scripts 3.3.2 |
| Compiler / Surefire / Shade | 3.13.0 / 3.2.5 / 3.5.3 |
| Tests | JUnit Jupiter 5.10.2, test scope only |
| Coordinates | `dev.jse:jse-demo:0.1.0` |
| Entry point | `jse.demo.DemoApplication` |
| Output | `target/jse-demo.jar` |
| Production dependencies | JDK only: Swing, Java2D, ImageIO |

`./mvnw clean verify` compiles, tests and packages the executable JAR. `mvnw.cmd` supplies the Windows equivalent. The packaged JAR includes main resources, excludes tests and needs no network at runtime. A desktop display is required for the window; `--help` and automated tests can run headlessly.

Build dependency versions are project choices for reproducibility. Java 17 is the supported baseline; other operating systems and newer JDKs are considered verified only after an actual check.

## Ownership and milestones

| Owner | Pattern responsibilities | Module responsibilities |
| --- | --- | --- |
| Sabirzhanov Emil | Bridge, Decorator | Math, core lifecycle, timing, platform, input, renderers, assets, preview/showcase |
| Baktiyarova Aruzhan | Factory Method, Abstract Factory | Entity categories and products, themed factories, spawners, requests, initial arena layout |
| Roziyeva Yasmina | Strategy, Observer | World/snapshots, AI, events, motion/collision, Arena rules, HUD/log and game scenes |

The shared runtime contracts are already available. Aruzhan and Yasmina coordinate GameObject, WorldView and BehaviorStrategy before implementing their dependent modules. Neither needs a temporary renderer or engine implementation.

The team tracks five milestones: foundation, integration, feature completion, release candidate and release. [Development status](Development-Status.md) maps these milestones to the team's remaining work. [Implementation plan](Implementation-Plan.md) records the executable steps.

## Package boundaries

| Package | Role | State |
| --- | --- | --- |
| `jse.math` | Vec2, Rect, Rgba | Implemented |
| `jse.input` | GameAction, InputState, InputSource | Implemented |
| `jse.core` | EngineConfig, EngineHost, EngineControl, EngineServices, Scene, SceneManager, JseEngine | Implemented |
| `jse.platform.awt` | SwingHost, GamePanel, SwingInputSource, KeyBindings, HostSettings | Implemented |
| `jse.render` | Renderer, Graphic, RectangleGraphic, SpriteGraphic, HealthView, RenderMode | Implemented; decorators planned |
| `jse.render.awt` | FrameRenderer, RenderStyle, FilledRenderer, WireframeRenderer | Implemented |
| `jse.assets` | SpriteId, AssetManager, MissingSpriteStyle | Implemented |
| `jse.demo` | LaunchSettings, PreviewSettings, PreviewScene, DemoApplication | Implemented; game scenes planned |
| `jse.world` | GameObject, Player, Enemy, Pickup, World and snapshots | Planned |
| `jse.factory` | SpawnRequest, category spawners and family factories | Planned |
| `jse.theme.forest`, `jse.theme.space` | Six concrete products | Planned |
| `jse.ai` | BehaviorStrategy, Idle/Patrol/Chase/Flee and AiSystem | Planned |
| `jse.event` | EventBus, typed events, listener and subscription | Planned |
| `jse.physics` | MotionSystem and CollisionSystem | Planned |

Core depends on InputSource, Renderer and EngineHost, never on World or EventBus. The desktop host implements core callbacks and prepares frames. Renderers depend on assets; graphics depend on Renderer. Game scenes compose factories, World, systems, rules and observers. No global engine or mutable service locator is used.

## Geometry and input — Implemented

`Vec2(double x, double y)` requires finite components. It supports add/subtract/scale, length and normalization. ZERO normalizes to ZERO. Normalizing extreme finite components avoids overflow by scaling before measuring length.

`Rect(double x, double y, double width, double height)` requires finite endpoints and positive dimensions. `overlaps` means positive shared area; edge touching is false. `contains(Vec2)` includes the edge. `clampPosition(position, size)` constrains a top-left position for a positive-size object that fits the rectangle.

`Rgba(red, green, blue, alpha)` validates channels from 0 to 255. `parse` accepts #RRGGBB and #RRGGBBAA. Math and core contain no AWT geometry or color types.

`InputState(Set<GameAction> held, Set<GameAction> pressed)` copies both sets. `InputSource.snapshotAndConsumePressed()` preserves held actions and consumes pending presses once. `clear()` removes both. The engine requests a new snapshot for every fixed update.

SwingInputSource tracks physical keys separately. W and Up may both map to MOVE_UP: releasing W keeps the action held while Up remains down. A second physical alias does not repeat a logical press already held. OS repeat is ignored; a short press/release is retained until the next snapshot.

KeyBindings maps key codes to actions. Binding uses WHEN_IN_FOCUSED_WINDOW; unbinding restores previous local mappings, removes installed actions, restores focus traversal and releases the component reference. Window deactivation clears input and calls the scene's focus-loss handler.

## Core lifecycle — Implemented

The concrete Java signatures are the source of truth for the current milestone:

```java
public record EngineConfig(int width, int height, int targetUps,
                           int maxUpdatesPerPump, double maxFrameDeltaSeconds,
                           int timerDelayMillis) {}
public record EngineServices(EngineConfig config, Renderer initialRenderer,
                             EngineControl control) {}
public interface EngineControl {
    void requestScene(Scene scene);
    void setRenderMode(RenderMode mode);
    void stop();
}
public interface Scene {
    void onEnter(EngineServices services);
    void update(double dt, InputState input);
    void render(Renderer renderer);
    void onFocusLost();
    void onExit();
}
```

The snippets summarize separate public types; they are not a single compilable source file. Values reject null or invalid input rather than substituting defaults.

EngineHost registers pump, paint, focus-loss, close and failure callbacks. `open` prepares the window without invoking those callbacks; `startTimer` shows it and starts updates. `currentRenderer`, `setRenderMode`, `requestRepaint`, `showError` and `close` complete the host boundary.

JseEngine receives EngineConfig, EngineHost and InputSource explicitly. It starts once and confines operations to the starting thread, EDT in the application. Subsequent starts fail. stop is idempotent and closes the host even if scene cleanup fails. Cleanup errors are retained, not silently discarded.

SceneManager keeps one current scene and one pending request. The last request before the transition wins. Transition applies at an update boundary, never while painting. The old scene exits once, the new one enters once. Failure during entry still calls its exit handler. If shutdown occurs during the outgoing scene's exit, replacement entry is cancelled.

After a transition, input is cleared and the accumulator reset; remaining updates from the current batch are cancelled. Scene-specific subscriptions and resources are owned and closed by that scene. EngineServices intentionally contains no EventBus.

## Fixed timing — Implemented

The timer is a pump source, not a variable-delta simulation. Default target updates are 60 per second, so dt is 1/60. The first timestamp initializes the clock without an update. Backward timestamps are rejected.

Elapsed time is clamped to the configured maximum, default 0.25 seconds. Each pump performs at most five updates by default. Excess whole steps are dropped, leaving a remainder below dt; a one-ULP correction handles floating point tick boundaries. EngineStats exposes completed updates, dropped seconds and pending seconds.

A stop request inside update prevents further updates or repaint in that batch. Update, entry or paint failure stops simulation, clears input and exits the active scene once. The desktop host displays an English error panel and keeps its window closable. A paint failure is reported only after endFrame and graphics disposal.

## Bridge rendering and assets — Implemented

```java
public interface Renderer {
    void drawSprite(SpriteId sprite, Rect bounds);
    void drawRectangle(Rect bounds, Rgba color);
    void drawOutline(Rect bounds, Rgba color);
    void drawText(String text, Vec2 position, Rgba color);
}
public abstract class Graphic {
    protected Renderer renderer;
    public void setRenderer(Renderer renderer);
    public abstract void draw(Rect bounds);
}
```

Graphic is the Bridge abstraction; RectangleGraphic and SpriteGraphic are refined abstractions. Renderer is the implementor; FilledRenderer and WireframeRenderer are concrete implementors. Adding a graphic type and adding a renderer style are independent changes.

Filled paints colored rectangles and scaled images. Wireframe outlines rectangles and draws sprite bounds, diagonals and resource labels. Both draw real Java2D output; a recording test renderer is not counted as a second production implementation. Both styles use one graphics backend.

FrameRenderer adds beginFrame(Graphics2D, width, height) and endFrame. Draw operations outside a frame and reentrant beginFrame fail. endFrame is idempotent. GamePanel copies its incoming graphics context and releases the copy after the frame. No graphics context survives a frame. Text position is a baseline; font, stroke, wireframe color and antialiasing come from RenderStyle.

SpriteId is a relative classpath path, not a theme enum. Absolute paths, empty path segments and traversal are rejected. Example future paths are `sprites/forest/player.png` and `sprites/space/player.png`.

AssetManager is composed with a ClassLoader and MissingSpriteStyle. preload reads each ID at most once, closes streams and caches the decoded image or fallback. get performs no resource reads or logging; it returns a cached image or a configured placeholder. Looking up an unprepared ID does not prevent subsequent preload. Missing resources are diagnosed during preload, once per ID. Images returned by the cache are read-only by contract.

## Runtime preview and configuration — Implemented

The preview scene moves a configurable rectangle inside a viewport-derived area. Initial/reset position is centered; motion is normalized before applying speed × dt, then clamped to the area. Opposing directions cancel. Pause freezes movement but leaves commands available. Focus loss pauses without automatic resume. Reset centers and resumes.

Bundled defaults are in `src/main/resources/jse.properties`. `--config FILE` overlays UTF-8 properties. `--help` prints usage without opening a window. Startup rejects unknown keys, invalid typed values, duplicate physical assignments and layouts that cannot fit the object. Help labels in the preview reflect configured bindings.

| Settings | Meaning |
| --- | --- |
| `window.*` | Title, content dimensions, background |
| `engine.*` | Fixed update rate, catch-up budget, maximum frame delta, timer delay |
| `render.*` | Initial style, font, stroke, wireframe color, antialiasing |
| `assets.*` | Placeholder style and comma-separated preload paths |
| `preview.*` | Object dimensions, speed, padding, header/line spacing and colors |
| `input.*` | Comma-separated physical key aliases per logical action |

Default controls are WASD/arrows, P pause, R reset, F1 style and Escape close. PreviewScene is an independent example; it does not stand in for the planned factories, game entities or AI.

## Decorator — Planned; Emil

GraphicDecorator extends Graphic and wraps a Graphic. draw first delegates, then adds its own layer. setRenderer updates itself and propagates to the wrapped graphic. OutlineDecorator adds a configurable border. HealthBarDecorator reads HealthView and draws the clamped current/max ratio; maxHealth must be positive.

Decorator styling belongs in an immutable settings value: proposed defaults are a 2-pixel outline, bar offset 6, height 4, green fill and dark background. Bar position is visual only and does not modify collision bounds.

GameObject retains immutable baseGraphic and replaceable graphic. Toggle handlers rebuild from baseGraphic in a fixed order, Outline then HealthBar, rather than repeatedly wrapping the existing decorated graphic. Health applies only to Player. Repeated F2/F3 toggles must not grow the chain. Renderer replacement must reach every wrapped component.

The showcase must exercise SpriteGraphic and RectangleGraphic under both renderers, each alone and with combined decorators, and display a partial health value such as 2/3.

## Entity model — Planned; Aruzhan

ObjectKind is PLAYER, ENEMY or PICKUP. ThemeId is FOREST or SPACE. SpawnRequest contains position and positive size. GameObject stores category, family, position, size, velocity, assigned ID, active/removal state, immutable baseGraphic and current graphic. Its bounds derive from position and size. No entity draws directly through Graphics2D.

Player is an abstract category implementing HealthView; Enemy is an abstract category holding a BehaviorStrategy; Pickup is an abstract category holding value. ForestPlayer/Enemy/Pickup and SpacePlayer/Enemy/Pickup are the six concrete products. Each fixes its family identity and takes a SpawnRequest, Graphic and validated category settings.

Before registration an object has ID 0 and active=false. World alone assigns a positive ID once and controls active/removal state. Assigning an ID twice or attaching an already registered object is invalid. Health cannot fall below zero; positive damage only. Enemy starts with IdleBehavior until scene setup assigns its intended strategy.

Game values must be injected through category settings; do not hardcode speed, health or pickup values in six product constructors. Target defaults: player size 24×24, speed 220, health 3; enemy size 24×24, speed 110; pickup size 16×16, value 1.

## Factory Method and Abstract Factory — Planned; Aruzhan

GameObjectFamilyFactory defines familyId and createPlayer/createEnemy/createPickup. ForestFactory and SpaceFactory produce three related products with corresponding SpriteGraphic resource IDs. They receive Renderer and validated entity settings. All category spawners in one ArenaScene share the same selected family factory.

EntitySpawner<T extends GameObject> is the creator. Its final spawn operation validates the request, invokes protected abstract createObject, checks returned category/family and queues it in World. PlayerSpawner, EnemySpawner and PickupSpawner override createObject with the corresponding family-factory operation.

Factory Method selects a product category through creator subclasses. Abstract Factory selects a coherent family across categories. A switch inside one helper is not the intended Factory Method structure. Factories create products; World owns IDs and registration; scenes own spawning policy and category limits.

Acceptance requires all three creators and both concrete families, validation of mismatched products, a coherent six-product matrix and identical game rules after a theme switch.

## World and snapshots — Planned; Yasmina

World is constructed with a positive runId, arena, family identity, injected capacity and scene-owned EventBus. enqueueAdd validates family, bounds, size, capacity and unregistered identity, assigns the next ID and reserves space. IDs increase monotonically per World and are never reused.

Adds are queued. Until commit an object is absent from view/findActive/forEachActive and remains inactive. spawnView includes queued additions to reserve occupied space. enqueueRemove immediately marks an object unavailable to active lookup and iteration, while structural removal is deferred.

commitChanges(tick) applies removals first, then additions in ID order. Committed changes publish one EntityRemoved or EntitySpawned. Removing a queued addition cancels it without either lifecycle event, but consumes its assigned ID. Repeated removal or an unknown ID is a no-op. Structural requests must not mutate the collection currently being iterated.

WorldView contains an immutable List<EntityView>, Optional<Vec2> playerPosition and arena. EntityView contains ID, category, family, position and bounds, with no mutable GameObject or Graphic reference. Both active view and spawn view exclude pending removals. A saved snapshot remains unchanged after the world mutates.

Default World capacity is 128 including queued additions. Arena policy separately allows one Player, at most 16 Enemies and at most 16 Pickups. Capacity and category limits are injected settings, not runtime constants.

## Strategy and physics — Planned; Yasmina

BehaviorStrategy defines `Vec2 desiredVelocity(Enemy enemy, WorldView world, double dt)`. It returns intent; it does not mutate World or position. dt must be positive and finite. AiSystem applies strategy results to velocities; MotionSystem alone integrates positions and clamps them to arena bounds.

Idle returns ZERO. Patrol holds two waypoints and its current target. Near a target it returns direction × min(speed, distance/dt) so it does not overshoot, then changes target for the following update. Chase follows the player and returns ZERO without a target or at zero distance. Flee moves away within a configurable radius, default 180; outside it returns ZERO. Exact player/enemy overlap chooses a stable rightward direction.

Behavior replacement changes neither entity ID nor position nor family. Publish BehaviorChanged only when an actual replacement occurs. Deterministic tests cover each behavior, no target, zero distance, waypoint arrival and clamping.

CollisionSystem reads WorldView and returns distinct positive ID pairs with min/max ordering, sorted by firstId then secondId. Positive area overlap is required. Physics owns detection; ArenaRules owns consequences.

## Observer and events — Planned; Yasmina

GameEvent exposes runId and tick. EventBus supports typed subscribe, publish, dispatchPending and close. subscribe accepts `Class<E>` and `GameEventListener<? super E>` and returns idempotent AutoCloseable Subscription. Routing uses the exact event class. Duplicate subscriptions for the same class and listener identity fail.

At dispatch start, snapshot the queued events and listener lists for all types in that batch. Events published inside callbacks and subscriptions created inside callbacks take effect only in a later dispatch. Before each callback, check that the subscription is still open. Recursive dispatch is prohibited. Default queued capacity is 1024; overflow fails visibly. Close clears queued events/subscriptions and rejects further use.

| Immutable event | Fields in addition to runId/tick |
| --- | --- |
| EntitySpawned | entityId, category, familyId |
| EntityRemoved | entityId, removal reason |
| ItemCollected | playerId, pickupId, newScore |
| HealthChanged | playerId, oldHealth, newHealth |
| GameFinished | WON/LOST result, finalScore |
| BehaviorChanged | enemyId, behaviorName |

runId and entity IDs are positive; tick is nonnegative. RemovalReason includes COLLECTED, SCENE_CLEANUP and DIAGNOSTIC.

HudObserver subscribes separately to ItemCollected, HealthChanged and GameFinished. It initializes score 0, health from Player and empty result. EventLogObserver subscribes to all six exact classes and stores the latest 50 chronological lines by default. The observers implement GameEventListener<GameEvent> but do not depend on implicit base-class delivery.

Both retain and close their subscriptions. Disabling the log closes its observer; reenabling creates an empty observer and does not replay missed events. Rules never read HUD state. Pause/selected AI may be displayed directly from read-only scene state.

## Arena example — Planned

ArenaScene owns a fresh EventBus, World, observers and rules for each run. Shared preferences retain theme, renderer, decoration toggles and log enabled state; they are explicitly passed, not global. Run IDs come from a composition-root LongSupplier and start at 1.

For the reference 960×540 viewport the arena is (24, 64, 912, 452). This is the game's layout profile, separate from the current preview header. Store the profile and object layout in game resources/typed settings; a resized viewport must use a compatible validated profile or derived coordinates.

| Initial object | Top-left position | Behavior |
| --- | --- | --- |
| Player | (460, 260) | Input |
| Enemy | (120, 130) | Patrol between (120, 130) and (300, 130) |
| Enemy | (760, 140) | Chase |
| Enemy | (720, 410) | Flee, radius 180 |
| Pickup ×8 | (80,90), (260,90), (450,90), (650,90), (850,90), (100,450), (430,450), (820,450) | None |

The layout queues 12 objects, commits tick 0 and dispatches before first paint. It is owned by Aruzhan. Diagnostic spawning finds the first free grid cell, row-major, starting (48,88) with spacing 48 in the reference profile. Search respects bounds and active plus queued objects. If no free cell or category slot remains, show a clear English status and skip spawning.

An active update performs this order:

1. Resolve commands and early scene transition requests.
2. Set Player velocity from normalized input.
3. Build the immutable AI snapshot and update enemy velocities.
4. Integrate/clamp motion, then snapshot and find overlaps.
5. Apply pickup interactions, then enemy contact damage.
6. Commit changes and evaluate outcome.
7. Dispatch queued events once.

Each pickup contributes 1 toward a target score of 8 and can be collected only once, even if duplicate pairs reach the rules. Contact damage is 1 with an active-time cooldown of 0.75 seconds; multiple simultaneous enemies do not bypass it. Cooldown decreases before processing contacts. Paused time does not affect motion, cooldown or score.

Outcome is evaluated after commit: health zero takes priority over collecting the last pickup in the same update. Publish GameFinished once per run. WON/LOST freezes simulation but preserves reset, menu, theme and visual controls. Rules derive health/score from their own state and entities, never from observers. Missing the sole Player during outcome evaluation is an invariant failure.

Ticks increment for each scene update, including pause; timing-sensitive rules receive dt only during active play. Event dispatch continues when commands produce events during pause.

## Scenes and controls — Planned

TitleScene offers Enter for Arena and H for Showcase. Escape returns from Arena/Showcase to Title. Current preview uses Escape to close; game scenes will interpret BACK_TO_TITLE separately from QUIT.

| Action | Default game key | Behavior |
| --- | --- | --- |
| Movement | WASD / arrows | Normalized Player movement |
| Pause / reset | P / R | Toggle pause; new run of current theme |
| Theme | T | New run with alternate family |
| Renderer | F1 | Change style without resetting model |
| Outline / health | F2 / F3 | Rebuild decorator chain from base graphics |
| Enemy selection | Tab | Cycle active enemies in ID order |
| Behavior | B | Cycle Patrol → Chase → Flee for selected enemy |
| Diagnostic spawn | N / M | Spawn Enemy / Pickup at first free location |
| Event log | L | Close or create log observer |
| Menu / showcase | Escape / H | Return to Title / open Showcase from Title |

Changing theme starts a new World, health and score, preserving renderer/decorator/log preferences. Reset does the same within the current theme. Log history and selected enemy reset per run. Command transition precedence is Escape → T → R → Enter/H; after selecting one, skip remaining commands that update. API callers still use the core's last-request-wins rule.

Paused scenes keep painting; renderer/decorator/log toggles and behavior selection remain available. Losing focus clears input and pauses; returning focus does not resume. On exit, close observers and EventBus, discard World and references, and clear input through core transition handling. Repeat reset/theme/menu cycles must leave no duplicate events or subscriptions.

Showcase reuses core and graphics without World or ArenaRules. It displays basic and decorated rectangles/sprites, Filled/Wireframe switching and a partial Player health bar. It is Emil's next example milestone.

## Error handling and configuration boundaries

Invalid configuration fails before window startup with exit status 2. Missing/unsupported images produce a configured fallback and preload warning. Unexpected scene failures stop simulation and show the host error panel. Capacity/invariant failures are not silently swallowed. Lack of a free diagnostic spawn position is an expected condition, not a runtime crash.

Configuration is loaded once. Engine settings configure timing and content size; RenderStyle configures visuals; future entity/arena settings configure game rules. Runtime code must not embed Forest/Space decisions or game score constants. Asset cache images are immutable to consumers, and mutable runtime/game services remain thread-confined.

## SOLID decisions

- Single responsibility: core schedules; host handles the desktop; renderers paint; World stores; systems move/detect; rules decide consequences; observers display.
- Open/closed: a new Graphic, renderer, family or strategy uses its corresponding interface. Composition may select the new implementation explicitly.
- Liskov substitution: every renderer obeys frame rules; products preserve category/family invariants; strategies produce finite velocities.
- Interface segregation: InputSource, HealthView and WorldView expose only the data their clients need.
- Dependency inversion: core uses host/input/render contracts; scenes receive services and compose game components; rules publish events without naming HUD/log implementations.

## Acceptance criteria

| Area | Required evidence |
| --- | --- |
| Build | Fresh checkout on Java 17, clean verify, executable resource-containing JAR |
| Runtime | Bounded catch-up, single-use pressed actions, thread confinement, transition and shutdown cleanup |
| Input | Aliases, autorepeat, quick taps, focus clear and binding removal |
| Bridge | Two actual image outputs; renderer replacement preserves scene position |
| Assets | No reads/logging in get; one preload per ID; fallback and later preload behavior |
| Decorator | Standalone and combined layers, propagation, repeated toggle without chain growth |
| Factories | Three creator subclasses × two families; mismatch rejection and queued registration |
| World | Stable IDs, queued modifications, cancelled add, immutable views and capacity |
| Strategy | Three behaviors, runtime swap, no target, arrival and boundary handling |
| Observer | Exact typed routing, independent unsubscribe, deferred callbacks, duplicate prevention and close |
| Rules | Single collection, cooldown, loss priority, one outcome event, pause behavior |
| Lifecycle | At least 20 reset/theme/menu cycles without observer leakage |
| Example | Playable Arena, menu, outcomes and independent Showcase |
| Documentation | English docs and UI; implemented/planned distinction; UML and images match the stated version |

Current foundation verification is recorded in [Verification](Verification.md). Planned acceptance checks are not marked passed before their modules exist.

## Release artifacts and walkthrough

A release includes source, wrapper/build configuration, resources with attribution, tests, README, specification, architecture/pattern/sequence diagrams, screenshots, executable JAR and a concise technical report. A source archive excludes .git, IDE settings, caches and generated temporary files; extracting it must reproduce the documented build.

The technical report covers introduction/problem, scope, architecture, six pattern roles and implementation excerpts, interactions, SOLID, tests/results, contributor responsibilities, limitations/future work and references. Each implemented pattern must be explained using its real classes, UML and an observable example action. Source materials and external requirements are kept separately from public product descriptions.

The target walkthrough opens the packaged application, starts Arena, moves/pauses, changes theme, swaps renderer, combines decorators, changes selected enemy behavior, disables/reenables the log, reaches an outcome, restarts and opens Showcase. Explain the object and event flow alongside the visible actions. Maintain local source, diagrams and release files for reproducibility.

## Shared game API — Planned

These signatures define the first integration contract. Add each public type in its own file. Settings are supplied by the game composition root; their defaults come from a game resource profile. Do not modify the current runtime configuration to embed game rules.

```java
// jse.factory
public record SpawnRequest(Vec2 position, Vec2 size) {}
public record PlayerSettings(Vec2 size, double speed, int maxHealth) {}
public record EnemySettings(Vec2 size, double speed) {}
public record PickupSettings(Vec2 size, int value) {}
public record EntitySettings(PlayerSettings player, EnemySettings enemy,
                             PickupSettings pickup) {}
public record FamilySprites(SpriteId player, SpriteId enemy, SpriteId pickup) {}
public interface GameObjectFamilyFactory {
    ThemeId familyId();
    Player createPlayer(SpawnRequest request);
    Enemy createEnemy(SpawnRequest request);
    Pickup createPickup(SpawnRequest request);
}
public abstract class EntitySpawner<T extends GameObject> {
    protected EntitySpawner(GameObjectFamilyFactory factory, ObjectKind expectedKind);
    public final T spawn(World world, SpawnRequest request);
    protected abstract T createObject(SpawnRequest request);
}
```

PlayerSpawner, EnemySpawner and PickupSpawner each accept GameObjectFamilyFactory. ForestFactory and SpaceFactory each accept `(Renderer renderer, EntitySettings settings, FamilySprites sprites)`. They validate requested sizes against category settings. Product constructors accept `(SpawnRequest request, Graphic graphic, CategorySettings settings)` with their corresponding category settings type. Identity assignment is exclusively World's responsibility.

```java
// jse.world
public abstract class GameObject {
    protected GameObject(ObjectKind category, ThemeId familyId,
                         SpawnRequest request, Graphic baseGraphic);
    public long id();
    public ObjectKind category();
    public ThemeId familyId();
    public Vec2 position();
    public Vec2 size();
    public Vec2 velocity();
    public Rect bounds();
    public boolean active();
    public boolean pendingRemoval();
    public Graphic baseGraphic();
    public Graphic graphic();
    public void setPosition(Vec2 position);
    public void setVelocity(Vec2 velocity);
    public void setGraphic(Graphic graphic);
}
public abstract class Player extends GameObject implements HealthView {
    protected Player(ThemeId familyId, SpawnRequest request,
                     Graphic graphic, PlayerSettings settings);
    public double speed();
    public int currentHealth();
    public int maxHealth();
    public void takeDamage(int amount);
}
public abstract class Enemy extends GameObject {
    protected Enemy(ThemeId familyId, SpawnRequest request,
                    Graphic graphic, EnemySettings settings);
    public double speed();
    public BehaviorStrategy behavior();
    public void setBehavior(BehaviorStrategy behavior);
}
public abstract class Pickup extends GameObject {
    protected Pickup(ThemeId familyId, SpawnRequest request,
                     Graphic graphic, PickupSettings settings);
    public int value();
}
public record EntityView(long id, ObjectKind category, ThemeId familyId,
                         Vec2 position, Rect bounds) {}
public record WorldView(List<EntityView> entities,
                        Optional<Vec2> playerPosition, Rect arena) {}
public final class World {
    public World(long runId, Rect arena, ThemeId familyId,
                 int capacity, EventBus events);
    public long runId();
    public Rect arena();
    public ThemeId familyId();
    public int sizeIncludingPending();
    public void enqueueAdd(GameObject object);
    public void enqueueRemove(long entityId, RemovalReason reason);
    public void commitChanges(long tick);
    public WorldView view();
    public WorldView spawnView();
    public void forEachActive(Consumer<GameObject> action);
    public Optional<GameObject> findActive(long id);
}
```

GameObject's `assignId`, `setActive` and `markPendingRemoval` are package-private methods in jse.world, not public mutation entry points. World alone calls them. SpawnRequest/category sizes must have positive components. World uses bounds endpoints to validate complete containment; Rect.contains takes a Vec2 in the current runtime.

```java
// jse.ai
public interface BehaviorStrategy {
    Vec2 desiredVelocity(Enemy enemy, WorldView world, double dt);
}
public final class AiSystem {
    public void update(World world, WorldView view, double dt);
}
// jse.physics
public record CollisionPair(long firstId, long secondId) {}
public final class MotionSystem {
    public void update(World world, double dt);
}
public final class CollisionSystem {
    public List<CollisionPair> findOverlaps(WorldView world);
}
// jse.event
public interface GameEvent { long runId(); long tick(); }
@FunctionalInterface
public interface GameEventListener<E extends GameEvent> { void onEvent(E event); }
public interface Subscription extends AutoCloseable { void close(); }
public final class EventBus implements AutoCloseable {
    public EventBus(int queueCapacity);
    public <E extends GameEvent> Subscription subscribe(
            Class<E> type, GameEventListener<? super E> listener);
    public void publish(GameEvent event);
    public void dispatchPending();
    public void close();
}
```

PatrolBehavior accepts two Vec2 waypoints; FleeBehavior accepts a positive finite radius. IdleBehavior and ChaseBehavior need no configuration. Each enemy gets its own stateful PatrolBehavior, never a shared patrol cursor.

HudObserver accepts `(EventBus events, Player player)`, exposes score/health/optional result and closes subscriptions. EventLogObserver accepts `(EventBus events, int capacity)`, exposes an immutable chronological list of lines and closes subscriptions. Both implement GameEventListener<GameEvent> and AutoCloseable.

ArenaRules receives EventBus and immutable ArenaSettings containing target score, damage cooldown and contact damage. Arena composition also receives category limits, World/event/log capacities, layout and preferences. `applyCollisions(World, List<CollisionPair>, double dt, long tick)` and `evaluateOutcome(World, long tick)` follow the ordering defined above. Rules expose read-only score/status for UI; begin/pause/focus-loss operations update their own state.

Before merging the first game model, Aruzhan and Yasmina validate these signatures together and update consumers in the same change if a necessary adjustment emerges. Runtime interfaces are already implemented and should remain compatible.
