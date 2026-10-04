# Runtime Foundation

The first implementation milestone delivers an executable 2D runtime that can be used before the game model, factories and AI are available. It belongs to Emil's platform and rendering work. The preview scene demonstrates the runtime; it does not simulate missing game modules.

## Boundaries

- `core` owns fixed updates, scene transitions and lifecycle. It depends on `InputSource`, `Renderer` and `EngineHost`, never on game objects or events.
- `platform.awt` implements the desktop host, physical keyboard bindings and painting. It is the only window-dependent module.
- `render` defines Bridge abstractions. `render.awt` implements drawing; every Graphics2D reference expires at endFrame.
- `assets` loads images before simulation. SpriteId is a relative classpath path, independent of a game's product family.
- `demo` composes the runtime and an interactive preview. Configuration is read once from UTF-8 properties, with optional external overrides.

No global engine, world, event bus or mutable service registry is introduced. A scene receives `EngineServices(config, initialRenderer, control)` on entry and owns any game-specific resources it creates.

## Contracts

`EngineConfig(width, height, targetUps, maxUpdatesPerPump, maxFrameDeltaSeconds, timerDelayMillis)` validates positive sizes and timing. Width and height are content dimensions. Timer cadence is a host setting; fixed dt is always `1 / targetUps`.

`EngineHost.open(config, pump, paint, focusLost, closeRequested, failed)` registers callbacks. Paint receives a prepared Renderer. The failure callback is called after the frame has ended. `currentRenderer`, `setRenderMode`, `startTimer`, `requestRepaint`, `showError` and `close` complete the host contract.

`Scene` defines onEnter, update(dt, input), render(renderer), onFocusLost and onExit. `SceneManager` performs the last queued transition after an update, never during iteration or paint. Enter and exit happen once per activation. Partial entry failure still closes the attempted scene. Replacement activation is cancelled if shutdown occurs during the outgoing exit handler.

`JseEngine` starts once and confines subsequent operations to the starting thread. The Swing application starts it on EDT. pump accepts explicit nanoseconds, allowing deterministic tests. stop is idempotent; update or paint failure stops simulation, releases the active scene and leaves an error panel visible until the window is closed.

## Timing

The first pump establishes a timestamp without updating. A negative elapsed interval is rejected. Elapsed time is clamped to maxFrameDeltaSeconds; the excess is recorded. At most maxUpdatesPerPump steps execute. Whole remaining steps are discarded, keeping a remainder below dt. Floating point tick boundaries use a one-ULP correction, not a millisecond approximation.

Every fixed step gets a fresh input snapshot: held actions remain active, pressed actions are consumed once. Commands remain available while the preview is paused. A scene transition cancels the rest of the current batch, clears pending input and resets the accumulator.

## Input and rendering

Physical keys are tracked individually. Two keys may hold the same logical action; releasing either one preserves the action while the other remains down. OS key repeat creates no new logical press. Window deactivation clears held and pending input, and pauses the preview. Reactivation does not resume automatically.

Key bindings use WHEN_IN_FOCUSED_WINDOW and support removal without retaining the host component. Keyboard focus traversal is disabled for the game panel.

Filled and Wireframe both draw real Java2D frames. SpriteGraphic and RectangleGraphic delegate to either implementation. A renderer switch does not change scene position or timing. AssetManager.preload loads/cache-resolves resources before startup; get never performs resource IO or logging during painting; missing-resource warnings belong to preload. Missing or unprepared sprites use a configured placeholder.

## Preview and configuration

The preview moves one rectangle inside a viewport-derived area. Movement uses normalized input and explicit dt. Object size, speed, padding, header height, colors, fonts, input aliases and timing settings come from `jse.properties`. P pauses, R resets, F1 changes renderer and Escape closes the preview by default. Help labels are generated from the actual key bindings.

Decorator, product factories, World, AI and Arena rules remain later milestones. The preview is a useful independent client of the engine, not a temporary implementation of those features.

## Verification

Tests cover finite geometry and extreme-vector normalization; immutable input; duplicate physical keys; queued commands; image output from both renderers; resource preload/cache behavior; fixed-step limits and time boundaries; scene entry/exit and transition cleanup; failure recovery; and preview motion/pause/reset.

Swing bindings and GamePanel are tested on EDT with buffered images, without a display. A separate display-enabled smoke run checks the assembled window. A successful headless test run alone does not claim native window verification.
