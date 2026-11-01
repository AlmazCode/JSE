# Emil game rendering integration

The user requested the remaining Emil work described in the status review: attach graphics to real entities/health, use F1/F2/F3 in Arena without resetting gameplay, connect Title/Arena/Showcase, and verify the packaged application through repeated transitions.

Keep Java 17 release compilation, Swing/Java2D, JDK-only production dependencies, English public text and the existing Preview/standalone Showcase launch behavior. Core remains game-independent.

## Approach

Integrate the actual Aruzhan/Yasmina branches into a new local feature branch, retaining their commit authors. Complete the smallest real shared dependencies needed by those sources: immutable World snapshots and queued registration/removal, IdleBehavior, canonical Forest/Space products and factories. Correct BehaviorStrategy's Enemy import and retain calculateMovement compatibility alongside desiredVelocity. These are shared integration dependencies, not alternative entity/World implementations.

EntityGraphics renders GameObject.graphic from the supplied frame Renderer, applies GraphicLayers from baseGraphic only when visual flags change, reads Player's actual HealthView, and drops references to removed objects. VisualPreferences is owned by the application composition, survives scene transitions and never resets game identities/health/position during visual toggles.

`--example game` opens Title. Enter opens Arena; H opens Showcase; Escape returns to Title and closes only from Title. Standalone Showcase still closes with Escape. GameScenes owns navigation, theme and monotonic run IDs. ArenaRun owns a real World, Player and scene-local EventBus; ArenaScene handles commands and drawing through EntityGraphics. R/T create fresh runs retaining visual preferences, P/focus loss pauses. Basic movement, pickup collection and cooldown contact damage provide a real health/position source for the rendering integration. Enemies start Idle; advanced AI behaviors, AI selection, full Observer HUD/log and full game-pattern acceptance remain Yasmina's next work.

Game values and layout constraints come from a separate game.properties profile. Preload both families before opening the window; validate game and menu text/layout before startup. Game wireframes omit resource-path labels for crowded entity layouts; standalone Showcase retains them. Menu and Showcase display configured key labels.

## Boundaries and acceptance

Use the canonical World API from the existing specification. Keep Yasmina's current EventBus and legacy event classes working; World emits new canonical lifecycle records, while the complete deferred event pipeline remains explicitly pending. Do not advertise full Strategy/Observer completion. Do not merge into main or publish without the task's existing authorization.

Tests must prove real Player health affects pixels, F1/2/3 leave IDs/health/positions intact, no chain growth after toggles, removed objects are released, fresh runs preserve only preferences, Escape precedence, focus/pause and at least 20 real engine scene-transition cycles. Verify packaged resources, headless help/config errors, an actual Swing window and screenshot output. Record actual platform/input evidence and remaining team scope.
