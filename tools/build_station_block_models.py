#!/usr/bin/env python3
"""Generate 32x32 opaque block textures and 3D multi-element block models for exercise stations."""

from __future__ import annotations

import json
import pathlib
from PIL import Image


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "earth_human"
BLOCK_TEXTURES = ASSETS / "textures" / "block"
BLOCK_MODELS = ASSETS / "models" / "block"


def fill_rect(
    img: Image.Image,
    x0: int,
    y0: int,
    x1: int,
    y1: int,
    base: tuple[int, int, int],
    alt: tuple[int, int, int],
    border: tuple[int, int, int] | None = None,
) -> None:
    pixels = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            is_edge = border is not None and (x in (x0, x1 - 1) or y in (y0, y1 - 1))
            if is_edge:
                c = border
            else:
                c = base if (x + y) % 2 == 0 else alt
            pixels[x, y] = (c[0], c[1], c[2], 255)


def make_face(uv: list[float], tex: str = "#tex") -> dict[str, object]:
    return {"uv": uv, "texture": tex}


def make_element(
    name: str,
    from_pos: list[int],
    to_pos: list[int],
    uv_up: list[float],
    uv_down: list[float],
    uv_side: list[float],
    uv_end: list[float] | None = None,
) -> dict[str, object]:
    end_uv = uv_end if uv_end is not None else uv_side
    return {
        "name": name,
        "from": from_pos,
        "to": to_pos,
        "faces": {
            "up": make_face(uv_up),
            "down": make_face(uv_down),
            "north": make_face(end_uv),
            "south": make_face(end_uv),
            "west": make_face(uv_side),
            "east": make_face(uv_side),
        },
    }


def build_training_mat() -> None:
    img = Image.new("RGBA", (32, 32), (58, 76, 52, 255))
    # Top Left [0..16, 0..16]: Olive anti-slip grid for mat body
    fill_rect(img, 0, 0, 16, 16, (68, 92, 60), (58, 80, 50), (46, 64, 40))
    px = img.load()
    for y in range(2, 14, 3):
        for x in range(2, 14):
            px[x, y] = (82, 108, 72, 255)
    # Top Right [16..32, 0..16]: Rolled mat end (slightly lighter olive with layered spiral ribs)
    fill_rect(img, 16, 0, 32, 16, (78, 104, 68), (64, 86, 56), (48, 66, 42))
    # Bottom Left [0..16, 16..32]: Black nylon strap with metallic buckle highlight
    fill_rect(img, 0, 16, 16, 32, (34, 36, 40), (26, 28, 32), (18, 20, 24))
    fill_rect(img, 5, 22, 11, 26, (150, 156, 164), (124, 130, 138))
    # Bottom Right [16..32, 16..32]: Dark rubberized underside
    fill_rect(img, 16, 16, 32, 32, (42, 52, 40), (36, 44, 34), (28, 36, 26))

    img.save(BLOCK_TEXTURES / "training_mat.png", format="PNG")

    model = {
        "textures": {
            "particle": "earth_human:block/training_mat",
            "tex": "earth_human:block/training_mat",
        },
        "elements": [
            make_element(
                "mat_body",
                [2, 0, 1],
                [14, 1, 15],
                uv_up=[0, 0, 8, 8],
                uv_down=[8, 8, 16, 16],
                uv_side=[0, 0, 8, 2],
            ),
            make_element(
                "rolled_head",
                [2, 1, 1],
                [14, 3, 4],
                uv_up=[8, 0, 16, 4],
                uv_down=[8, 4, 16, 8],
                uv_side=[8, 2, 12, 5],
                uv_end=[8, 0, 16, 3],
            ),
            make_element(
                "strap_left",
                [4, 0, 1],
                [5, 3, 4],
                uv_up=[1, 9, 3, 15],
                uv_down=[1, 9, 3, 15],
                uv_side=[1, 9, 5, 15],
            ),
            make_element(
                "strap_right",
                [11, 0, 1],
                [12, 3, 4],
                uv_up=[5, 9, 7, 15],
                uv_down=[5, 9, 7, 15],
                uv_side=[3, 9, 7, 15],
            ),
        ],
    }
    (BLOCK_MODELS / "training_mat.json").write_text(
        json.dumps(model, indent=2) + "\n", encoding="utf-8"
    )


def build_simple_dumbbell() -> None:
    img = Image.new("RGBA", (32, 32), (48, 52, 58, 255))
    # Top Left [0..16, 0..16]: Dark gray shock-absorbing rubber floor pad
    fill_rect(img, 0, 0, 16, 16, (54, 58, 64), (46, 50, 56), (36, 40, 44))
    # Top Right [16..32, 0..16]: Cast iron hex plates (matte charcoal with metallic rim)
    fill_rect(img, 16, 0, 32, 16, (72, 78, 86), (62, 68, 76), (92, 98, 108))
    # Bottom Left [0..16, 16..32]: Knurled chrome steel handle bar
    fill_rect(img, 0, 16, 16, 32, (186, 194, 204), (156, 164, 174), (128, 136, 146))
    # Bottom Right [16..32, 16..32]: Rubber pad underside & darker iron shadow
    fill_rect(img, 16, 16, 32, 32, (38, 42, 48), (32, 36, 42), (26, 28, 34))

    img.save(BLOCK_TEXTURES / "simple_dumbbell.png", format="PNG")

    model = {
        "textures": {
            "particle": "earth_human:block/simple_dumbbell",
            "tex": "earth_human:block/simple_dumbbell",
        },
        "elements": [
            make_element(
                "rubber_pad",
                [3, 0, 4],
                [13, 1, 12],
                uv_up=[0, 0, 8, 8],
                uv_down=[8, 8, 16, 16],
                uv_side=[0, 0, 8, 2],
            ),
            make_element(
                "front_handle",
                [6, 2, 6],
                [10, 3, 7],
                uv_up=[1, 9, 7, 11],
                uv_down=[1, 11, 7, 13],
                uv_side=[1, 9, 7, 11],
            ),
            make_element(
                "front_plate_left",
                [4, 1, 5],
                [6, 4, 8],
                uv_up=[9, 1, 12, 4],
                uv_down=[9, 4, 12, 7],
                uv_side=[9, 1, 13, 5],
            ),
            make_element(
                "front_plate_right",
                [10, 1, 5],
                [12, 4, 8],
                uv_up=[12, 1, 15, 4],
                uv_down=[12, 4, 15, 7],
                uv_side=[11, 1, 15, 5],
            ),
            make_element(
                "back_handle",
                [6, 2, 10],
                [10, 3, 11],
                uv_up=[1, 12, 7, 14],
                uv_down=[1, 13, 7, 15],
                uv_side=[1, 12, 7, 14],
            ),
            make_element(
                "back_plate_left",
                [4, 1, 9],
                [6, 4, 12],
                uv_up=[9, 1, 12, 4],
                uv_down=[9, 4, 12, 7],
                uv_side=[9, 2, 13, 6],
            ),
            make_element(
                "back_plate_right",
                [10, 1, 9],
                [12, 4, 12],
                uv_up=[12, 1, 15, 4],
                uv_down=[12, 4, 15, 7],
                uv_side=[11, 2, 15, 6],
            ),
        ],
    }
    (BLOCK_MODELS / "simple_dumbbell.json").write_text(
        json.dumps(model, indent=2) + "\n", encoding="utf-8"
    )


def build_resistance_band() -> None:
    img = Image.new("RGBA", (32, 32), (46, 52, 56, 255))
    # Top Left [0..16, 0..16]: Anti-slip footplate pad
    fill_rect(img, 0, 0, 16, 16, (56, 62, 68), (48, 54, 60), (36, 42, 48))
    # Top Right [16..32, 0..16]: Teal / cyan latex resistance tubes
    fill_rect(img, 16, 0, 32, 16, (44, 168, 154), (34, 144, 132), (26, 118, 108))
    # Bottom Left [0..16, 16..32]: Black EVA foam handles
    fill_rect(img, 0, 16, 16, 32, (36, 38, 42), (28, 30, 34), (20, 22, 26))
    # Bottom Right [16..32, 16..32]: Steel anchor D-ring & underside
    fill_rect(img, 16, 16, 32, 32, (164, 172, 182), (138, 146, 156), (96, 104, 114))

    img.save(BLOCK_TEXTURES / "resistance_band.png", format="PNG")

    model = {
        "textures": {
            "particle": "earth_human:block/resistance_band",
            "tex": "earth_human:block/resistance_band",
        },
        "elements": [
            make_element(
                "base_pad",
                [3, 0, 3],
                [13, 1, 13],
                uv_up=[0, 0, 8, 8],
                uv_down=[0, 0, 8, 8],
                uv_side=[0, 0, 8, 2],
            ),
            make_element(
                "anchor_buckle",
                [7, 1, 10],
                [9, 2, 12],
                uv_up=[9, 9, 13, 13],
                uv_down=[9, 11, 13, 15],
                uv_side=[9, 9, 13, 11],
            ),
            make_element(
                "band_left",
                [5, 1, 6],
                [6, 2, 11],
                uv_up=[9, 1, 11, 7],
                uv_down=[11, 1, 13, 7],
                uv_side=[9, 1, 15, 3],
            ),
            make_element(
                "band_right",
                [10, 1, 6],
                [11, 2, 11],
                uv_up=[12, 1, 14, 7],
                uv_down=[13, 1, 15, 7],
                uv_side=[9, 3, 15, 5],
            ),
            make_element(
                "grip_left",
                [4, 1, 4],
                [7, 2, 6],
                uv_up=[1, 9, 4, 12],
                uv_down=[1, 12, 4, 15],
                uv_side=[1, 9, 4, 11],
            ),
            make_element(
                "grip_right",
                [9, 1, 4],
                [12, 2, 6],
                uv_up=[4, 9, 7, 12],
                uv_down=[4, 12, 7, 15],
                uv_side=[4, 9, 7, 11],
            ),
        ],
    }
    (BLOCK_MODELS / "resistance_band.json").write_text(
        json.dumps(model, indent=2) + "\n", encoding="utf-8"
    )


def main() -> None:
    BLOCK_TEXTURES.mkdir(parents=True, exist_ok=True)
    BLOCK_MODELS.mkdir(parents=True, exist_ok=True)
    build_training_mat()
    build_simple_dumbbell()
    build_resistance_band()
    print("Generated 3 opaque 32x32 block textures and 3 multi-element 3D block models.")


if __name__ == "__main__":
    main()
