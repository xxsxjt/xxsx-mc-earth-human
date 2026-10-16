# Earth Human 0.1.18 - Minecraft 26.2 / NeoForge Beta

## Added And Improved

- Consolidated the body model into three player-facing regions: head, torso, and limbs.
- Added migration for older left-arm, right-arm, left-leg, and right-leg save data without resetting existing injuries.
- Kept the body HUD in its normal configured screen corner while the vanilla survival inventory is open.
- Made the inventory body summary clickable without replacing the vanilla player-model area.
- Expanded the body status screen, field health guide, care feedback, display settings, and bilingual text.
- Added location-aware damage profiles, environmental contact tags, air-quality logic, thermal modeling, masks, and PPE hooks.
- Improved the public compatibility API and server-authoritative body recovery paths.
- Added and expanded automated tests for body migration, hit routing, thermal logic, air quality, and API behavior.

## Verification

- `gradlew clean test build --no-daemon` passed.
- 19 automated tests passed with no failures.
- 92 resource JSON files parsed successfully.
- English and Simplified Chinese language files contain the same 299 keys.
- NeoForge 26.2 dedicated server reached `Done (0.366s)` with Earth Human 0.1.18, Earth on Minecraft 0.1.9, Earth Online: Magic 0.4.2, and Earth Online: Xuanhuan 0.4.2 loaded together.

## Known Limits

- This is an in-development beta for Minecraft 26.2 and NeoForge 26.2.0.7-beta.
- The current verification covers build, automated tests, resources, deployment, and dedicated-server startup.
- Client gameplay, inventory clicking, guide navigation, injury handling, and care interactions require separate client testing.
- Save data, balance values, compatibility APIs, and interfaces may change in later beta versions.

SHA256: `1368D41B419D7A19A89BF5FA4D99CE7D0AD9A9558D078196D102710A24E1D54F`
