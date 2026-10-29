## Earth Human 0.1.20 for Minecraft 26.1.2

PREVIEW / TEST BUILD ONLY. This incomplete build is published only for early preview,
compatibility testing, and bug reports. Back up worlds before testing. Features, balance,
saves, APIs, and interfaces may change or be removed without migration support.

This is the Minecraft 26.1.2 / NeoForge 26.1.2.124 build of the 0.1.20 core. It carries
the same gameplay as the 26.2 build, ported to the 26.1.2 API.

### Artifact

- `earth-human-neoforge-26.1.2-0.1.20.jar` - Earth Human core 0.1.20, Minecraft 26.1.2,
  NeoForge 26.1.2.124, Java 25. Required.
- SHA-256: `BCA36ACB1A2B7F756AB818D4BE231EE0F3E8FE7131A36D85E032B3036910CA7B`

The optional Body Health add-on is a separate project and is not part of this file.

### Changes from the 26.2 build

- Ported to Minecraft 26.1.2 / NeoForge 26.1.2.124. 26.1.2 has no `Hud` class, so screen
  switching uses `Minecraft.setScreen`, HUD offset uses `Gui.rightHeight`, and the
  hidden-HUD check uses `Options.hideGui`.
- `neoforge.mods.toml` now requires NeoForge `[26.1.2.124,)` and Minecraft `[26.1.2]`.
- `pack.mcmeta` spans resource format 84.0 through data format 101.1, the values in
  26.1.2's `version.json`.
- The archive name is `earth-human-neoforge-26.1.2` so a 26.1.2 jar cannot be mistaken
  for the 26.2 one.
- Gameplay, items, textures, recipes, advancements, commands and config are unchanged
  from 0.1.20.

### Verification

- `gradlew build`: passed, 36 tests, 0 failures.
- Resource validation: 92 JSON files, 25 complete item asset chains, bilingual key parity.
- Dedicated server on 26.1.2.124: reached `Done`, mod loaded, `Loaded 1542 recipes`
  (1515 vanilla + 27 mod), 1617 advancements, no error lines.
- Not verified: no game client was run for this build, so HUD stacking, the inventory body
  summary position, the status screen and quick-care interaction have source-level and
  server-level evidence only.
