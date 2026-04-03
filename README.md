# bilibili-source

A standalone Bilibili source plugin for Lavalink.

This repository follows the same general plugin-distribution style as `youtube-source`: build a jar, place it in Lavalink's `plugins` directory, or publish it to a Maven repository and load it through `lavalink.plugins`.

## Build

```bash
./gradlew build
```

Output:

```text
build/libs/bilibili-plugin-1.0.0-SNAPSHOT.jar
```

## Plugin

This module is intended for use with Lavalink v3.

To use the plugin with Lavalink, declare the dependency and configure the plugin block.

### Using with Lavalink v3

```yaml
lavalink:
  plugins:
    - dependency: "dev.lavalink.bilibili:bilibili-plugin:VERSION"
      repository: "https://maven.lavalink.dev/releases"
```

If you are building this repository yourself instead of publishing it, you can also copy the built jar directly into Lavalink's `plugins/` directory.

### Using with Lavalink v4

This repository currently targets Lavalink v3's `plugin-api` line. If you want a true v4 plugin, the main work left is upgrading off `plugin-api:3.6.1` and revalidating the Spring/plugin wiring against the v4 server APIs.

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

## Migration from the old fork

This repository started as a forked Lavalink tree with Bilibili support embedded in `LavalinkServer`. It has now been split into a standalone plugin repository.

If you were using the forked server:

1. Remove the old built-in fork jar.
2. Run standard Lavalink.
3. Add this plugin through `lavalink.plugins` or drop the jar into `plugins/`.
4. Keep your previous Bilibili config temporarily if needed; the plugin still reads the legacy keys listed above.

## Development notes

- Java target: `11`
- CI runtime: `17`
- Published artifact: `dev.lavalink.bilibili:bilibili-plugin`

## License

MIT. See [LICENSE](LICENSE).
