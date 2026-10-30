#!/usr/bin/env python3
"""Validate Earth Human release metadata and player-visible resources."""

from __future__ import annotations

import json
import pathlib
import re
import sys
import tomllib

from PIL import Image


ROOT = pathlib.Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / "earth_human"
ITEM_SOURCE = ROOT / "src" / "main" / "java" / "com" / "xxsx" / "earthhuman" / "EarthHumanItems.java"


def load_json(path: pathlib.Path, errors: list[str]) -> object | None:
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"invalid JSON: {path.relative_to(ROOT)}: {exc}")
        return None


def registered_item_ids(errors: list[str]) -> set[str]:
    source = ITEM_SOURCE.read_text(encoding="utf-8")
    ids = set(re.findall(
        r"=\s*(?:guided|canteen|consumable|mask|thermometer|fieldNote|exercise|station)\(\"([a-z0-9_]+)\"",
        source,
    ))
    if not ids:
        errors.append("no registered item ids were found in EarthHumanItems.java")
    return ids


def validate_item_chain(item_id: str, errors: list[str]) -> None:
    definition_path = ASSETS / "items" / f"{item_id}.json"
    model_path = ASSETS / "models" / "item" / f"{item_id}.json"
    texture_path = ASSETS / "textures" / "item" / f"{item_id}.png"

    definition = load_json(definition_path, errors) if definition_path.exists() else None
    model = load_json(model_path, errors) if model_path.exists() else None
    if not definition_path.exists():
        errors.append(f"missing item definition: {item_id}")
    if not model_path.exists():
        errors.append(f"missing item model: {item_id}")
    if not texture_path.exists():
        errors.append(f"missing item texture: {item_id}")

    expected_model = f"earth_human:item/{item_id}"
    if isinstance(definition, dict):
        actual_model = definition.get("model", {}).get("model")
        if actual_model != expected_model:
            errors.append(f"item definition model mismatch for {item_id}: {actual_model!r}")
    if isinstance(model, dict):
        if model.get("parent") != "minecraft:item/generated":
            errors.append(f"item model parent mismatch for {item_id}: {model.get('parent')!r}")
        layer = model.get("textures", {}).get("layer0")
        if layer != expected_model:
            errors.append(f"item texture reference mismatch for {item_id}: {layer!r}")

    if texture_path.exists():
        try:
            with Image.open(texture_path) as opened:
                image = opened.convert("RGBA")
                if image.size != (32, 32):
                    errors.append(f"item texture must be 32x32: {item_id} is {image.size}")
                alpha = image.getchannel("A")
                if alpha.getbbox() is None:
                    errors.append(f"item texture is fully transparent: {item_id}")
                if alpha.getextrema()[0] != 0:
                    errors.append(f"item texture has no transparent padding: {item_id}")
        except OSError as exc:
            errors.append(f"invalid PNG: {item_id}: {exc}")


def validate_metadata(errors: list[str]) -> None:
    build_text = (ROOT / "build.gradle").read_text(encoding="utf-8")
    version_match = re.search(r"^version\s*=\s*'([^']+)'", build_text, re.MULTILINE)
    if not version_match:
        errors.append("build.gradle has no project version")
        return
    build_version = version_match.group(1)
    metadata_path = RESOURCES / "META-INF" / "neoforge.mods.toml"
    try:
        metadata = tomllib.loads(metadata_path.read_text(encoding="utf-8"))
    except (OSError, tomllib.TOMLDecodeError) as exc:
        errors.append(f"invalid neoforge.mods.toml: {exc}")
        return
    mods = metadata.get("mods", [])
    earth_human = next((entry for entry in mods if entry.get("modId") == "earth_human"), None)
    if earth_human is None:
        errors.append("neoforge.mods.toml has no earth_human entry")
    elif str(earth_human.get("version")) != build_version:
        errors.append(
            f"version mismatch: build.gradle={build_version}, neoforge.mods.toml={earth_human.get('version')}"
        )


def main() -> int:
    errors: list[str] = []
    json_paths = sorted(RESOURCES.rglob("*.json"))
    for path in json_paths:
        load_json(path, errors)

    language = {
        code: load_json(ASSETS / "lang" / f"{code}.json", errors)
        for code in ("zh_cn", "en_us")
    }
    if all(isinstance(value, dict) for value in language.values()):
        zh_keys = set(language["zh_cn"])
        en_keys = set(language["en_us"])
        for key in sorted(zh_keys - en_keys):
            errors.append(f"missing en_us language key: {key}")
        for key in sorted(en_keys - zh_keys):
            errors.append(f"missing zh_cn language key: {key}")

    item_ids = registered_item_ids(errors)
    for item_id in sorted(item_ids):
        validate_item_chain(item_id, errors)
        for code, entries in language.items():
            if isinstance(entries, dict) and f"item.earth_human.{item_id}" not in entries:
                errors.append(f"missing {code} item name: {item_id}")

    for folder in ("items", "models/item", "textures/item"):
        suffix = ".png" if folder == "textures/item" else ".json"
        actual = {path.stem for path in (ASSETS / folder).glob(f"*{suffix}")}
        for extra in sorted(actual - item_ids):
            errors.append(f"unregistered asset in {folder}: {extra}")
        for missing in sorted(item_ids - actual):
            errors.append(f"registered item missing from {folder}: {missing}")

    validate_metadata(errors)

    if errors:
        print("Earth Human resource validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print(
        f"Earth Human resource validation passed: {len(json_paths)} JSON files, "
        f"{len(item_ids)} complete item asset chains, bilingual key parity, metadata aligned."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
