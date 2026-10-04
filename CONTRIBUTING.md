# Contributing to JSE

Project documentation, UI text, comments and diagnostics are English. Discuss changes in the language comfortable for the team, then record the resulting decisions in English.

## Ownership

Sabirzhanov Emil maintains the runtime, desktop platform, input, rendering and assets. Baktiyarova Aruzhan maintains game objects, product families and creation flows. Roziyeva Yasmina maintains World, AI, events, physics, game rules and game scenes. Core Scene and SceneManager belong to the runtime; ArenaScene, TitleScene and their state belong to the game.

Use [the integration guide](docs/Integration-Guide.md) before changing a shared signature. A module's owner reviews changes to its contracts. Ownership describes responsibility, not a restriction on assistance; commits record their actual contributors.

## Development

```sh
./mvnw clean verify
java -jar target/jse-demo.jar
```

Use a focused branch such as `feature/forest-family`, `feature/event-bus` or `feature/render-decorators`. Keep commits coherent and describe the behavior they introduce. Open a pull request with the problem, resulting behavior, validation and any remaining integration dependency. The default branch is `main`.

## Design rules

- Pass dependencies explicitly through constructors. Avoid global mutable state, engine lookups and service registries.
- Keep engine code independent of the Arena example. Game defaults belong in typed game settings or resource files.
- Keep mutable runtime and game state on the starting thread. Swing operations run on EDT.
- Retain no Graphics2D reference after endFrame. Load resources before rendering.
- Validate invariants where values enter a module. Prefer immutable records and defensive snapshots at boundaries.
- Comments explain a non-obvious reason or invariant. Names and small methods explain ordinary operations.
- Do not add a pattern solely to increase the pattern count. Each planned pattern has a specific role in the example.

## Verification and documentation

Write behavior tests for changed timing, lifecycle, factories, world mutations, event delivery and game rules. Use explicit timestamps instead of sleeps. Swing component tests run on EDT using buffered images; they do not require a desktop. Native window checks are reported separately.

Before requesting review, run the relevant checks and update docs or diagrams when a public contract changes. Do not claim cross-platform behavior, a complete Arena game or a working pattern without verifying it.

Keep generated build output and local source materials out of Git. Store original artwork and its attribution with the game resources when those are added. Maven Wrapper notices remain intact.
