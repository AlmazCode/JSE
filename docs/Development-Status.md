# Development Status

## Unified six-pattern application — 2026-11-02

The three team contributions are integrated into a single application while retaining contributor history. Latest factory sources from Aruzhan8698bb0 and AI/Observer sources from Yasmina1b6bc46 are combined with Emil0436677. Category-size validation is retained when reconciling duplicate factory/product files.

All six patterns are observable through `java -jar target/jse-demo.jar --example game`:

- Emil: Bridge switches actual Filled/Wireframe output with F1; Decorator composes outlines and real Player health with F2/F3. Visual choices survive reset, theme changes and menu/Showcase navigation.
- Aruzhan: three creator subclasses implement Factory Method; Forest/Space factories create six related products through Abstract Factory. T recreates the selected family through the existing spawners and World lifecycle.
- Yasmina: each enemy owns a Strategy, initialized Patrol/Chase/Flee and replaceable through Tab/B. AiSystem runs before motion. HudObserver and EventLogObserver follow six typed event categories; L removes/recreates only the log.

The Arena reserves a separate log region, draws observer-derived health/score, highlights the selected enemy and displays its current behavior. Rules own authoritative game state; observers are displays. Closed runs release observers, bus, World storage and applied graphic layers. Existing standalone Preview/Showcase behavior remains available.

## Verification and report

`./mvnw clean verify` passes 94 tests across 26 classes. Tests include real renderer/health pixels, six-product creation, real AI movement/swaps, independent observer updates, pause/outcome controls and 20 actual engine transition cycles. The display-only verifier additionally exercises 20 route cycles, Tab/B/L, both renderers and collision health while the log is detached. Its latest native Robot Enter attempt required the installed-action fallback; physical human input and Windows/macOS are not claimed as verified.

The English report draft has 17 pages, eight source listings and nine figures, with an editable DOCX and submission-format PDF under `docs/report`. It records actual class roles and test evidence, distinguishes responsibility from exclusive authorship and describes current event semantics. The report and source ZIP require team review before Moodle submission.

## Remaining release work

The coding/integration portion of Emil's listed work is complete. Remaining human tasks are reviewing cover/course details, practising the explanation of Bridge/Decorator and the common demonstration, and submitting the PDF/source archive. Verify other desktop platforms if they are part of the submission environment. Diagnostic spawning and a bounded next-tick event protocol remain future improvements, not completed features.
