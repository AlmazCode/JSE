# Strategy and Observer — Roziyeva Yasmina

Report section for the two patterns owned by Yasmina. Class names are the real ones in `src/main/java`.

## Strategy — interchangeable enemy behavior

**Problem.** Enemies must patrol, chase or flee, and the behavior must be replaceable while the game runs, without `if/else` chains inside the enemy.

| Role | Class |
| --- | --- |
| Strategy | `jse.ai.BehaviorStrategy` — `calculateMovement(enemy, worldView, dt)` |
| Concrete strategies | `IdleBehavior`, `PatrolBehavior`, `ChaseBehavior`, `FleeBehavior` |
| Context | `jse.world.Enemy` — holds the current strategy, `setBehavior(...)` replaces it |
| Client | `jse.ai.AiSystem` — `update(...)` applies each enemy's strategy; `changeBehavior(...)` swaps it |
| Composition | `jse.demo.ArenaRun` gives enemies Patrol, Chase, Flee in turn and calls `AiSystem` every update |

**How it works.** `AiSystem.update` takes one immutable `WorldView` snapshot and asks every enemy's own strategy for a velocity. A strategy only returns an intent; `MotionSystem` is the only class that changes positions. `changeBehavior` replaces a strategy, keeps id, position and family, and publishes `BehaviorChanged` only when something really changed. Patrol keeps its own state (current waypoint), so every enemy gets its own instance.

**Why Strategy.** A new behavior is one new class implementing `BehaviorStrategy`; `Enemy`, `AiSystem` and the other behaviors are unchanged (Open/Closed). Any strategy can replace another (Liskov).

**Tests.** `BehaviorTest` (no player, zero distance, flee radius, patrol arrival without overshoot, runtime swap, one event per real replacement), `ArenaBehaviorsTest` (strategies inside the real Arena run).

## Observer — events instead of direct calls

**Problem.** The world and the rules must report what happened (item collected, health lost, game finished) without knowing who displays it.

| Role | Class |
| --- | --- |
| Subject | `jse.event.EventBus` — `subscribe`, `publish`, `drainDeferred`, `close` |
| Observer | `jse.event.EventListener<E>` |
| Subscription handle | `jse.event.Subscription` |
| Concrete observers | `jse.observer.HudObserver`, `jse.observer.EventLogObserver` |
| Publishers | `World` (EntitySpawned, EntityRemoved), `ArenaRules` (ItemCollected, HealthChanged, GameFinished), `AiSystem` (BehaviorChanged) |

**How it works.** Routing uses the exact event class. Events published while a dispatch is running are queued and delivered afterwards, so callbacks cannot re-enter the bus. `HudObserver` keeps score, health and result; `EventLogObserver` keeps the latest 50 lines. Both are created and closed by `ArenaRun`, so a reset or theme change never leaves old subscriptions behind.

**Why Observer.** `ArenaRules` never reads the HUD; the HUD or the log can be added, removed or replaced without touching the rules (Dependency Inversion, Single Responsibility).

**Tests.** `EventBusTest`, `ObserverTest` (HUD, six-type log, capacity, no replay after re-enabling, 20 create/close cycles without leaked subscriptions), `ArenaBehaviorsTest`.

## Typical defense questions

- *Where exactly is Strategy?* `Enemy` holds a `BehaviorStrategy`; `AiSystem.changeBehavior` swaps it at runtime.
- *Why not an `if` on a behavior enum?* A new behavior would require editing existing code; here it is one new class.
- *Why does the HUD not read the rules?* Observers only display; rules must not depend on them.
- *Why are events queued during dispatch?* Deterministic order and no re-entrancy.
- *Strategy vs State?* Strategies are chosen from outside (by the scene); they do not decide their own successor.
