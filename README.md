# KiraziumEmotes

KiraziumEmotes is a modular, server-side emote framework for modern Paper servers.

## Project goals

- No client mod requirement.
- Paper 26.1.2 / Java 25 baseline.
- First-class ModelEngine animation backend.
- Optional Nexo, ItemsAdder and Oraxen integrations.
- Clean public API so other plugins can start/stop emotes.
- Safe lifecycle handling for movement, damage, teleport, quit and plugin reloads.
- One plugin JAR; integrations are optional and detected at runtime.

## Status

Early development (`0.1.0-SNAPSHOT`). The core session/registry architecture and integration contracts are being built first. An integration is only marked implemented after its current API has been verified and compiled.

## Planned modules inside the plugin

- `core` — emote definitions, registry, sessions, cancellation and API.
- `render` — animation backends (ModelEngine first).
- `assets` — icon/glyph/item providers for Nexo, ItemsAdder and Oraxen.
- `integration` — runtime detection and version-aware adapters.
- `command` — `/emote` command and admin tools.
- `ui` — inventory GUI / favorites / categories (later milestone).

## Compatibility target

| Component | Target |
| --- | --- |
| Paper | 26.1.2 |
| Java | 25 |
| ModelEngine | R4.1.x |
| Nexo | 1.27+ adapter target |
| ItemsAdder | current v4 API adapter target |
| Oraxen | current 1.218+ adapter target |

Compatibility targets are development targets, not blanket claims for every historical release.

## License

No license has been selected yet.
