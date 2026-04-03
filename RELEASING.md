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
- A verified Sonatype Central namespace: `io.github.kyokusakin`
- Central Portal user token
- GPG key pair for signing

## GitHub configuration

Set these repository settings before publishing:

- repo variable: `CENTRAL_NAMESPACE`
  - value: `io.github.kyokusakin`
- repo secret: `CENTRAL_PORTAL_USERNAME`
- repo secret: `CENTRAL_PORTAL_PASSWORD`
- repo secret: `SIGNING_IN_MEMORY_KEY`
- repo secret: `SIGNING_PASSWORD`

## Local verification

Before cutting a release:

```bash
./gradlew clean build publishToMavenLocal
```

To test a Central release locally:

```bash
./gradlew clean publish \
  -PcentralPortalUsername=<token username> \
  -PcentralPortalPassword=<token password> \
  -PsigningInMemoryKey=<ascii-armored private key> \
  -PsigningPassword=<gpg passphrase>
```

## Create a release

1. Make sure `main` is up to date.
2. Create the release tag:

   ```bash
   git tag bilibili-source-1.0.0
   git push origin bilibili-source-1.0.0
   ```

3. The `publish.yml` workflow will:
   - publish signed artifacts to the Sonatype compatibility endpoint
   - call the Central manual upload endpoint with `publishing_type=automatic`
4. Open GitHub Releases for this repository.
5. Create a new Release from the pushed tag.
6. Use the notes from `docs/releases/1.0.0.md` as a starting point.

## Workflows

- `build.yml`: validates every push to `main` and pull requests.
- `publish.yml`: runs on release publication, matching release tags, and manual dispatch.

When Sonatype token and signing secrets are present, `publish.yml` publishes artifacts to Central and uploads the generated jars to the GitHub Release.
