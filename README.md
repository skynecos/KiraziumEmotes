# KiraziumEmotes

KiraziumEmotes is a modular, server-side emote framework for modern Paper servers.

## Client policy

KiraziumEmotes is **plugin-only**. Players do not install Fabric, Forge, NeoForge or a KiraziumEmotes client mod.

- `/emotes` (also `/emote`) opens the server-side selector.
- ModelEngine is the animation renderer.
- Nexo, ItemsAdder and Oraxen remain optional server integrations.
- Reference screenshots are visual/layout references only; their emote content is not imported implicitly.

A normal Paper plugin cannot read an otherwise-unbound client keyboard key such as `G`, so the project deliberately uses server-side commands/menus instead.

## Private bundled emotes

KiraziumEmotes supports private builds containing licensed emote assets without committing those assets to this public repository.

A private build may embed:

- `bundled/modelengine/kirazium_emotes.bbmodel`
- `bundled/emotes.yml`

During `onLoad`, before any plugin `onEnable` runs, KiraziumEmotes safely installs the bundled blueprint to:

`plugins/ModelEngine/blueprints/kiraziumemotes/kirazium_emotes.bbmodel`

The installer uses SHA-256 ownership tracking and refuses to overwrite a manually changed or unmanaged blueprint. Bundled emote definitions are merged into `plugins/KiraziumEmotes/emotes.yml` later without overwriting administrator-defined entries.

The licensed `.bbmodel` itself is intentionally excluded from the public repository.

## Nexo + ModelEngine player limbs

ModelEngine player-limb models require ModelEngine's core shaders. Nexo automatically imports ModelEngine's pack but removes ModelEngine core shaders by default. For player-limb emotes, configure Nexo so `Pack.import.modelengine.exclude_shaders` is disabled/false, then regenerate the Nexo pack.

## Current foundation

- Paper 26.1.2 / Java 25 baseline.
- ModelEngine R4.1.1 renderer.
- ModelEngine PLAYER_LIMB player-skin binding.
- ProtocolLib-backed visual suppression of armor and held items while an emote is active; inventory and combat stats are unchanged.
- Safe private blueprint installer that runs during plugin load.
- Safe bundled-emote config merger.
- Runtime detection for ModelEngine, Nexo, ItemsAdder and Oraxen.
- Nexo, ItemsAdder and Oraxen item asset providers.
- Per-player emote sessions and safe cancellation.
- `/emotes` server-side inventory selector.
- `/emote <id>`, `/emote stop` and `/emote list` direct commands.
- Public Bukkit service API.

## Compatibility target

| Component | Target |
| --- | --- |
| Paper | 26.1.2 |
| Java | 25 |
| ModelEngine | R4.1.1 |
| ProtocolLib | 5.4.0 compile API; installed 5.5.0 API inspected, client test required |
| Nexo | 1.28.0 adapter target |
| ItemsAdder API | 4.0.18-beta-10 |
| Oraxen | 1.218.0 adapter target |

Compatibility targets are development targets, not blanket runtime claims for every historical release.

## Architecture

- `core` — emote definitions, registry, sessions, cancellation and API.
- `render` — animation backends (ModelEngine first).
- `assets` — item providers for Nexo, ItemsAdder and Oraxen.
- `integration` — runtime detection and version-aware adapters.
- `bootstrap` — private licensed asset installation and config migration.
- `ui` — plugin-only inventory menus.
- `command` — `/emote` and `/emotes` entry points.

## Status

### Transparent quick menu

With UltimateUI enabled, `/emote` opens eight square shortcuts around an empty center.
Left-click plays; right-click opens the registered emote choices and assigns one to that shortcut.
Assignments persist per UUID in `plugins/KiraziumEmotes/wheels/`. Selecting an existing shortcut
swaps the two assignments. Without UltimateUI the original inventory selector remains available.
Set `ui.ultimateui: false` to explicitly use that fallback.

The optional bridge uses UltimateUI's public builder and click event through reflection; it does
not redistribute its plugin or resource pack. API signatures and close/teleport behavior were
inspected against the supplied UltimateUI Beta 1.3.3 JAR. Menu rendering still needs a client test.
The private build installs only its own icon ZIP into Nexo's `pack/external_packs/` directory;
UltimateUI's own resource pack must also be supplied to clients. Regenerate the combined server
pack after adding/updating icons. Do not replace ModelEngine or other packs with the icon-only ZIP.

Emote replacement validates the target first, reuses the active ModelEngine model where possible,
and cancels the previous timeout. Closing UltimateUI precedes playback because closing its camera
teleports the player. `gradle check` runs server-independent replacement/timeout/persistence checks;
these checks do not claim Minecraft-client visual coverage. Licensed animations are kept outside
this repository and outside public CI artifacts.

Early development (`0.1.0-SNAPSHOT`). CI compiles the Paper plugin on Java 25 before a test artifact is considered usable. Runtime validation on the target server is still required before calling a build production-ready.

## License

No project-wide license has been selected yet.
