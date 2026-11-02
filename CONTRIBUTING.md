# Working on JSE

Use JDK 17 or newer. Application text, source comments and committed documentation are in English. Keep the Java build independent of the optional report-publishing tools.

```sh
./mvnw clean verify
java -jar target/jse-demo.jar --example game
```

## Java style

The formatter is Eclipse JDT 4.35, run by Spotless with [the committed profile](config/java-format.xml). `verify` checks the result; format edits with:

```sh
./mvnw spotless:apply
```

Use four spaces and a 100-column wrapping target. Import types and static methods explicitly. Put braces around control-flow bodies and declare variables separately. Keep a blank line between methods and group fields by their role. Use `@Override` for implemented methods. Choose names that describe the game or runtime operation; comments should explain an invariant or a reason that the code alone does not show.

The source follows ordinary Java naming and control-flow conventions, with references to [Google Java Style](https://google.github.io/styleguide/javaguide.html) and [JUnit's contributor guide](https://github.com/junit-team/junit-framework/blob/main/CONTRIBUTING.md). Four-space indentation and the Eclipse profile are project choices, so this is not a claim of strict Google Style compliance. [libGDX's Group](https://github.com/libgdx/libgdx/blob/1.13.1/gdx/src/com/badlogic/gdx/scenes/scene2d/Group.java) was also consulted for focused scene operations and lifecycle documentation; its formatting conventions are not copied.

## Runtime boundaries

Pass dependencies through constructors. Keep game rules and theme selection outside `core`. Mutable runtime state belongs to the starting thread; the Swing host runs on the event dispatch thread. World mutations cross a commit boundary, while EventBus delivery is synchronous. Preserve those distinct contracts when changing integration code.

Load assets before rendering. A renderer may retain Graphics2D only between beginFrame and endFrame. Compose optional layers from the base graphic rather than from the current decorator chain. Close scene-owned observers and world state on exit.

Use behavior tests for changes to timing, lifecycle, event order, spawning and rules. Timing tests should advance explicit timestamps. Swing component tests execute on EDT using buffered images. Desktop verification is a separate check; it is not part of the headless Maven suite.

## Figures and verification

After a passing build, on a graphical desktop:

```sh
java -cp target/jse-demo.jar tools/DesktopVerification.java
python3 tools/evidence.py
```

The verifier captures `docs/screenshots` and writes `target/verification/desktop.json`. The evidence command records test totals, runtime details and a source digest in `docs/verification.json`. It must be run before another `clean`, which removes the desktop result. The verifier reports whether native Enter worked; most checks use registered Swing actions.

UML is written in PlantUML. Use PlantUML 1.2026.8 to regenerate PNG and SVG output under `docs/uml/rendered`; `style.puml` contains shared monochrome settings. Source diagrams remain the editable reference.

## Report and submission bundle

The report source is [report.md](docs/report/report.md). The generator extracts complete methods from Java and fills verification values from the recorded evidence. Python 3.10+ and python-docx are needed only to publish the report:

```sh
python3 -m venv /tmp/jse-report-env
/tmp/jse-report-env/bin/pip install -r tools/requirements-report.txt
/tmp/jse-report-env/bin/python tools/build_report.py
```

Export the DOCX with LibreOffice and its Python UNO bindings (`python3-uno` on Debian/Ubuntu):

```sh
/usr/bin/python3 tools/export_report.py
```

The exporter updates the table of contents and page references and creates a tagged PDF. Both report files are committed so the reviewer does not need publishing software.

After committing a verified tree, run `python3 tools/package_release.py`. It checks the source digest and report evidence and packages the exact commit into `release/JSE-Source.zip`, then creates `release/JSE-Submission.zip` with the report, runnable JAR, source archive, launchers and checksum manifest. The release directory is generated and stays outside Git.

Course handouts, local specifications, internal work notes, caches and build output do not belong in the source archive. Retain source, tests, build configuration, original assets and the documentation needed to inspect or reproduce the submission.
