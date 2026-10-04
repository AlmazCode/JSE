# GitHub Repository

The public repository is [AlmazCode/JSE](https://github.com/AlmazCode/JSE). The default branch is `main`.

## Team checkout

```sh
git clone https://github.com/AlmazCode/JSE.git
cd JSE
./mvnw clean verify
java -jar target/jse-demo.jar
```

Use `mvnw.cmd` on Windows. Start each feature branch from updated main and coordinate changes to shared contracts before merging dependent modules.

## Collaboration

The repository owner can add the three contributors as collaborators through GitHub settings. Use focused feature branches and pull requests for integration. Enable branch protection for main if supported by the account and require the Java 17 build check.

```sh
git switch main
git pull --ff-only
git switch -c feature/your-change
```

The existing GitHub Actions workflow builds/tests the project on Java 17 and uploads the executable JAR. Check each run's actual status on GitHub; a successful local build does not prove that a workflow passed remotely.

Do not store tokens or machine-specific credentials in the repository. Existing commit history is preserved. Choose a license separately if the team intends public reuse.
