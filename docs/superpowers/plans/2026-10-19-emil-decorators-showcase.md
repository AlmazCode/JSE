# Emil Decorator and Showcase Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. The user requested implementation of the already specified week 6–7 work.

**Goal:** Deliver Emil's independent Decorator, six packaged sprites and an interactive Showcase, ready for later Arena integration.

**Architecture:** Keep the existing Renderer and game contracts stable. GraphicDecorator forwards renderer changes; immutable DecorationSettings and GraphicLayers compose Outline then HealthBar from a base Graphic. ShowcaseScene owns its display state and uses HealthView directly without World or unfinished entities. LaunchArguments selects preview (default) or showcase; LaunchSettings loads a separate showcase resource profile into typed ShowcaseSettings and validates layout before the window opens.

**Tech Stack:** Java 17, Maven Wrapper 3.9.9, Swing/Java2D, JUnit 5.10.2; JDK-only production dependencies.

**Spec:** ../../JSE-Technical-Specification.md (Decorator, assets, showcase, acceptance); ../../Integration-Guide.md (Emil's deliverable).

## Global Constraints

- Public text, documentation, comments and messages are English.
- Core must remain independent of World, EventBus and game rules.
- No IO during rendering; no Graphics2D survives a frame.
- Preserve preview's existing launch and control behavior.
- No changes to teammates' object model; document its current integration gap.
- Work in feature/emil-decorators-showcase; publishing is outside this task.

## Review Focus

1. Renderer replacement reaches all nested layers and the base; image test uses replacement after the first frame ends.
2. Zero/negative maximum health fails; changing current health is clamped each draw, including zero without a zero-width Rect.
3. Repeated toggles preserve a bounded chain and restore undecorated output.
4. Showcase layout rejects clipped borders, bars and labels before startup; test small viewport and large styling.
5. Every default sprite decodes from the packaged classpath and has visible transparent artwork.

### Task 1: Composable graphics

**Files:** Create render/GraphicDecorator.java, DecorationSettings.java, OutlineDecorator.java, HealthBarDecorator.java, GraphicLayers.java; test render/GraphicDecoratorTest.java.

**Interfaces:** GraphicDecorator(Graphic), OutlineDecorator(Graphic, DecorationSettings), HealthBarDecorator(Graphic, HealthView, DecorationSettings). GraphicLayers.compose(Graphic base, HealthView health, boolean outline, boolean healthBar, DecorationSettings settings) returns Graphic. A null health view means the object has no health layer. Outline uses four configurable-width rectangle bands outside bounds; both renderer styles interpret the same geometric layer. HealthBar reads current/max each draw, above the object, with clamped fill. Settings validates positive finite width/height and nonnegative offset.

- [x] Add image and recording-renderer tests: order, current/max clamping, invalid health/style, replacement, bounded repeated composition and unchanged object bounds.
- [x] Run ./mvnw -Dtest=GraphicDecoratorTest test; confirm missing feature fails.
- [x] Implement the decorators and composer.
- [x] Repeat the targeted command and full test suite.

### Task 2: Showcase, settings and launch

**Files:** Create demo/ShowcaseScene.java, ShowcaseSettings.java, LaunchArguments.java; modify demo/DemoApplication.java; create demo/ShowcaseLabels.java to share rendered/validated text; add resources/showcase.properties; test demo/ShowcaseSceneTest.java, ShowcaseSettingsTest.java, LaunchArgumentsTest.java and assets/GameSpritesTest.java. Add six sprite PNGs, editable source generator and attribution under resources/sprites.

**Interfaces:** LaunchArguments.parse(String[]) returns help/config/example choice; LaunchArguments.createScene(LaunchSettings) composes the chosen example. LaunchSettings.load(Path overrides, LaunchArguments.Example example) loads both resource profiles through one validated override map. ShowcaseSettings.from(Properties) parses visuals and validateLayout(EngineConfig, RenderStyle, KeyBindings) checks the selected showcase before startup. ShowcaseSettings holds decoration style, two three-sprite families and sample health. ShowcaseScene uses F1 renderer, F2 outline, F3 health, T theme, Escape close; two columns compare the base with the current decorated state; toggles exercise outline/health/combined with a rectangle and three product sprite rows. All visible choices are exposed through existing key labels. Health is applied only to the rectangle health sample and player sprite.

- [x] Add tests for CLI parsing/invalid values; configuration overrides and small viewport; showcase rendered output, toggles, renderer propagation, quit precedence and re-entry; six actual resources.
- [x] Run targeted tests; confirm missing feature fails.
- [x] Implement typed settings, scene, explicit launch composition and original sprite assets. Preload selected showcase resources before opening the host.
- [x] Repeat targeted tests and full suite.

### Task 3: Verification and handoff

**Files:** Update README.md, docs/Development-Status.md, Integration-Guide.md, Implementation-Plan.md, Verification.md, JSE-Technical-Specification.md and rendering UML; add Showcase screenshot.

- [x] Run ./mvnw clean verify; verify packaged help, resources and invalid CLI exit status.
- [x] Launch packaged Showcase under the available desktop; exercise F1/F2/F3/T and Escape; capture screenshots and check for clipping and errors.
- [x] Review diff for contract changes and inaccurate completion claims; update diagrams and documentation to reflect actual behavior and remaining game integration dependency.
