# Runtime Foundation Verification

Verified on **2026-10-04**, using **Java 17** on Linux. This record covers version 0.1.0, the runtime preview and its public contracts.

## Build and automated checks

```sh
./mvnw --batch-mode --no-transfer-progress clean verify
java -Djava.awt.headless=true -jar target/jse-demo.jar --help
```

The build completed successfully: **23 tests, 0 failures, 0 errors, 0 skipped**, across nine test classes. The JAR contains DemoApplication and jse.properties; compiled classes use Java 17 bytecode, major version 61. JUnit is absent from the production JAR.

| Test class | Protected behavior |
| --- | --- |
| GeometryTest | Extreme/zero normalization, finite endpoints, overlap and clamping |
| InputStateTest | Defensive immutable held/pressed snapshots |
| RendererImageTest | Bridge replacement, actual style differences, sprite drawing and frame guards |
| AssetManagerTest | Cache-only lookup, no draw-time diagnostics, preload once and fallback |
| RuntimeTest | Catch-up, timestamp boundaries, input consumption, transitions, failure/stop cleanup and thread confinement |
| SwingInputSourceTest | Physical aliases, autorepeat, taps, clear and binding restoration |
| GamePanelTest | Failure notification after frame end, copied graphics context |
| LaunchSettingsTest | Overrides, unknown keys, conflicting bindings, invalid values and layout |
| PreviewSceneTest | Normalized movement, pause/focus loss, reset and renderer replacement |

Invalid CLI arguments, invalid override values and headless window launch each returned the expected exit status 2 with an English diagnostic. Headless --help returned successfully.

Each implementation batch had a failing targeted run before implementation and a passing run afterward. Independent review found two important defects: shutdown inside scene exit could activate an orphaned replacement, and unprepared image lookup could log during painting. Both have regression checks observed failing against the original behavior and passing after correction. A further cleanup check protects against reusing the same exception during failure and exit.

## Display-enabled check

The packaged classes were assembled in a real SwingHost window under the available Linux display. The check verified:

- Visible native window with exactly 960×540 content dimensions.
- Running Swing timer and movement through the installed keyboard actions.
- Pause preventing movement, reset restoring the center and F1 changing actual image output.
- Window deactivation clearing held input.
- Normal window close disposing the frame, with repeated engine stop remaining safe.

Controls were invoked through the installed Swing InputMap/ActionMap. Native physical key delivery was **not verified**. Windows and macOS were **not tested**. The JAR entry point was also launched successfully under the display; this is separate from the headless test results.

[runtime-filled.png](screenshots/runtime-filled.png) and [runtime-wireframe.png](screenshots/runtime-wireframe.png) were rendered from the application's content component during the display-enabled check. They are application-content images, not desktop captures. Desktop capture was unavailable under the system's screen-capture permissions.

## Documentation and scope

PlantUML sources pass syntax checking and have regenerated SVG/PNG output. Existing-game diagrams are marked planned; runtime architecture/lifecycle diagrams describe implemented code. Public text is English and local Markdown links are checked before final packaging.

The Arena, factories, World, events, strategies and decorators remain planned. This record does not claim their acceptance checks have passed or all six design patterns are implemented. Bridge is implemented; Decorator is Emil's next milestone.

## Fresh source check

The implementation commit `e19b9d7` was exported with `git archive HEAD` into a separate empty directory. No ignored files, preexisting target directory or local source materials were copied.

From that exported source, Java 17 `./mvnw --batch-mode --no-transfer-progress clean verify` passed **23/23 tests** and produced the executable JAR. Packaged headless --help also returned successfully. The documentation update following this check changes no production or test code.

## Decorator and Showcase verification — 2026-10-19

The subsequent rendering milestone was verified on Linux using OpenJDK 25.0.3, compiling with Java 17 release bytecode. `./mvnw clean verify` passed **42 tests across 14 classes, 0 failures, 0 errors, 0 skipped**. Java 17 itself and other operating systems were not re-tested in this milestone.

New behavior checks cover:

- GraphicDecoratorTest: base/layer order, configurable bands, live/clamped health, positive maximum, renderer replacement using real pixels, independent layers and 200 recompositions without chain growth.
- ShowcaseSceneTest: comparison views, non-player health omission, 100 repeated toggles, theme/style preservation, renderer propagation, re-entry, focus behavior and quit precedence.
- ShowcaseSettingsTest: resource profiles, overrides, bindings, invalid values, and clipping of samples, bars, borders and resource labels.
- LaunchArgumentsTest: default preview, explicit examples, config option ordering and malformed/duplicate options.
- GameSpritesTest: six distinct transparent PNGs decode and match the preloaded image cache.
- RendererImageTest: sprite resource labels render below the image, without overlapping the health layer above it. The regression test failed with 253 unexpected pixels above the sprite before the change and passed afterward.

Both implementation batches first failed because their new public types were absent, then passed after implementation. Packaged headless `--help` succeeded; invalid CLI and headless window startup returned exit status 2. The JAR includes showcase.properties, all six PNGs and their attribution; rendering still performs no resource reads.

The packaged `DemoApplication` was opened on the available display with a real SwingHost at 960×540. Installed InputMap/ActionMap actions exercised F1, F2, F3, T and Escape with the running engine timer. Image checks confirmed plain, outline-only, health-only and combined states, Filled/Wireframe switching and normal window disposal. Forest/Space appearance and label placement were inspected in [showcase-filled.png](screenshots/showcase-filled.png), [showcase-wireframe.png](screenshots/showcase-wireframe.png), [showcase-space.png](screenshots/showcase-space.png) and [showcase-space-wireframe.png](screenshots/showcase-space-wireframe.png). These are content-component captures. X11 synthetic key injection did not establish reliable native input delivery; physical keyboard delivery remains unverified.

This milestone completes the independent rendering deliverable. Arena, Title navigation, real Player health binding, World, factories, events and strategies remain integration work. Existing early object sketches from `2008b74` do not yet supply the planned graphics/health API.

Independent review found two important issues: new Showcase bindings conflicted with old Preview overrides, and layout validation omitted configured help widths and font descent. Both were reproduced with failing tests and corrected. Binding conflict checks now include only the selected example's active actions; Preview can retain T/F2/F3 overrides. Showcase validation uses the actual configured help labels and font metrics. Its shared label formatter also keeps the footer correct after remapping. The final suite passes 42/42 tests.
