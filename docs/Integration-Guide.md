# Integration Guide

The runtime compiles independently. Aruzhan and Yasmina can implement their modules against the current public sources without waiting for the complete Arena.

## Existing contracts

- Math: finite Vec2, positive Rect, Rgba. Rect.contains accepts a point; object containment can be checked using bounds endpoints.
- Input: immutable InputState and GameAction. Core consumes presses per fixed step.
- Graphics: Graphic.draw(Rect), setRenderer(Renderer), SpriteGraphic(Renderer, SpriteId), RectangleGraphic(Renderer, Rgba). HealthView supplies current/max health.
- Resources: `new SpriteId("sprites/forest/player.png")`; add IDs to the application's preload list before startup. get is cache-only. Images are read-only to consumers.
- Scene: onEnter(EngineServices), update(dt, input), render(Renderer), onFocusLost(), onExit(). Core handles pending transitions and thread confinement.
- Services: config, initialRenderer and EngineControl. **There is no EventBus in EngineServices.** ArenaScene owns its game bus.

Review [public source](../src/main/java/jse) for exact signatures and [the specification](JSE-Technical-Specification.md) for invariants.

## Aruzhan model/factory handoff

ObjectKind, ThemeId, SpawnRequest, GameObject/Player/Enemy/Pickup, both factories, six products and three spawners are integrated and tested. For further changes, preserve separate baseGraphic/current graphic, one-time World-assigned IDs and category/family invariants. Inject health, speed, pickup value and resource paths through settings rather than duplicating literals in product classes.

Agree BehaviorStrategy and entity mutation methods with Yasmina first. ID/active/removal mutations should remain package-private in `jse.world`, callable by World. Public clients use read-only identity and state accessors.

Maintain the integrated six-product matrix, GameObjectFamilyFactory, ForestFactory/SpaceFactory and three EntitySpawner subclasses. Keep final spawn's validation/registration flow separate from protected createObject. Use the implemented World.enqueueAdd and immutable snapshots instead of introducing a competing World implementation. Model and family construction tests can run earlier.

## Yasmina integrated AI and observers

AiSystem and Patrol/Chase/Flee are now wired into ArenaRun before motion, and HUD/log observers are created before initial lifecycle events. Tab/B select and replace behavior; L toggles only the log. The synchronous event protocol is retained; a stronger bounded deferred contract remains optional follow-up. Enforce exact-class dispatch, a batch snapshot, deferred publications, close semantics and deterministic IDs/queued changes. Inject capacity through settings.

Maintain per-enemy Patrol state and use AiSystem.changeBehavior for a real replacement event. MotionSystem and CollisionSystem remain independent of AI choices. ArenaScene owns World, EventBus, rules and observers and closes them in onExit. EngineServices stays game-independent.

## Emil's completed rendering and scene integration

GraphicDecorator, OutlineDecorator, HealthBarDecorator and immutable DecorationSettings are implemented. Use `GraphicLayers.compose(baseGraphic, playerOrNull, outline, healthBar, settings)` to rebuild from the undecorated base. It composes Outline then HealthBar; pass Player as HealthView and null for non-player objects. Passing an already decorated base is rejected to prevent accidental chain growth. Direct decorator construction still supports arbitrary nested layers.

Outline draws four bands outside the entity bounds; its width is independent of the renderer's stroke style. HealthBar reads live health, rejects nonpositive maxHealth and clamps the displayed ratio. Neither changes collision bounds. The independently runnable ShowcaseScene verifies the layers and uses a sample HealthView; it requires no World, ArenaRules or concrete Player.

Both families now have original PNGs at `sprites/{forest,space}/{player,enemy,pickup}.png`. Showcase preloads all six before startup. Factories can consume these paths through FamilySprites after the team agrees them. The editable source is `tools/generate_sprites.py` and attribution ships with the sprites.

The real `jse.world` model is now integrated. EntityGraphics renders any current iterable of real GameObjects and composes Player health layers from its actual HealthView. It owns only applied visual layers, restores retired entities to their base graphic and closes on scene exit. VisualPreferences is shared through GameScenes, never through global/core state.

TitleScene, ArenaScene and the menu-hosted Showcase are connected through `--example game`. ArenaRun owns the World/EventBus lifetime and delegates motion/collisions/consequences to separate systems/rules. Twenty engine cycles exercise reset, theme, menu and Showcase cleanup. The grid runs Patrol/Chase/Flee and event-driven HUD/log displays. Additional reference layouts and diagnostic spawning remain follow-up work.

WorldView is now the immutable snapshot record defined by the shared API. Its old getPlayerPosition/isWithinBounds accessors remain compatibility adapters. BehaviorStrategy uses jse.world.Enemy, retains calculateMovement and offers desiredVelocity. Avoid reintroducing jse.factory entity sketches.

The inherited EventBus and EventListener/GameEvent marker contracts remain compatible and synchronous. New EntitySpawned/EntityRemoved/ItemCollected/HealthChanged/GameFinished records carry runId/tick. Coordinate the next deferred-dispatch migration with World and ArenaRules; do not assume the full queued Observer contract is implemented.
For rendering, use the Renderer passed to render on every frame; update current graphics with setRenderer. Do not retain a Graphics2D or cache a renderer choice only at scene entry. Factories use initialRenderer for construction; later frame rendering replaces it.

## Shared integration order

1. Agree model, BehaviorStrategy and settings types; implement events alongside the model.
2. Integrate World and family/spawner tests, including queued additions and pending spawn occupancy.
3. Integrate movement, collision, rules and observers under an ArenaScene.
4. Add game input bindings and preferences, Title/Arena/Showcase navigation.
5. Verify theme/reset cleanup, decorator toggles, packaged assets and release walkthrough.

Keep PreviewScene available as an independent runtime example when adding the game entry point. Select the example through explicit launch composition/configuration; avoid teaching core about Arena.

## Contract changes and checks

Changes to shared signatures require updating consumers, tests and specification in the same pull request or coordinating a compatible staged change. Keep pull requests small enough to build. Do not add null-returning stubs for another contributor's unfinished module.

Run `./mvnw clean verify`. Include behavior tests for the subsystem and one integration test when crossing a boundary. A renderer spy can verify delegation, but real image checks still verify actual Filled/Wireframe output. Native window verification is separate from headless component tests.
