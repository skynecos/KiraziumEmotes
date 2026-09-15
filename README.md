# KiraziumEmotes

KiraziumEmotes is a modular emote framework for modern Paper servers.

## Project goals

- Emote rendering itself is server-side through ModelEngine; players do not need a client mod just to see/play an emote through commands.
- Optional `KiraziumEmotesClient` provides the G-key radial wheel. A Paper plugin cannot receive an otherwise unbound keyboard key from a vanilla client.
- Paper 26.1.2 / Java 25 baseline.
- First-class ModelEngine animation backend.
- Optional Nexo, ItemsAdder and Oraxen integrations.
- Clean public API so other plugins can start/stop emotes.
- Safe lifecycle handling for movement, damage, teleport, quit and plugin reloads.

## Current test milestone

The first bundled test emote is `floss`:

- ModelEngine model: `player_floss`
- animation: `dance`
- player skin is applied through ModelEngine PlayerLimb behaviors
- `/emote floss` starts it
- `/emote stop`, movement, damage, teleport, quit or death stops it
- the optional Fabric client opens an eight-slot radial menu with `G` and currently exposes The Floss in the first slot

On first server start KiraziumEmotes copies the bundled verified `player_floss.bbmodel` into ModelEngine's `blueprints` directory if that file is not already present. Existing administrator files are never overwritten. Because ModelEngine has already loaded by that point, restart the server once after the first installation before testing the bundled emote.

### Nexo + ModelEngine PlayerLimb requirement

Nexo automatically imports ModelEngine's generated resource pack, but Nexo excludes ModelEngine core shaders by default. ModelEngine PlayerLimb rendering needs those shaders. When using Nexo, disable:

`Pack.import.modelengine.exclude_shaders`

so the ModelEngine shaders are included in the merged pack. Restart/rebuild the resource pack after changing it.

## Compatibility target

| Component | Target |
| --- | --- |
| Paper | 26.1.2 |
| Java | 25 |
| ModelEngine | R4.1.1 |
| Nexo | 1.28.0 adapter target |
| ItemsAdder API | 4.0.18-beta-10 |
| Oraxen | 1.218.0 adapter target |
| Fabric client bridge | Minecraft 26.1.2, Loader 0.19.5, Fabric API 0.155.3+26.1.2 |

Compatibility targets are development targets, not blanket claims for every historical release.

## Architecture

- `core` — emote definitions, registry, sessions, cancellation and API.
- `render` — animation backends (ModelEngine first).
- `assets` — item providers for Nexo, ItemsAdder and Oraxen.
- `integration` — runtime detection and version-aware adapters.
- `command` — `/emote` command and admin tools.
- `client/` — optional Fabric G-key radial wheel.

## Third-party content

The initial `player_floss.bbmodel` test blueprint is pinned to an MIT-licensed upstream revision. See `THIRD_PARTY_NOTICES.md` for attribution and license text.

## Status

Early development (`0.1.0-SNAPSHOT`). CI must compile both the Paper plugin and the optional Fabric client bridge before a test artifact is considered usable.

## License

No project-wide license has been selected yet.
