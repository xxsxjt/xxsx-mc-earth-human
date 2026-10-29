# CurseForge publication record: Earth Human 0.1.20

## Project identity

- Authors project: `https://authors.curseforge.com/#/projects/1615063/files`
- Project ID: `1615063`
- Uploaded file ID: `8463721`
- Upload time: `2026-07-19 17:52 +08:00`

## Artifact

- File: `earth-human-neoforge-26.2-0.1.20.jar`
- Target: Minecraft 26.2 / NeoForge 26.2.0.7-beta / Java 25
- SHA256: `4AE2DA9ED991C1A1C6A847910100825F3F648683E13FACA7B49F10557BF512F1`
- Distribution boundary: the core project received only the core jar. The optional
  Body Health add-on was not bundled or uploaded to this project.

## Submitted metadata

- Display name: `Earth Human 0.1.20 PREVIEW TEST - MC 26.2 NeoForge`
- Release type: `beta`
- Manual release: enabled
- Changelog source: `docs/changelog-0.1.20.md`
- Description source: `docs/curseforge-description.md`
- Project icon source: `docs/assets/earth-human-mod-icon.png`

| Project-form choice | ID |
|---|---:|
| Minecraft 26.2 | 16498 |
| Java 25 | 14454 |
| NeoForge | 10150 |
| Client | 9638 |
| Server | 9639 |

## Verification and state

- The Upload API returned file ID `8463721`.
- The authenticated Authors file record returned the exact jar name and display name,
  all five project-form choices above, manual release enabled, and status code `1`.
- The project description was saved and reloaded with the prominent
  `PREVIEW / TEST BUILD ONLY` warning.
- CurseForge moderation and the later manual-release action are still pending. Upload
  acceptance is not being reported as public availability.
- The standalone project icon is prepared locally. Replacing the project logo is an
  Authors-editor action and is not performed by the file Upload API.

## Upload credential

- The Upload API token lives in the shared local vault (`D:/Users/xxsx/AppData/Local/
  RuntimeData`) under alias `curseforge_codex_api_token`. Read it only through that
  vault's local resolver and send it only in the `X-Api-Token` header; never print it,
  put it in a URL, or log it.
- Measured on 2026-10-29 by the vault owner after re-importing the value:
  `GET https://minecraft.curseforge.com/api/game/versions` with `X-Api-Token` returns
  `200` and 7,629 entries, while the same request without the header returns `401`.
  The stored value is therefore accepted by that Authors Upload API read-only endpoint.
- It is not a Studios `/v1` key: `x-api-key` on `GET https://api.curseforge.com/v1/games`
  and on `GET https://api.curseforge.com/v1/mods/1616748` both return `403`. The two
  headers are different auth schemes for different products and are not interchangeable;
  a `403` there says nothing about the token's validity for Authors. Neither probe
  touched this project (`1615063`).
- The upload path itself is separately blocked: every `POST` to
  `minecraft.curseforge.com` returns `403` with a Cloudflare JS challenge page, including
  `POST /api/game/versions`, so only `GET` reaches the API. A bogus token answers
  `400 errorCode 3` (malformed) and a real-but-unauthorized one `403 errorCode 5100`
  (no permission); those are different failures and must not be reported as "invalid
  token". `mc-agent/memory/curseforge-authors-upload.md` records the working fallback:
  drive the authenticated Authors page in a browser session, which is not challenged.
- No upload has been attempted for this project (`1615063`). `POST https://
  minecraft.curseforge.com/api/projects/1615063/uploadFile` is the real submission path
  and is unreachable while the Cloudflare challenge stands, so neither the token's upload
  permission nor a 0.1.20 re-submission is verified. Uploads recorded on 2026-10-29 for
  other projects went through the browser-session fallback, not this API path.
- `node scripts/publish-curseforge.cjs --dry-run` in the Temple of Nihility repo (its
  project is `1616748`) exits `2` on a `401` from `https://authors.curseforge.com/_api/
  project-files/1616748/create-project-file-form-data` and uploads nothing. That
  endpoint's browser-session authentication is separate from API-token authentication,
  so the script's "stored token rejected" wording is too broad and should not be read
  as a verdict on the token.
- Vault metadata for the alias now reads `validated-authors-readonly` with an allowlist
  limited to the probed read-only endpoints. Do not widen the allowlist or attempt an
  upload without the owner confirming the target project and operation scope.
- `mc-agent/memory/curseforge-authors-upload.md` is the durable write-up of this boundary
  (endpoint table, error-code meanings, the browser-session fallback, and the 2026-10-29
  eight-jar record). It supersedes any earlier note here that treated the token as
  unused or as invalid.
- The per-key status is kept only in the vault's own metadata; this note is a pointer,
  not a second copy.
