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

## Aruzhan's first deliverable

Implement ObjectKind, ThemeId, SpawnRequest and the GameObject/Player/Enemy/Pickup category model. Preserve separate baseGraphic/current graphic, one-time World-assigned IDs and category/family invariants. Inject health, speed, pickup value and resource paths through settings rather than duplicating literals in product classes.

Agree BehaviorStrategy and entity mutation methods with Yasmina first. ID/active/removal mutations should remain package-private in `jse.world`, callable by World. Public clients use read-only identity and state accessors.

Then implement six products, GameObjectFamilyFactory, ForestFactory/SpaceFactory and three EntitySpawner subclasses. Keep final spawn's validation/registration flow separate from protected createObject. Wait for World.enqueueAdd instead of introducing a competing World implementation. Model and family construction tests can run earlier.

## Yasmina's first deliverable

Implement typed events, Subscription and EventBus independently, then World/EntityView/WorldView against the agreed model. Enforce exact-class dispatch, a batch snapshot, deferred publications, close semantics and deterministic IDs/queued changes. Inject capacity through settings.

Declare BehaviorStrategy early so Enemy can depend on its interface. Implement AI and physics after snapshots are stable. ArenaScene owns World, EventBus, rules and observers and closes them in onExit. EngineServices stays game-independent.

## Emil's delivered rendering module and next integration

GraphicDecorator, OutlineDecorator, HealthBarDecorator and immutable DecorationSettings are implemented. Use `GraphicLayers.compose(baseGraphic, playerOrNull, outline, healthBar, settings)` to rebuild from the undecorated base. It composes Outline then HealthBar; pass Player as HealthView and null for non-player objects. Passing an already decorated base is rejected to prevent accidental chain growth. Direct decorator construction still supports arbitrary nested layers.

Outline draws four bands outside the entity bounds; its width is independent of the renderer's stroke style. HealthBar reads live health, rejects nonpositive maxHealth and clamps the displayed ratio. Neither changes collision bounds. The independently runnable ShowcaseScene verifies the layers and uses a sample HealthView; it requires no World, ArenaRules or concrete Player.

Both families now have original PNGs at `sprites/{forest,space}/{player,enemy,pickup}.png`. Showcase preloads all six before startup. Factories can consume these paths through FamilySprites after the team agrees them. The editable source is `tools/generate_sprites.py` and attribution ships with the sprites.

The next integration requires the planned `jse.world` object model, including immutable baseGraphic/current graphic and Player implementing HealthView. The initial classes in incoming commit `2008b74` are console-only `jse.factory` sketches and do not yet provide these contracts. ArenaScene and TitleScene remain with Yasmina; connect Showcase navigation after those scenes exist.

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
