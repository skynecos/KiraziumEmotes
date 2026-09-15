# KiraziumEmotes Roadmap

KiraziumEmotes is intended to grow as a modular emote platform rather than a single command plugin.

## Product constraint

- [x] Plugin-only: players never need a KiraziumEmotes client mod
- [x] `/emotes` is the primary server-side menu entry point
- [x] Reference menu images are visual/layout references only; their emote content is not imported implicitly

## M0 — Foundation

- [x] Paper 26.1.2 / Java 25 baseline
- [x] Emote registry and YAML definitions
- [x] Per-player emote sessions
- [x] Safe stop lifecycle (move, damage, teleport, quit, death, disable, timeout)
- [x] Renderer abstraction
- [x] ModelEngine R4.1.x renderer
- [x] ModelEngine PLAYER_LIMB skin binding
- [x] Runtime integration detection
- [x] Nexo item asset provider
- [x] ItemsAdder item asset provider
- [x] Oraxen item asset provider
- [x] Public Bukkit service API
- [x] Java 25 CI build

## M1 — Emote presentation

- [x] Server-side inventory GUI
- [ ] Custom menu appearance using server resource-pack integrations
- [ ] Categories
- [ ] Search and pagination
- [ ] Favorites
- [ ] Provider-backed GUI icons
- [ ] Configurable sounds and particles
- [ ] Per-emote permissions
- [ ] Cooldowns
- [ ] Movement locking policy
- [ ] Config reload command

## M2 — Animation quality

- [ ] Original built-in animation library
- [ ] Loop / one-shot modes
- [ ] Entry and exit animation states
- [ ] Camera-safe behavior
- [ ] Equipment visibility policy
- [ ] Vehicle / gliding / swimming conflict policy
- [ ] Automatic animation-duration support where the renderer exposes reliable timing
- [ ] Model validation diagnostics

## M3 — Social emotes

- [ ] Two-player synchronized emotes
- [ ] Invitations and acceptance flow
- [ ] Group emotes
- [ ] Distance / world validation
- [ ] Safe synchronization cancellation

## M4 — Progression and persistence

- [ ] Unlockable emotes
- [ ] Permission-based unlocks
- [ ] Achievement / reward hooks
- [ ] Favorites persistence
- [ ] Player settings
- [ ] Storage abstraction
- [ ] SQLite baseline
- [ ] Optional MySQL/MariaDB

## M5 — Public platform

- [ ] Public API events
- [ ] Developer documentation
- [ ] PlaceholderAPI integration
- [ ] Admin diagnostics
- [ ] Compatibility test matrix
- [ ] Release artifacts and changelog automation

## Compatibility policy

Integrations are optional. KiraziumEmotes must still load when any optional integration is absent. Direct integration code is added only after its dependency and current API surface have been verified and compiled in CI.
