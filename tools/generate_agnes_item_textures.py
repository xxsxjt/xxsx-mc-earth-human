#!/usr/bin/env python3
"""Generate reviewed Earth Human item texture candidates through Agnes."""

from __future__ import annotations

import argparse
import base64
from collections import deque
from concurrent.futures import ThreadPoolExecutor, as_completed
import io
import json
import os
import pathlib
import time
import urllib.parse

import requests
import numpy as np
from PIL import Image, ImageDraw


ROOT = pathlib.Path(__file__).resolve().parents[1]
PROVIDERS = pathlib.Path(os.environ.get(
    "AGNES_PROVIDERS_PATH",
    str(pathlib.Path.home() / "WorkBuddy" / "agnes" / "providers.json"),
))
ENDPOINT = "https://apihub.agnes-ai.com/v1/images/generations"
MODEL = "agnes-image-2.1-flash"


def load_key() -> str:
    data = json.loads(PROVIDERS.read_text(encoding="utf-8", errors="ignore"))
    keys = data.get("agnes", {}).get("keys", [])
    if not isinstance(keys, list) or not keys:
        raise RuntimeError("Agnes provider has no usable key.")
    return str(keys[0])


def load_jobs(path: pathlib.Path) -> list[dict[str, str]]:
    jobs: list[dict[str, str]] = []
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if not line.strip():
            continue
        entry = json.loads(line)
        item_id = str(entry.get("id", "")).strip()
        prompt = str(entry.get("prompt", "")).strip()
        if not item_id or not prompt:
            raise RuntimeError("Every JSONL row needs non-empty id and prompt fields.")
        jobs.append({"id": item_id, "prompt": prompt})
    return jobs


def request_image(key: str, job: dict[str, str], attempts: int) -> bytes:
    body = {"model": MODEL, "prompt": job["prompt"], "size": "1024x1024", "n": 1}
    for attempt in range(1, attempts + 1):
        try:
            response = requests.post(
                ENDPOINT,
                headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
                json=body,
                timeout=300,
            )
            response.raise_for_status()
            payload = response.json()
            image = (payload.get("data") or [None])[0]
            if not isinstance(image, dict):
                raise RuntimeError("Agnes returned no image object.")
            if image.get("b64_json"):
                return base64.b64decode(image["b64_json"])
            url = image.get("url") or image.get("image_url")
            if not url:
                raise RuntimeError("Agnes image response has no URL or base64 data.")
            if url.startswith("data:image"):
                return base64.b64decode(url.split(",", 1)[1])
            if not urllib.parse.urlparse(url).scheme:
                raise RuntimeError("Agnes returned an invalid image URL.")
            download = requests.get(url, timeout=300)
            download.raise_for_status()
            return download.content
        except (requests.RequestException, RuntimeError) as exc:
            if attempt >= attempts:
                raise RuntimeError(f"{job['id']} failed after {attempts} attempts: {exc}") from exc
            time.sleep(min(60, attempt * 8))
    raise AssertionError("unreachable")


def remove_border_background(source: Image.Image) -> Image.Image:
    image = source.convert("RGBA")
    pixels = image.load()
    width, height = image.size
    corners = [pixels[0, 0], pixels[width - 1, 0], pixels[0, height - 1], pixels[width - 1, height - 1]]
    magenta = any(r > 150 and b > 150 and g < min(r, b) * 0.60 for r, g, b, _a in corners)
    green = any(g > 150 and g > r * 1.45 and g > b * 1.45 for r, g, b, _a in corners)
    average = tuple(sum(pixel[channel] for pixel in corners) // 4 for channel in range(3))

    if magenta or green:
        data = np.array(image, dtype=np.uint8, copy=True)
        rgb = data[:, :, :3].astype(np.int16)
        red, green_channel, blue = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
        if magenta:
            mask = ((red > 125) & (blue > 125)
                    & (green_channel * 100 < np.minimum(red, blue) * 76))
        else:
            mask = ((green_channel > 125)
                    & (green_channel * 100 > red * 132)
                    & (green_channel * 100 > blue * 132))
        data[:, :, 3][mask] = 0
        return Image.fromarray(data, "RGBA")

    def is_background(x: int, y: int) -> bool:
        r, g, b, _a = pixels[x, y]
        if magenta:
            return r > 130 and b > 130 and g < min(r, b) * 0.74
        if green:
            return g > 130 and g > r * 1.32 and g > b * 1.32
        return abs(r - average[0]) + abs(g - average[1]) + abs(b - average[2]) < 52

    queue: deque[tuple[int, int]] = deque()
    visited: set[tuple[int, int]] = set()
    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))

    while queue:
        x, y = queue.popleft()
        if (x, y) in visited or not is_background(x, y):
            continue
        visited.add((x, y))
        if x > 0:
            queue.append((x - 1, y))
        if x + 1 < width:
            queue.append((x + 1, y))
        if y > 0:
            queue.append((x, y - 1))
        if y + 1 < height:
            queue.append((x, y + 1))

    for x, y in visited:
        r, g, b, _a = pixels[x, y]
        pixels[x, y] = (r, g, b, 0)
    return image


def make_icon(raw: pathlib.Path, final: pathlib.Path, size: int) -> None:
    with Image.open(raw) as opened:
        image = remove_border_background(opened)
    box = image.getbbox()
    if box:
        image = image.crop(box)
    side = max(image.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.alpha_composite(image, ((side - image.width) // 2, (side - image.height) // 2))
    padding = max(2, side // 8)
    padded = Image.new("RGBA", (side + padding * 2, side + padding * 2), (0, 0, 0, 0))
    padded.alpha_composite(canvas, (padding, padding))
    final.parent.mkdir(parents=True, exist_ok=True)
    reduced = padded.resize((size, size), Image.Resampling.BOX)
    reduced = reduced.quantize(
        colors=64,
        method=Image.Quantize.FASTOCTREE,
        dither=Image.Dither.NONE,
    ).convert("RGBA")
    alpha = reduced.getchannel("A").point(lambda value: 255 if value >= 96 else 0)
    reduced.putalpha(alpha)
    reduced.save(final)


def make_contact_sheet(jobs: list[dict[str, str]], final_dir: pathlib.Path, output: pathlib.Path) -> None:
    cell_width = 136
    cell_height = 116
    columns = 4
    rows = (len(jobs) + columns - 1) // columns
    sheet = Image.new("RGBA", (columns * cell_width, rows * cell_height), (18, 23, 24, 255))
    draw = ImageDraw.Draw(sheet)
    for index, job in enumerate(jobs):
        path = final_dir / f"{job['id']}.png"
        if not path.exists():
            continue
        with Image.open(path) as opened:
            icon = opened.convert("RGBA").resize((80, 80), Image.Resampling.NEAREST)
        x = (index % columns) * cell_width
        y = (index // columns) * cell_height
        checker = Image.new("RGBA", (96, 88), (37, 44, 45, 255))
        checker_draw = ImageDraw.Draw(checker)
        for cy in range(0, 88, 8):
            for cx in range(0, 96, 8):
                if (cx // 8 + cy // 8) % 2 == 0:
                    checker_draw.rectangle((cx, cy, cx + 7, cy + 7), fill=(49, 57, 58, 255))
        checker.alpha_composite(icon, (8, 4))
        sheet.alpha_composite(checker, (x + 20, y + 4))
        draw.text((x + 6, y + 96), job["id"], fill=(225, 231, 226, 255))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prompts", type=pathlib.Path, required=True)
    parser.add_argument("--out-dir", type=pathlib.Path, required=True)
    parser.add_argument("--size", type=int, default=32, choices=[32, 64])
    parser.add_argument("--max-concurrent", type=int, default=2, choices=[1, 2])
    parser.add_argument("--attempts", type=int, default=4)
    args = parser.parse_args()

    jobs = load_jobs(args.prompts)
    raw_dir = args.out_dir / "raw"
    final_dir = args.out_dir / "final"
    raw_dir.mkdir(parents=True, exist_ok=True)
    final_dir.mkdir(parents=True, exist_ok=True)
    key = load_key()

    def generate(job: dict[str, str]) -> str:
        raw = raw_dir / f"{job['id']}.png"
        final = final_dir / f"{job['id']}.png"
        if not raw.exists():
            raw.write_bytes(request_image(key, job, args.attempts))
        make_icon(raw, final, args.size)
        return job["id"]

    failures: list[str] = []
    with ThreadPoolExecutor(max_workers=args.max_concurrent) as executor:
        future_jobs = {executor.submit(generate, job): job for job in jobs}
        for future in as_completed(future_jobs):
            job = future_jobs[future]
            try:
                print(f"generated {future.result()}", flush=True)
            except Exception as exc:
                failures.append(job["id"])
                print(f"failed {job['id']}: {exc}", flush=True)

    contact_sheet = args.out_dir / f"contact-sheet-{args.size}px.png"
    make_contact_sheet(jobs, final_dir, contact_sheet)
    print(f"contact_sheet={contact_sheet}", flush=True)
    if failures:
        raise SystemExit(f"missing={','.join(failures)}")


if __name__ == "__main__":
    main()
