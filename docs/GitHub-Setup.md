# GitHub Setup

The repository is local. No GitHub repository or remote is created by these instructions.

## First publication

Create an empty GitHub repository named `JSE` under the team's chosen account. Do not generate another README, .gitignore or license during creation. Review the source and choose a license separately if the team intends public reuse.

After reviewing the completed foundation locally, fast-forward main and add the URL copied from GitHub:

```sh
git switch main
git merge --ff-only feature/emil-runtime-foundation
git remote add origin <repository-url>
git push -u origin main
```

The feature branch contains the runtime and English documentation. The local fast-forward keeps its commits intact and makes the runnable version the default for first publication. For an existing shared repository, publish the feature branch and use a pull request instead. Existing commit history has not been rewritten.

Add the three contributors as collaborators. Enable branch protection for main if supported by the account, require the Java 17 build check and use pull requests for integration. Do not store tokens or machine-specific credentials in the repository.

## Team checkout

```sh
git clone <repository-url>
cd JSE
./mvnw clean verify
java -jar target/jse-demo.jar
```

Use `mvnw.cmd` on Windows. Once the foundation has merged, start each feature branch from updated main. Coordinate changes to shared contracts before merging dependent modules.

The existing GitHub Actions workflow builds/tests the project on Java 17 and uploads the executable JAR. Local verification does not claim that an unpushed workflow has run on GitHub.
