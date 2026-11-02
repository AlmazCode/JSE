# Verification — release 1.0.0

The current source passed `./mvnw --batch-mode --no-transfer-progress clean verify` on Linux using OpenJDK 17+35-2724 and Temurin 25.0.3. Both runs completed 96 tests in 26 classes, with no failures, errors or skipped tests. The build also checks Java formatting and creates an executable Java 17 JAR.

[verification.json](verification.json) records the source digest and the primary Java 17 test/desktop evidence. The report generator rejects a changed source digest or different test totals.

## Automated coverage

The suite covers fixed-step timing and bounded catch-up; lifecycle transitions, owner-thread rules and failure cleanup; input aliases and focus handling; family/category validation and world mutation; collision consequences and outcome priority; stateful enemy strategies; exact-class event delivery and subscription snapshots; independent observers; graphics/decorator composition; frame cleanup; and configuration/layout validation.

The rectangular layout regression checks both axes independently. Event tests check that nested publication waits for the current listener batch and that changes to subscriptions take effect on the next event.

## Desktop verification

`tools/DesktopVerification.java` opens the real Swing window against the packaged JAR. It checks Arena entry, renderer and strategy changes, HUD updates while the log is detached, empty log re-attachment, both families, Showcase and 20 cycles of reset/theme/menu navigation. Replaced event buses must be closed. Captures are saved under `docs/screenshots`.

A controlled contact fixture places an idle enemy on the player, then uses the normal simulation and event path to verify health 2/3 while the log is absent. This fixture is part of the verifier, not a production cheat key.

Native Enter success is recorded per run in `verification.json`. Other controls are exercised through installed Swing action bindings, which verifies their path into the engine. Complete physical keyboard coverage, Windows/macOS desktop behavior and performance benchmarks have not been established.

## Reproduce

```sh
./mvnw clean verify
java -Djava.awt.headless=true -jar target/jse-demo.jar --help
java -cp target/jse-demo.jar tools/DesktopVerification.java
python3 tools/evidence.py
```

The first two commands work headlessly; the verifier requires a desktop. For a release, the source ZIP is also extracted into a separate directory and built with the same Maven command. SHA-256 values for the delivered artifacts are stored in the generated submission manifest.
