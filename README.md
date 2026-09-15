# KiraziumEmotes

KiraziumEmotes is a modular, server-side emote framework for modern Paper servers.

## Non-negotiable client policy

KiraziumEmotes is a **plugin-only** project. Players must not install Fabric, Forge, NeoForge or any KiraziumEmotes client mod.

- `/emotes` (also `/emote`) opens the server-side emote menu.
- ModelEngine is the first animation renderer.
- Nexo, ItemsAdder and Oraxen are optional server integrations for assets/items/resource-pack content.
- Reference screenshots are treated only as visual/layout inspiration. Their emote names or animations are never copied into the project unless explicitly requested.

A normal Paper plugin cannot read an otherwise-unbound keyboard key such as `G` from an unmodified vanilla client, so KiraziumEmotes does not implement or require a G-key bridge.

## Current foundation

- Paper 26.1.2 / Java 25 baseline.
- ModelEngine R4.1.1 renderer.
- ModelEngine PLAYER_LIMB skin binding.
- Runtime detection for ModelEngine, Nexo, ItemsAdder and Oraxen.
- Nexo, ItemsAdder and Oraxen item asset providers.
- Per-player emote sessions and safe cancellation.
- `/emotes` server-side inventory selector.
- `/emote <id>`, `/emote stop` and `/emote list` direct commands.
- Public Bukkit service API.

No third-party test dance or reference-image emote is bundled. Emotes are loaded from `emotes.yml` and must point to animations that actually exist in the selected renderer. An original built-in animation library can be developed separately and verified before release.

## Compatibility target

| Component | Target |
| --- | --- |
| Paper | 26.1.2 |
| Java | 25 |
| ModelEngine | R4.1.1 |
| Nexo | 1.28.0 adapter target |
| ItemsAdder API | 4.0.18-beta-10 |
| Oraxen | 1.218.0 adapter target |

Compatibility targets are development targets, not blanket claims for every historical release.

## Architecture

- `core` — emote definitions, registry, sessions, cancellation and API.
- `render` — animation backends (ModelEngine first).
- `assets` — item providers for Nexo, ItemsAdder and Oraxen.
- `integration` — runtime detection and version-aware adapters.
- `ui` — plugin-only inventory menus.
- `command` — `/emote` and `/emotes` entry points.

## Status

Early development (`0.1.0-SNAPSHOT`). CI compiles the Paper plugin on Java 25 before a test artifact is considered usable.

## License

No project-wide license has been selected yet.
