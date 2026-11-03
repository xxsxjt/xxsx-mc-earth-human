## Earth Human 0.1.20 for Minecraft 26.1.2

PREVIEW / TEST BUILD ONLY. This incomplete build is published only for early preview,
compatibility testing, and bug reports. Back up worlds before testing. Features, balance,
saves, APIs, and interfaces may change or be removed without migration support.

This is the Minecraft 26.1.2 / NeoForge 26.1.2.124 build of the 0.1.20 core. It carries
the same gameplay as the 26.2 build, ported to the 26.1.2 API.

### Artifact

- `earth-human-neoforge-26.1.2-0.1.20.jar` - Earth Human core 0.1.20, Minecraft 26.1.2,
  NeoForge 26.1.2.124, Java 25 (built from commit `7e1966a`). Required.
- Size: `304,682 B`
- SHA-256: `ED9F5BD528738F7E0124221160564566838C72C8FF44D0C92DE082BE5395184E`

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
- New features in `d527c9a`–`7e1966a`:
  - **Water canteen filling (`d527c9a`)**: Right-clicking a water source block with an empty canteen (`canteen_empty`) fills it into a filled canteen (`canteen_filled`) with the bottle-fill sound without consuming the water source.
  - **Visible item & care actions (`d527c9a`)**: Right-clicking the thermometer (20 ticks, `BLOCK` pose), field health note (10 ticks, `BLOCK` pose), or PPE mask (12 ticks, `BRUSH` pose before equipping, including off-hand support) now plays a visible use animation. Active server-authoritative quick-care actions swing the main hand every 10 ticks on the client, and `/eh exercise` swings the main hand once.
  - **Placeable 3D exercise station blocks (`5ecc9b8`–`7e1966a`)**: Resistance band, training mat, and simple dumbbell are now placeable station blocks (`BlockItem`s keeping their existing item IDs and inventory icons). Right-clicking a placed station runs a timed server-authoritative exercise session (40–62 ticks) while switching the client to third-person view (restoring the previous camera afterward) and swinging the main hand every 8 ticks; moving more than 6 blocks away or breaking the block cancels the session with feedback. Each station uses a dedicated 64×64 opaque (`alpha=255`) shaded texture in the block atlas (`textures/block/*.png`) and a multi-element rotated 3D model (`training_mat`: 12 elements, `simple_dumbbell`: 18 elements, `resistance_band`: 16 elements) with a `14×4×14` footprint.

### Verification

- `gradlew build`: passed, 36 tests, 0 failures.
- Resource validation: 98 JSON files, 25 complete item asset chains, bilingual key parity.
- Artifact check (`7e1966a`): `earth-human-neoforge-26.1.2-0.1.20.jar` is `304,682 B` with SHA-256 `ED9F5BD528738F7E0124221160564566838C72C8FF44D0C92DE082BE5395184E`.
- Dedicated server on 26.1.2.124: reached `Done`, mod loaded, `Loaded 1542 recipes`
  (1515 vanilla + 27 mod), 1617 advancements, no error lines.
- Not verified in an automated headless run here: HUD stacking, the inventory body
  summary position, the status screen, and quick-care interaction are backed by source-level,
  resource-validator, and dedicated-server checks.
