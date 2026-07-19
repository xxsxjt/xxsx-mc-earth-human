# Earth Human 0.1.20 current audit

## Release boundary

- Target: Minecraft 26.2 / NeoForge 26.2.0.7-beta / Java 25.
- Core artifact: `earth-human-neoforge-26.2-0.1.20.jar`.
- Optional add-on artifact: `earth-human-body-neoforge-26.2-0.1.1.jar`.
- The core keeps vanilla health, armor, absorption, healing, death, melee,
  projectile and explosion resolution authoritative.
- Core-specific injuries are deliberately narrow: high falls affect the legs;
  bare-hand hard-stone mining and bare-hand cactus contact affect the right hand.
- Split body health is owned by the separately installable Body Health add-on and
  is not deployed by default.

## Player-facing state

- The inventory keeps Minecraft's player model untouched and displays the clickable
  body summary in the configured normal HUD corner.
- The status screen has overview, body, quick-care and client display pages.
- Quick care reports ready, missing-item, active, completed and failed states using
  server-authoritative progress and item validation.
- Compact layouts use single-line care rows and wrapped recommendations; long English
  guidance no longer disappears behind an ellipsis.
- Offline preview coverage includes healthy, leg injury, hand injury, severe condition,
  missing supplies, care in progress, care failure, settings, HUD and inventory states.
- All 25 item textures use one reviewed modern field-health art family. Prompts and
  processing provenance are stored under `docs/art` and `docs/art-provenance.md`.

## Compatibility

- `com.xxsx.earthhuman.api.BodyHealthBridgeApi` is the supported split-health bridge.
- The bridge has explicit ownership, prevents silent add-on replacement and supports
  same-owner config reloads and clean `overlay_only` deactivation.
- The old `EarthHumanBodyHealthBridge` remains as a deprecated compatibility facade.
- The Body Health add-on compiles through a Gradle composite build against the public
  core coordinate and does not package core implementation classes into its jar.

## Verification evidence

- `python tools/validate_release_resources.py`: passed 92 JSON files, 25 complete
  item definition/model/texture chains, bilingual key parity and metadata alignment.
- Core `gradlew clean test build`: passed; focused tests cover core injury policy,
  injury math, public bridge ownership and the deprecated facade.
- Add-on `gradlew clean build`: passed through the composite build.
- Clean dedicated server, core only: loaded only `earth_human`, Minecraft and NeoForge;
  reached `Done` with zero error lines.
- Clean dedicated server, core plus add-on: loaded `earth_human` and
  `earth_human_body`; reached `Done` with zero error lines.
- Clean development client: reloaded `mod/earth_human`, created the item texture atlas
  and reached the main-menu resource-ready state with zero error lines. No world was entered.
- Core jar ZIP validation: 233 entries, 25 item textures, public bridge class present,
  no backup, prompt, provider or temporary files.
- Add-on jar ZIP validation: 13 entries, no embedded core internals.
- Archive tasks use fixed entry timestamps and reproducible ordering. Two clean core
  archive builds produced the same SHA256.

## Deployment

- Active instance:
  `D:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.2-NeoForge_26.2.0.7-beta\mods`
- Active core jar count: 1.
- Core SHA256: `4AE2DA9ED991C1A1C6A847910100825F3F648683E13FACA7B49F10557BF512F1`.
- Previous 0.1.19 jar was retained as
  `earth-human-neoforge-26.2-0.1.19.jar.bak-20260719-144747`.
- The optional Body Health add-on was intentionally not deployed to the normal test instance.

## GitHub prerelease

- Release: `earth-human-neoforge-26.2-v0.1.20`.
- URL: https://github.com/xxsxjt/xxsx-mc-earth-human/releases/tag/earth-human-neoforge-26.2-v0.1.20
- Published assets: the core 0.1.20 jar and optional Body Health 0.1.1 jar.
- Both exact jar URLs returned `302` to `release-assets.githubusercontent.com`.
- Both remote downloads matched the verified local SHA256 values above and in the release notes.

## Residual risk

- NeoForge 26.2.0.7-beta remains a beta target and ModDevGradle reports that it uses
  the latest known 26.1 capability model for the unknown `26.2` version string.
- Automated startup and deterministic previews do not replace player gameplay testing.
  This pass intentionally did not perform manual play or re-enable split health in the core.
