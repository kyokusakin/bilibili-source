# Bilibili Plugin for Lavalink

Standalone Lavalink plugin that adds Bilibili audio and video loading support.

This repository is already split out from the Lavalink server tree. Running `./gradlew build` produces a plugin jar that can be dropped into Lavalink's `plugins` directory or published to your own Maven repository.

## Build

```bash
./gradlew build
```

Output jar:

```text
build/libs/bilibili-plugin-1.0.0-SNAPSHOT.jar
```

## Use with Lavalink

### Option 1: local jar

Copy the built jar into Lavalink's `plugins/` directory and restart Lavalink.

### Option 2: Maven dependency

```yaml
plugins:
  bilibili:
    enabled: true
    playlistLoadLimit: 6

lavalink:
  plugins:
    - dependency: "dev.lavalink.bilibili:bilibili-plugin:1.0.0-SNAPSHOT"
      repository: "https://your.repo/releases"
```

## Configuration

```yaml
plugins:
  bilibili:
    enabled: true
    playlistLoadLimit: 6
```

- `enabled`: whether the source manager should be registered.
- `playlistLoadLimit`: limits how many Bilibili audio playlist pages are fetched. `-1` means unlimited.

## Migration

For backward compatibility with the older forked Lavalink layout, the plugin still reads these legacy keys if present:

- `lavalink.server.sources.bilibili`
- `lavalink.server.bilibiliPlaylistLoadLimit`
