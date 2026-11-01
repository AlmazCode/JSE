# Emil Game Integration Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans task-by-task. The user authorized implementing the previously listed remaining work; execute inline and request one final independent code review.

**Goal:** Finish real-entity rendering, persistent visual controls, menu navigation and packaged integration verification.

**Architecture:** Use the actual teammate models and shared canonical dependencies. EntityGraphics is the rendering boundary; VisualPreferences and GameScenes own display preferences/navigation. ArenaRun owns a fresh World/EventBus and basic interactive model, ArenaScene draws it, standalone examples retain existing behavior.

**Tech Stack:** Java 17 release, Maven, Swing/Java2D and JUnit; JDK-only production dependencies.

**Spec:** ../specs/2026-11-01-emil-game-integration-design.md and ../../JSE-Technical-Specification.md.

## Global constraints

English public text; no game dependency in core; no IO during paint; preloaded classpath sprites; keep old preview CLI/bindings; actual contributor history retained; full AI/Observer gameplay acceptance is not claimed.

## Review focus

- Visual changes preserve model identity, health, position and collision bounds.
- Rendering does not retain removed entities or multiply decorator chains.
- Reset/theme/menu transitions close old run resources; queued additions/snapshots stay coherent.
- Escape has priority; focus loss pauses; visual commands work during pause/outcome.
- Configured keys, fonts, bars and sprite extents fit the viewport; packaged sprites are available in every scene.

### Task 1: Compile real shared models

**Status:** Complete.

Merge origin/feature/aruzhan-factory and origin/feature/yasmina-events-ai into feature/emil-game-integration. Add WorldTest, FamilyFactoryTest and IdleBehaviorTest before implementation; verify the missing dependencies fail compilation. Implement World, immutable EntityView/WorldView, RemovalReason and lifecycle events; correct the AI entity contract and implement Idle. Add six canonical theme products, ForestFactory/SpaceFactory with size validation. Preserve existing EventBus tests. Run the full suite.

### Task 2: Entity rendering and preferences

**Status:** Complete.

Create VisualPreferences and EntityGraphics. Add real image tests using Forest/Space Player/Enemy/Pickup, actual takeDamage, renderer replacement, 200 toggles and removal reference cleanup before code. Implement bounded composition, current-frame renderer propagation and clear on exit. Run targeted tests then the full suite.

### Task 3: Scene flow and packaged composition

**Status:** Complete.

Create GameSettings/game.properties, ArenaRun, ArenaScene, TitleScene and GameScenes. Extend Showcase with injected preferences and menu callback while preserving its standalone constructor. Add GAME launch mode and context input mappings; preload both families, preserve preview bindings, hide path labels in game wireframes. Tests first: real movement/health/collection/pause, Escape/reset/theme precedence, preference persistence, outcome visual controls, invalid layout/keys and at least 20 engine transition cycles. Implement and run targeted/full verification.

### Task 4: Review and verification

**Status:** Complete.

Run clean verify, packaged help/resources/config checks and a display smoke. Inspect screenshots of Title/Arena/Showcase in both renderer modes. Update spec/status/handoff/UML to distinguish implemented rendering/navigation and basic run from pending advanced AI/Observer work. Independent read-only code review; reproduce important findings with failing tests, fix and rerun suite. Keep feature work reviewable separately from main.

Verification: 67 tests pass; actual packaged window, live contact damage, native Robot Enter and 20 GUI route cycles checked. Final review found one menu layout case, reproduced RED and fixed GREEN. Full advanced AI/Observer remains explicitly pending.
