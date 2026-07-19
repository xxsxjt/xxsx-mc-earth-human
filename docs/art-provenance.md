# Art provenance

## Earth Human 0.1.20 item textures

- Scope: all 25 item textures under `assets/earth_human/textures/item`.
- Source: generated as 1254 px masters with `gpt-image-2` through the local
  `image2-secure-generator` workflow.
- Prompt contract: modern field-health equipment, readable 32 px silhouette,
  restrained material colors, no text, no watermark, and a solid chroma-key background.
- Review: the complete 32 px contact sheet was checked for silhouette, false text,
  background residue, visual consistency, and item-to-item distinction before adoption.
- Final processing: chroma-key alpha cleanup, centered square crop, BOX downsampling,
  64-color quantization without dithering, and binary alpha at a threshold of 96.
- Reproducible prompts: `docs/art/item-texture-prompts-0.1.20.jsonl`.

The generated masters are development artifacts and are intentionally excluded from the
release jar. The prompts and deterministic final-processing script remain in the repository.
