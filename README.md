# bilibili-source

A standalone Bilibili source manager for Lavaplayer and Lavalink.

This repository follows the same general module layout as `youtube-source`:

- `common`: reusable Bilibili source manager for Lavaplayer-compatible projects.
- `plugin`: Lavalink plugin wrapper around the `common` module.

Published on Maven Central under `io.github.kyokusakin`.

Current release:

- `io.github.kyokusakin:bilibili-common:1.0.2`
- `io.github.kyokusakin:bilibili-plugin:1.0.2`

Maven Central base repository URL:

```text
https://repo1.maven.org/maven2
```

## Build

```bash
./gradlew build
```

Output:

```text
common/build/libs/bilibili-common-<version>.jar
plugin/build/libs/bilibili-plugin-<version>.jar
```

## common

This module provides the base Bilibili source manager for Lavaplayer `1.x` environments.

Using in Gradle:

```kotlin
repositories {
  mavenCentral()
}

dependencies {
  implementation("io.github.kyokusakin:bilibili-common:1.0.2")
}
```

Example usage:

```java
AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
playerManager.registerSourceManager(new BilibiliAudioSourceManager());
```

## plugin

This module serves as the plugin for use with Lavalink v3.

To use the plugin with Lavalink, declare the dependency and configure the plugin block.

### Using with Lavalink v3

```yaml
lavalink:
  plugins:
    - dependency: "io.github.kyokusakin:bilibili-plugin:1.0.2"
      repository: "https://repo1.maven.org/maven2"
```

Artifact path:

```text
https://repo1.maven.org/maven2/io/github/kyokusakin/bilibili-plugin/1.0.2/
```

### Using with Lavalink v4

This repository currently targets Lavalink v3's `plugin-api` line (`3.6.1`). A true v4 plugin still requires migrating the `plugin` module to Lavalink v4's server/plugin APIs.

## Configuring the plugin

```yaml
plugins:
  bilibili:
    enabled: true
    playlistLoadLimit: 6
```

- `enabled`: whether the Bilibili source manager should be registered.
- `playlistLoadLimit`: limits how many Bilibili audio playlist pages are fetched. `-1` means unlimited.

> [!NOTE]
> This plugin keeps compatibility with the old fork-style config keys below, so existing users do not have to rename them immediately:
>
> - `lavalink.server.sources.bilibili`
> - `lavalink.server.bilibiliPlaylistLoadLimit`

### Local development

If you are testing local builds instead of publishing to a remote repository, use one of the following:

1. Run `./gradlew publishToMavenLocal` and load `bilibili-plugin` through `lavalink.plugins`.
2. Copy the `bilibili-plugin` jar from `plugin/build/libs/` into Lavalink's `plugins/` directory.

From `1.0.2` onward, `bilibili-plugin` is self-contained and already bundles the runtime classes from `bilibili-common`.

### Releases

GitHub Releases are set up to attach both built jars automatically:

- `bilibili-common-*.jar`
- `bilibili-plugin-*.jar`

Released artifacts are published to Maven Central.

## Versioning

- Snapshot builds use the current Git commit hash, for example `c98bb246dd23-SNAPSHOT`.
- Release builds only activate when `HEAD` is tagged with `bilibili-source-<version>`.
- Example release tag: `bilibili-source-1.0.2`
- Release process notes live in [RELEASING.md](RELEASING.md).

## Migration from the old fork

This repository started as a forked Lavalink tree with Bilibili support embedded in `LavalinkServer`. It has now been split into a standalone plugin repository.

If you were using the forked server:

1. Remove the old built-in fork jar.
2. Run standard Lavalink.
3. Add this plugin through `lavalink.plugins` or copy the built jars into `plugins/`.
4. Keep your previous Bilibili config temporarily if needed; the plugin still reads the legacy keys listed above.

## Development notes

- Modules: `common`, `plugin`
- Java target: `11`
- CI runtime: `17`
- Published artifacts:
  - `io.github.kyokusakin:bilibili-common`
  - `io.github.kyokusakin:bilibili-plugin`

## License

MIT. See [LICENSE](LICENSE).
