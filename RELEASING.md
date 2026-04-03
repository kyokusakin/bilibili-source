# Releasing

This repository derives its version from Git state:

- snapshots: `<commit>-SNAPSHOT`
- releases: a tag named `bilibili-source-<version>`

Example release tag:

```text
bilibili-source-1.0.0
```

## Requirements

- Git push access to `origin`
- GitHub access to create a Release in `kyokusakin/bilibili-source`
- Optional Maven credentials configured in GitHub:
  - repo variable: `MAVEN_USERNAME`
  - repo secret: `MAVEN_PASSWORD`

## Local verification

Before cutting a release:

```bash
./gradlew clean build publishToMavenLocal
```

## Create a release

1. Make sure `main` is up to date.
2. Create the release tag:

   ```bash
   git tag bilibili-source-1.0.0
   git push origin bilibili-source-1.0.0
   ```

3. Open GitHub Releases for this repository.
4. Create a new Release from the pushed tag.
5. Use the notes from `docs/releases/1.0.0.md` as a starting point.

## Workflows

- `build.yml`: validates every push to `main` and pull requests.
- `publish.yml`: runs on release publication, matching release tags, and manual dispatch.

When Maven credentials are present, `publish.yml` publishes artifacts and uploads the generated jars to the GitHub Release.
