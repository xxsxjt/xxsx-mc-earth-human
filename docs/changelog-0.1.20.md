# Earth Human 0.1.20 - in-development test build

> **PREVIEW / TEST BUILD ONLY**
>
> This incomplete build is published only for early preview, compatibility testing,
> and bug reports. Back up worlds before testing. Features, balance, saves, APIs, and
> interfaces may change or be removed without migration support.

## Artifacts

| File | Mod | Target | Required |
|---|---|---|---|
| `earth-human-neoforge-26.2-0.1.20.jar` | Earth Human core 0.1.20 | Minecraft 26.2 / NeoForge 26.2.0.7-beta | Yes |
| `earth-human-body-neoforge-26.2-0.1.1.jar` | Body Health add-on 0.1.1 | Minecraft 26.2 / NeoForge 26.2.0.7-beta | No |

Install the core jar for normal play. Install the Body Health add-on only when split
body health is wanted; it deliberately changes ordinary combat assumptions.

The CurseForge core project distributes only the core jar in this upload. The optional
Body Health add-on remains a separate artifact and is not bundled into the core file.

- Restored vanilla-compatible combat in the standalone core. Normal melee, projectiles,
  explosions, armor, absorption, healing and death are no longer routed through split health.
- Kept focused physical injuries: falls can injure legs and slow movement; bare-hand hard
  mining or cactus contact can injure the right hand and slow mining.
- Moved split body health behind the optional `Earth Human: Body Health` add-on and added
  a documented, owner-locked public bridge API.
- Improved compact status and care layouts, wrapped actionable recommendations, and added
  clearer missing-item, active-care and failure feedback.
- Added deterministic UI previews for normal, injured, severe, active and error states.
- Replaced all 25 item icons with a reviewed Image-2 modern field-health texture family.
- Added release resource validation for JSON, localization parity, metadata and complete
  item definition/model/texture chains.

## Verification

- Core `clean test build`: passed.
- Optional add-on `clean build`: passed against the public composite-build API.
- Core-only and core-plus-add-on dedicated servers both reached `Done` with no errors.
- A clean development client loaded the Earth Human resources and item atlas with no errors.
- Two clean core archive builds produced the same SHA256.

## SHA256

- Core 0.1.20: `4AE2DA9ED991C1A1C6A847910100825F3F648683E13FACA7B49F10557BF512F1`
- Body Health add-on 0.1.1: `12ECE6D65FA1CE6E8AB84BF8C91245FAFF45199672F777BD447773BDE8A33FFF`

This is a test build for Minecraft 26.2 and NeoForge 26.2.0.7-beta.
