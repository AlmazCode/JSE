# JSE Implementation Plan

**Goal:** Deliver an independent 2D runtime, then integrate themed game objects, AI and an event-driven arena example.

**Architecture:** Core lifecycle and rendering build without game modules. Scenes own game state and events. Typed startup configuration supplies example and platform defaults.

**Tech stack:** Java 17, Maven Wrapper 3.9.9, Swing, Java2D, JUnit 5.10.2. Production code uses the JDK only.

**Specification:** [Technical specification](JSE-Technical-Specification.md), [runtime design](design/Runtime-Foundation.md).

## Constraints

Public text, documentation, comments and messages are English. Keep game values in configuration or game modules. No mutable globals, no Graphics2D outside a frame, no IO in paint, no sleeps in logic tests. Git records actual contributors; task ownership identifies responsibility.

## Runtime milestone

This milestone covers Emil's preparation and first implementation work. Steps are tracked below; game features are not required to build the runtime.

### Task 1: Geometry and input contracts

**Owner:** Sabirzhanov Emil.

**Files:** `src/main/java/jse/math/{Vec2,Rect,Rgba}.java`, `input/{GameAction,InputState,InputSource}.java`, `render/{Renderer,Graphic,HealthView,RenderMode}.java`, `assets/SpriteId.java`.

**Interfaces:** produce finite immutable geometry, immutable InputState(held, pressed), InputSource.snapshotAndConsumePressed/clear and Renderer drawing operations. Consumers are renderers, the host, preview and future game modules.

- [x] Write GeometryTest/InputStateTest covering ZERO normalization, Double.MAX_VALUE normalization, NaN/Infinity rejection, endpoint overflow, edge touching, bounds clamping and defensive copies.
- [x] Run `./mvnw -Dtest=GeometryTest,InputStateTest test`; confirm missing behavior fails.
- [x] Implement the value objects and contracts exactly as the runtime design specifies.
- [x] Repeat the command and the full `./mvnw test`; expect zero failures.
- [x] Include the foundation in the runtime milestone commit.

### Task 2: Bridge rendering and assets

**Owner:** Sabirzhanov Emil. **Depends on:** task 1.

**Files:** `render/{RectangleGraphic,SpriteGraphic}.java`, `render/awt/{FrameRenderer,RenderStyle,AbstractFrameRenderer,FilledRenderer,WireframeRenderer}.java`, `assets/{AssetManager,MissingSpriteStyle}.java`.

**Interfaces:** consume Renderer/SpriteId/Rect/Rgba. Produce Graphic.draw/setRenderer, beginFrame/endFrame, preload(Collection<SpriteId>)/get(SpriteId). Images are preloaded; get performs no IO.

- [x] Write RendererImageTest/AssetManagerTest: delegation follows replacement; Filled paints a rectangle center while Wireframe leaves it untouched; both render sprite bounds; drawing outside a frame fails; repeated preload reads a resource once; get before preload does not open it; missing assets return a stable placeholder.
- [x] Run those tests and confirm the initial failure.
- [x] Implement real styles, explicit frame ownership, classpath loading and configured placeholders.
- [x] Repeat tests and full suite; expect zero failures.
- [x] Include rendering in the runtime milestone commit.

### Task 3: Fixed updates and scene lifecycle

**Owner:** Sabirzhanov Emil. **Depends on:** tasks 1–2.

**Files:** `core/{EngineConfig,EngineControl,EngineHost,EngineServices,Scene,SceneManager,FixedStepClock,EngineStats,JseEngine}.java`.

**Interfaces:** consume InputSource and EngineHost callbacks; produce start/pump/render/requestScene/setRenderMode/stop, current Scene lifecycle and runtime statistics. EngineServices has no EventBus.

- [x] Write RuntimeTest: first pump gives zero updates; 1-second gap is clamped and executes at most five default updates; remainder stays below dt; presses appear once; last scene request wins; transitions cancel catch-up and input; failed entry exits once; failure preserves a visible error state; stop closes once even after callback failure.
- [x] Run targeted tests and confirm missing behavior fails before implementation.
- [x] Implement the clock and lifecycle with explicit constructor dependencies and thread confinement.
- [x] Repeat targeted and full tests; expect zero failures.
- [x] Include core lifecycle in the runtime milestone commit.

### Task 4: Swing host and keyboard

**Owner:** Sabirzhanov Emil. **Depends on:** tasks 1–3.

**Files:** `platform/awt/{KeyBindings,HostSettings,SwingInputSource,GamePanel,SwingHost}.java`.

**Interfaces:** consume EngineConfig/FrameRenderer; produce the EngineHost implementation, bind/unbind and InputSource snapshots. HostSettings owns title/background/initial mode.

- [x] Write SwingInputSourceTest/GamePanelTest on EDT: W+Up then W release keeps MOVE_UP; repeat P does not repeat presses; brief press/release survives until snapshot; clear removes keys; unbind restores component mappings; frame failure ends the frame before reporting; the original Graphics2D state stays unchanged.
- [x] Run targeted tests and confirm missing behavior fails before implementation.
- [x] Implement key bindings, fixed content sizing, Timer, focus callbacks, renderer switching and error panel.
- [x] Repeat targeted and full tests; expect zero failures.
- [x] Include platform integration in the runtime milestone commit.

### Task 5: Configurable runtime preview

**Owner:** Sabirzhanov Emil. **Depends on:** tasks 1–4.

**Files:** `demo/{LaunchSettings,PreviewSettings,PreviewScene,DemoApplication}.java`, `src/main/resources/jse.properties`.

**Interfaces:** consume engine and graphics contracts. Produce startup from defaults plus optional `--config FILE`, optional `--help`, and a standalone Scene that exercises movement/pause/reset/render switching.

- [x] Write LaunchSettingsTest/PreviewSceneTest: external values override defaults; invalid keys/colors/timing and clipped object layout fail clearly; diagonal velocity preserves configured speed; pause/focus loss freeze movement; reset returns to center; changing Renderer preserves position.
- [x] Run targeted tests and confirm missing behavior fails before implementation.
- [x] Implement typed configuration, startup wiring, viewport-derived layout and input-derived help text.
- [x] Run the full suite and `./mvnw clean verify`; launch the JAR under a display; check assembled-window controls through installed Swing actions and closing. Native physical key delivery is recorded separately.
- [x] Commit the runnable preview with English project documentation.

## Foundation results

The behavior tests are grouped into nine test classes rather than one file per contract. Geometry and input first passed 3 tests; rendering/assets brought the suite to 6; core to 13; Swing boundaries to 16; preview/settings to 18. Final regression coverage brings the suite to 23 tests. Each implementation batch was preceded by a failing targeted run.

Independent review found shutdown during scene exit and logging during cache lookup. Both were reproduced with failing regression checks and fixed. See [verification](Verification.md) for final build and display results.

## Review focus

1. Extreme finite vectors normalize without overflow — GeometryTest.
2. A second physical alias does not retrigger a held logical command — SwingInputSourceTest.
3. A scene transition or stop cancels the remaining fixed steps — RuntimeTest.
4. A failed paint ends its frame before the failure callback — GamePanelTest.
5. New image IDs do not cause IO during paint — AssetManagerTest.

## Team integration milestones

| Owner | Next deliverable | Dependencies | Acceptance |
| --- | --- | --- | --- |
| Baktiyarova Aruzhan | GameObject/Player/Enemy/Pickup and SpawnRequest | Math, Graphic, HealthView; BehaviorStrategy contract with Yasmina | Typed model and validation tests |
| Roziyeva Yasmina | EventBus, event records, World/WorldView | Object model and math | Queued changes, immutable snapshots, stable dispatch and unsubscribe tests |
| Baktiyarova Aruzhan | EntitySpawner subclasses, Forest/Space families | Model, World, SpriteGraphic | Three categories × two families; pending-to-active transition |
| Roziyeva Yasmina | Patrol/Chase/Flee, motion/collision and ArenaRules | World and object model | Strategy swaps preserve IDs; single pickup collection; cooldown and loss priority |
| Sabirzhanov Emil | GraphicDecorator, Outline/HealthBar, game sprites and showcase | Existing Graphic; HealthView from Player | Composable layers, no growing chain on repeated toggles |
| All | Title/Arena/Showcase integration | Above modules | One packaged JAR, stable scene cleanup and shared rendering |

See [integration guide](Integration-Guide.md) for the concrete handoff contracts. The runtime milestone ends with working infrastructure; it does not mark teammates' game patterns complete.

## Release milestones

- Foundation: deterministic runtime and runnable preview.
- Integration: six working pattern structures inside the Arena example.
- Feature completion: scene controls, game outcomes, reset/theme switching and showcase.
- Release candidate: contract tests, diagrams matching code, screenshots and documentation.
- Release: fresh-checkout build, packaged resources, cross-platform checks and a tagged version.
