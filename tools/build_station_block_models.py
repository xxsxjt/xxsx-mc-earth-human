#!/usr/bin/env python3
"""Generate 64x64 opaque block textures and multi-element 3D block models for exercise stations."""

from __future__ import annotations

import json
import math
import pathlib
from PIL import Image


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "earth_human"
BLOCK_TEXTURES = ASSETS / "textures" / "block"
BLOCK_MODELS = ASSETS / "models" / "block"


def clamp_byte(v: float) -> int:
    return max(0, min(255, int(round(v))))


def lerp_color(
    c0: tuple[int, int, int], c1: tuple[int, int, int], t: float
) -> tuple[int, int, int]:
    t = max(0.0, min(1.0, t))
    return (
        clamp_byte(c0[0] + (c1[0] - c0[0]) * t),
        clamp_byte(c0[1] + (c1[1] - c0[1]) * t),
        clamp_byte(c0[2] + (c1[2] - c0[2]) * t),
    )


def soft_hash(x: int, y: int, seed: int = 0) -> float:
    """Deterministic smooth micro-variation in [-1.0, 1.0] without checkerboard artifacts."""
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 32767.5 - 1.0


def make_face(
    uv: list[float], tex: str = "#tex", rotation: int | None = None
) -> dict[str, object]:
    face: dict[str, object] = {"uv": uv, "texture": tex}
    if rotation is not None:
        face["rotation"] = rotation
    return face


def make_element(
    name: str,
    from_pos: list[float],
    to_pos: list[float],
    uv_up: list[float],
    uv_down: list[float],
    uv_side: list[float],
    uv_end: list[float] | None = None,
    rotation: dict[str, object] | None = None,
) -> dict[str, object]:
    end_uv = uv_end if uv_end is not None else uv_side
    elem: dict[str, object] = {
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
    if rotation is not None:
        elem["rotation"] = rotation
    return elem


def paint_training_mat_texture() -> Image.Image:
    """
    64x64 layout (mapped to 0..16 UV space, where 1 UV unit = 4 px):
    - Quadrant TL [0..32, 0..32] (UV 0..8, 0..8): Olive TPE mat top with posture alignment lines & beveled border
    - Quadrant TR [32..64, 0..32] (UV 8..16, 0..8): Rolled cylinder shading (0..16 y = longitudinal cylinder shading, 16..32 y = spiral roll end-cap)
    - Quadrant BL [0..32, 32..64] (UV 0..8, 8..16): Nylon webbing straps (0..16 x) & brushed aluminum buckle (16..32 x)
    - Quadrant BR [32..64, 32..64] (UV 8..16, 8..16): Dark charcoal rubberized underside with subtle wave grip
    """
    img = Image.new("RGBA", (64, 64), (54, 72, 48, 255))
    px = img.load()

    # 1. Quadrant TL [0..32, 0..32]: Olive fitness mat surface
    for y in range(32):
        for x in range(32):
            # Radial + longitudinal soft cushion shading
            dx = (x - 15.5) / 16.0
            dy = (y - 15.5) / 16.0
            dist = math.sqrt(dx * dx * 0.7 + dy * dy * 0.7)
            base = lerp_color((86, 112, 74), (62, 84, 54), min(1.0, dist * 0.85))
            # Subtle horizontal micro-ribbing and soft grain
            rib = math.sin(y * math.pi / 2.0) * 2.5 + soft_hash(x, y, 11) * 1.8
            r, g, b = base[0] + rib, base[1] + rib, base[2] + rib

            # Beveled stitched piping border (outer 2px)
            edge_dist = min(x, y, 31 - x, 31 - y)
            if edge_dist == 0:
                r, g, b = 46, 62, 40
            elif edge_dist == 1:
                r, g, b = 98, 124, 84
            elif edge_dist == 2:
                r, g, b = 56, 76, 48

            # Laser-etched posture alignment lines (center axis x=15,16 + crossbars)
            if 4 <= y <= 27 and x in (15, 16):
                r, g, b = 122, 148, 106
            elif y in (7, 15, 23) and 7 <= x <= 24:
                r, g, b = 114, 140, 98
            elif (x in (6, 25) and y in (6, 7, 24, 25)) or (
                y in (6, 25) and x in (6, 7, 24, 25)
            ):
                # Corner alignment L-marks
                r, g, b = 106, 132, 92

            px[x, y] = (clamp_byte(r), clamp_byte(g), clamp_byte(b), 255)

    # 2. Quadrant TR [32..64, 0..32]: Rolled cylinder surface (y: 0..16) & concentric spiral end (y: 16..32)
    for y in range(16):
        # Smooth cylindrical highlight across y
        cyl_t = math.sin((y / 15.0) * math.pi)
        for x in range(32, 64):
            c = lerp_color((52, 70, 44), (104, 132, 88), cyl_t)
            grain = soft_hash(x, y, 22) * 1.5
            px[x, y] = (
                clamp_byte(c[0] + grain),
                clamp_byte(c[1] + grain),
                clamp_byte(c[2] + grain),
                255,
            )

    for y in range(16, 32):
        for x in range(32, 64):
            cx = x - 47.5
            cy = y - 23.5
            rad = math.sqrt(cx * cx + cy * cy)
            angle = math.atan2(cy, cx)
            # Concentric spiral layers for rolled mat side profile
            spiral = math.sin(rad * 2.1 + angle * 1.0)
            c = lerp_color((44, 58, 38), (94, 120, 80), (spiral + 1.0) * 0.5)
            if rad < 2.2:
                c = (34, 44, 30)
            px[x, y] = (c[0], c[1], c[2], 255)

    # 3. Quadrant BL [0..32, 32..64]: Black nylon herringbone webbing (x: 0..16) & metallic buckle (x: 16..32)
    for y in range(32, 64):
        for x in range(16):
            chevron = math.sin((x * 0.9 + (y % 4) * 0.8)) * 3.5
            edge_shade = -6.0 if x in (0, 15) else (3.0 if x in (1, 14) else 0.0)
            v = 34 + chevron + edge_shade + soft_hash(x, y, 33) * 1.2
            px[x, y] = (clamp_byte(v), clamp_byte(v + 2), clamp_byte(v + 5), 255)

        for x in range(16, 32):
            bx = x - 16
            by = y - 32
            # Brushed satin aluminum tri-glide buckle with dark center slot
            spec = math.sin((bx / 15.0) * math.pi) * 28.0 + (15 - by) * 1.2
            base_v = 148 + spec + soft_hash(x, y, 44) * 2.0
            if 4 <= bx <= 11 and by in (6, 7, 10, 11):
                base_v = 48  # recessed slot where nylon strap passes through
            px[x, y] = (
                clamp_byte(base_v - 4),
                clamp_byte(base_v),
                clamp_byte(base_v + 6),
                255,
            )

    # 4. Quadrant BR [32..64, 32..64]: Dark charcoal TPE underside with wave traction ribs
    for y in range(32, 64):
        for x in range(32, 64):
            wave = math.sin((x + math.sin(y * 0.5) * 2.0) * 0.9) * 4.0
            r = 40 + wave + soft_hash(x, y, 55) * 1.2
            g = 48 + wave + soft_hash(x, y, 55) * 1.2
            b = 38 + wave + soft_hash(x, y, 55) * 1.2
            px[x, y] = (clamp_byte(r), clamp_byte(g), clamp_byte(b), 255)

    return img


def paint_simple_dumbbell_texture() -> Image.Image:
    """
    64x64 layout:
    - Quadrant TL [0..32, 0..32] (UV 0..8, 0..8): Dual-tone shock-absorbing rubber floor pad with beveled rim
    - Quadrant TR [32..64, 0..32] (UV 8..16, 0..8): Octagonal cast-iron weight plates (beveled faces + recessed weight badge)
    - Quadrant BL [0..32, 32..64] (UV 0..8, 8..16): Brushed chrome knurled steel handle bar & spinlock hex collar
    - Quadrant BR [32..64, 32..64] (UV 8..16, 8..16): Charcoal rubber base rim & metallic nameplate
    """
    img = Image.new("RGBA", (64, 64), (44, 48, 54, 255))
    px = img.load()

    # 1. Quadrant TL [0..32, 0..32]: Slate gray cushioned inlay pad with recessed dumbbell grooves
    for y in range(32):
        for x in range(32):
            dx = (x - 15.5) / 16.0
            dy = (y - 15.5) / 16.0
            vignette = min(1.0, math.sqrt(dx * dx + dy * dy) * 0.9)
            c = lerp_color((68, 74, 82), (48, 52, 58), vignette)
            grain = soft_hash(x, y, 101) * 2.2
            r, g, b = c[0] + grain, c[1] + grain, c[2] + grain

            # Molded anti-rollindentations at y in 8..11 and 20..23
            if (8 <= y <= 11 or 20 <= y <= 23) and 5 <= x <= 26:
                r -= 7
                g -= 7
                b -= 6

            # Soft bevel border
            edge = min(x, y, 31 - x, 31 - y)
            if edge == 0:
                r, g, b = 36, 40, 46
            elif edge == 1:
                r, g, b = 82, 88, 96

            px[x, y] = (clamp_byte(r), clamp_byte(g), clamp_byte(b), 255)

    # 2. Quadrant TR [32..64, 0..32]: Matte cast-iron / urethane plates with metallic bevel edge
    for y in range(32):
        for x in range(32, 64):
            lx = x - 32
            ly = y
            # Metallic rim highlight around 16x16 sub-tiles
            tx = lx % 16
            ty = ly % 16
            cdx = tx - 7.5
            cdy = ty - 7.5
            rad = math.sqrt(cdx * cdx + cdy * cdy)

            # Directional light from top-left
            light = ((7.5 - cdx) * 0.6 + (7.5 - cdy) * 0.8) / 10.0
            base = lerp_color((52, 58, 66), (86, 94, 106), (light + 1.0) * 0.5)

            if tx in (0, 15) or ty in (0, 15):
                # Chamfered outer plate edge
                base = lerp_color((98, 108, 120), (42, 46, 52), (tx + ty) / 30.0)
            elif 3.2 <= rad <= 4.6:
                # Raised cast-iron flange ring
                base = (96, 104, 116)
            elif rad < 3.2:
                # Recessed inner hub with metallic collar sheen
                base = (44, 48, 56)

            grain = soft_hash(x, y, 202) * 1.8
            px[x, y] = (
                clamp_byte(base[0] + grain),
                clamp_byte(base[1] + grain),
                clamp_byte(base[2] + grain),
                255,
            )

    # 3. Quadrant BL [0..32, 32..64]: Chrome steel handle with fine diamond knurling & collar threads
    for y in range(32, 64):
        ly = y - 32
        # Cylindrical specular highlight across the bar height
        cyl = math.sin((ly / 15.0) * math.pi) if ly < 16 else math.sin(((ly - 16) / 15.0) * math.pi)
        spec_boost = 38.0 * (cyl ** 2.2)
        for x in range(32):
            if 4 <= x <= 27:
                # Fine diamond knurlingband (smooth diagonal cross-hatch, not 1x1 noise)
                knurl = math.sin((x + y) * 1.1) * math.cos((x - y) * 1.1) * 7.0
            else:
                # Smooth machined chrome sleeve ends
                knurl = 10.0
            v = 148 + cyl * 36.0 + spec_boost + knurl
            px[x, y] = (
                clamp_byte(v - 6),
                clamp_byte(v - 1),
                clamp_byte(v + 8),
                255,
            )

    # 4. Quadrant BR [32..64, 32..64]: Heavy charcoal rubber base border (y: 32..52) & brushed nameplate (y: 52..64)
    for y in range(32, 64):
        for x in range(32, 64):
            if y < 52:
                grad = (42 - y) * 0.7
                v = 34 + grad + soft_hash(x, y, 303) * 1.5
                px[x, y] = (clamp_byte(v), clamp_byte(v + 2), clamp_byte(v + 5), 255)
            else:
                # Brushed stainless nameplate with dark engraved stripe
                v = 168 + math.sin((x - 32) * 0.25) * 18.0
                if y in (57, 58) and 38 <= x <= 57:
                    v = 58
                px[x, y] = (clamp_byte(v - 4), clamp_byte(v), clamp_byte(v + 6), 255)

    return img


def paint_resistance_band_texture() -> Image.Image:
    """
    64x64 layout:
    - Quadrant TL [0..32, 0..32] (UV 0..8, 0..8): Anti-slip footplate platform with raised chevron grip zones
    - Quadrant TR [32..64, 0..32] (UV 8..16, 0..8): Glossy teal/cyan latex resistance tubes with specular highlight ridge
    - Quadrant BL [0..32, 32..64] (UV 0..8, 8..16): Contoured EVA foam handle grips (0..16 y) & nylon strap arms (16..32 y)
    - Quadrant BR [32..64, 32..64] (UV 8..16, 8..16): Brushed stainless steel U-anchor mount & dark base rim
    """
    img = Image.new("RGBA", (64, 64), (44, 50, 56, 255))
    px = img.load()

    # 1. Quadrant TL [0..32, 0..32]: Footplate platform with dual foot-tread zones
    for y in range(32):
        for x in range(32):
            dx = (x - 15.5) / 16.0
            dy = (y - 15.5) / 16.0
            base = lerp_color((64, 72, 80), (44, 50, 56), min(1.0, math.sqrt(dx * dx + dy * dy)))
            r, g, b = float(base[0]), float(base[1]), float(base[2])

            # Left and right foot tread grip zones with diagonal traction ribs
            if (4 <= x <= 12 or 19 <= x <= 27) and 5 <= y <= 26:
                rib = math.sin((y + (x if x < 16 else -x) * 0.5) * 1.4) * 6.0
                r += rib - 3
                g += rib - 2
                b += rib - 1

            # Teal accent trim line near front/back of platform
            if y in (3, 28) and 4 <= x <= 27:
                r, g, b = 42.0, 148.0, 138.0

            edge = min(x, y, 31 - x, 31 - y)
            if edge == 0:
                r, g, b = 30.0, 34.0, 40.0
            elif edge == 1:
                r, g, b = 78.0, 86.0, 96.0

            grain = soft_hash(x, y, 401) * 1.5
            px[x, y] = (
                clamp_byte(r + grain),
                clamp_byte(g + grain),
                clamp_byte(b + grain),
                255,
            )

    # 2. Quadrant TR [32..64, 0..32]: Glossy teal/cyan latex tubing with cylindrical specular sheen
    for y in range(32):
        for x in range(32, 64):
            # Repeat 8px tube strips across y so every tube UV samples a rounded glossy profile
            ty = (y % 8) / 7.0
            cyl = math.sin(ty * math.pi)
            spec = max(0.0, math.sin((ty - 0.15) * math.pi)) ** 4.0
            c = lerp_color((18, 98, 92), (52, 196, 180), cyl)
            r = c[0] + spec * 58.0
            g = c[1] + spec * 64.0
            b = c[2] + spec * 60.0
            px[x, y] = (clamp_byte(r), clamp_byte(g), clamp_byte(b), 255)

    # 3. Quadrant BL [0..32, 32..64]: Ridged EVA foam grip bar (y: 32..48) & reinforced nylon webbing (y: 48..64)
    for y in range(32, 48):
        ly = (y - 32) / 15.0
        cyl = math.sin(ly * math.pi)
        for x in range(32):
            # Ergonomic finger-groove rings along the foam grip
            groove = math.sin(x * math.pi / 3.0) * 4.5
            # Teal collar rings at both ends of the foam grip
            if x in (1, 2, 29, 30):
                px[x, y] = (46, 178, 164, 255)
            else:
                v = 28 + cyl * 24.0 + groove + soft_hash(x, y, 502) * 1.2
                px[x, y] = (clamp_byte(v), clamp_byte(v + 2), clamp_byte(v + 5), 255)

    for y in range(48, 64):
        for x in range(32):
            weave = math.sin((x + y * 0.5) * 1.2) * 3.0
            v = 36 + weave + soft_hash(x, y, 503) * 1.2
            px[x, y] = (clamp_byte(v), clamp_byte(v + 2), clamp_byte(v + 6), 255)

    # 4. Quadrant BR [32..64, 32..64]: Brushed stainless steel U-anchor mount
    for y in range(32, 64):
        ly = (y - 32) / 31.0
        for x in range(32, 64):
            lx = (x - 32) / 31.0
            sheen = math.sin(lx * math.pi) * 34.0 + math.cos(ly * math.pi) * 14.0
            v = 152 + sheen + soft_hash(x, y, 604) * 2.0
            px[x, y] = (clamp_byte(v - 5), clamp_byte(v), clamp_byte(v + 8), 255)

    return img


def build_training_mat() -> None:
    img = paint_training_mat_texture()
    img.save(BLOCK_TEXTURES / "training_mat.png", format="PNG")

    # 12 detailed elements: dual-layer bordered mat + south lip + sloped roll transition +
    # octagonal rolled cylinder head + protruding spiral core + 2 straps + 2 metallic buckles + carry sling
    model = {
        "textures": {
            "particle": "earth_human:block/training_mat",
            "tex": "earth_human:block/training_mat",
        },
        "elements": [
            make_element(
                "mat_underlayer",
                [1.8, 0.0, 1.0],
                [14.2, 0.55, 15.2],
                uv_up=[0.2, 0.2, 7.8, 7.8],
                uv_down=[8.2, 8.2, 15.8, 15.8],
                uv_side=[8.2, 9.0, 15.8, 10.0],
            ),
            make_element(
                "mat_top_surface",
                [2.2, 0.55, 3.8],
                [13.8, 1.1, 14.8],
                uv_up=[0.5, 0.5, 7.5, 7.5],
                uv_down=[8.5, 8.5, 15.5, 15.5],
                uv_side=[0.5, 0.5, 7.5, 1.5],
            ),
            make_element(
                "mat_south_curled_lip",
                [2.1, 0.45, 14.5],
                [13.9, 1.2, 15.3],
                uv_up=[0.5, 6.8, 7.5, 7.8],
                uv_down=[8.5, 14.5, 15.5, 15.5],
                uv_side=[8.5, 1.0, 15.5, 2.0],
                rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 0.8, 14.9]},
            ),
            make_element(
                "roll_ramp_transition",
                [2.1, 0.7, 3.0],
                [13.9, 1.65, 4.3],
                uv_up=[8.5, 0.5, 15.5, 2.5],
                uv_down=[8.5, 8.5, 15.5, 10.5],
                uv_side=[8.5, 1.0, 11.5, 2.2],
                rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 1.15, 3.65]},
            ),
            make_element(
                "roll_cylinder_orthogonal",
                [1.9, 0.9, 1.0],
                [14.1, 3.7, 3.8],
                uv_up=[8.5, 0.2, 15.5, 3.8],
                uv_down=[8.5, 0.2, 15.5, 3.8],
                uv_side=[8.2, 4.2, 15.8, 7.8],
                uv_end=[8.5, 0.2, 15.5, 3.8],
            ),
            make_element(
                "roll_cylinder_diagonal",
                [1.95, 0.9, 1.0],
                [14.05, 3.7, 3.8],
                uv_up=[8.5, 0.2, 15.5, 3.8],
                uv_down=[8.5, 0.2, 15.5, 3.8],
                uv_side=[8.2, 4.2, 15.8, 7.8],
                uv_end=[8.5, 0.2, 15.5, 3.8],
                rotation={"angle": 45, "axis": "x", "origin": [8.0, 2.3, 2.4]},
            ),
            make_element(
                "roll_inner_spiral_core",
                [1.65, 1.6, 1.7],
                [14.35, 3.0, 3.1],
                uv_up=[9.0, 1.0, 15.0, 3.0],
                uv_down=[9.0, 1.0, 15.0, 3.0],
                uv_side=[9.5, 4.8, 14.5, 7.2],
                uv_end=[9.0, 1.0, 15.0, 3.0],
                rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 2.3, 2.4]},
            ),
            make_element(
                "strap_left_webbing",
                [3.8, 0.75, 0.85],
                [4.9, 3.85, 3.95],
                uv_up=[0.5, 8.5, 3.5, 15.5],
                uv_down=[0.5, 8.5, 3.5, 15.5],
                uv_side=[0.5, 8.5, 3.5, 15.5],
            ),
            make_element(
                "strap_left_buckle",
                [3.65, 1.95, 0.65],
                [5.05, 3.15, 1.15],
                uv_up=[4.2, 8.5, 7.8, 12.5],
                uv_down=[4.2, 8.5, 7.8, 12.5],
                uv_side=[4.2, 8.5, 7.8, 12.5],
                rotation={"angle": -22.5, "axis": "x", "origin": [4.35, 2.55, 0.9]},
            ),
            make_element(
                "strap_right_webbing",
                [11.1, 0.75, 0.85],
                [12.2, 3.85, 3.95],
                uv_up=[0.5, 8.5, 3.5, 15.5],
                uv_down=[0.5, 8.5, 3.5, 15.5],
                uv_side=[0.5, 8.5, 3.5, 15.5],
            ),
            make_element(
                "strap_right_buckle",
                [10.95, 1.95, 0.65],
                [12.35, 3.15, 1.15],
                uv_up=[4.2, 8.5, 7.8, 12.5],
                uv_down=[4.2, 8.5, 7.8, 12.5],
                uv_side=[4.2, 8.5, 7.8, 12.5],
                rotation={"angle": -22.5, "axis": "x", "origin": [11.65, 2.55, 0.9]},
            ),
            make_element(
                "carry_sling_handle",
                [4.7, 3.55, 1.85],
                [11.3, 4.1, 2.85],
                uv_up=[0.5, 9.0, 3.5, 15.0],
                uv_down=[0.5, 9.0, 3.5, 15.0],
                uv_side=[0.5, 9.0, 3.5, 11.0],
                rotation={"angle": 22.5, "axis": "x", "origin": [8.0, 3.8, 2.35]},
            ),
        ],
    }
    (BLOCK_MODELS / "training_mat.json").write_text(
        json.dumps(model, indent=2) + "\n", encoding="utf-8"
    )


def dumbbell_assembly_elements(
    prefix: str, x_center: float, y_center: float, z_center: float
) -> list[dict[str, object]]:
    """Build an 8-element composite octagonal dumbbell centered at (x_center, y_center, z_center)."""
    # Handle half-length = 2.0, inner plate width = 1.25, outer step plate width = 0.65, spinlock nut = 0.45
    hx0, hx1 = round(x_center - 2.0, 2), round(x_center + 2.0, 2)
    lp_in0, lp_in1 = round(hx0 - 1.25, 2), hx0
    lp_out0, lp_out1 = round(lp_in0 - 0.65, 2), lp_in0
    lp_nut0, lp_nut1 = round(lp_out0 - 0.45, 2), lp_out0

    rp_in0, rp_in1 = hx1, round(hx1 + 1.25, 2)
    rp_out0, rp_out1 = rp_in1, round(rp_in1 + 0.65, 2)
    rp_nut0, rp_nut1 = rp_out1, round(rp_out1 + 0.45, 2)

    r_big = 1.35
    r_step = 0.95
    r_bar = 0.42
    r_nut = 0.48

    return [
        make_element(
            f"{prefix}_handle_bar",
            [hx0, round(y_center - r_bar, 2), round(z_center - r_bar, 2)],
            [hx1, round(y_center + r_bar, 2), round(z_center + r_bar, 2)],
            uv_up=[0.5, 8.5, 7.5, 11.5],
            uv_down=[0.5, 11.5, 7.5, 14.5],
            uv_side=[0.5, 8.5, 7.5, 11.5],
            uv_end=[0.5, 8.5, 3.5, 11.5],
            rotation={"angle": 45, "axis": "x", "origin": [x_center, y_center, z_center]},
        ),
        make_element(
            f"{prefix}_left_plate_oct_a",
            [lp_in0, round(y_center - r_big, 2), round(z_center - r_big, 2)],
            [lp_in1, round(y_center + r_big, 2), round(z_center + r_big, 2)],
            uv_up=[8.2, 0.2, 11.8, 1.8],
            uv_down=[8.2, 2.2, 11.8, 3.8],
            uv_side=[8.0, 0.0, 12.0, 4.0],
            uv_end=[8.2, 0.2, 9.8, 3.8],
        ),
        make_element(
            f"{prefix}_left_plate_oct_b",
            [round(lp_in0 + 0.04, 2), round(y_center - r_big, 2), round(z_center - r_big, 2)],
            [round(lp_in1 - 0.04, 2), round(y_center + r_big, 2), round(z_center + r_big, 2)],
            uv_up=[12.2, 0.2, 15.8, 1.8],
            uv_down=[12.2, 2.2, 15.8, 3.8],
            uv_side=[12.0, 0.0, 16.0, 4.0],
            uv_end=[12.2, 0.2, 13.8, 3.8],
            rotation={
                "angle": 45,
                "axis": "x",
                "origin": [round((lp_in0 + lp_in1) * 0.5, 2), y_center, z_center],
            },
        ),
        make_element(
            f"{prefix}_left_plate_step_and_nut",
            [lp_out0, round(y_center - r_step, 2), round(z_center - r_step, 2)],
            [lp_out1, round(y_center + r_step, 2), round(z_center + r_step, 2)],
            uv_up=[8.5, 4.2, 11.5, 5.8],
            uv_down=[8.5, 6.2, 11.5, 7.8],
            uv_side=[8.0, 4.0, 12.0, 8.0],
            uv_end=[8.5, 4.2, 10.0, 7.8],
            rotation={
                "angle": 22.5,
                "axis": "x",
                "origin": [round((lp_out0 + lp_out1) * 0.5, 2), y_center, z_center],
            },
        ),
        make_element(
            f"{prefix}_right_plate_oct_a",
            [rp_in0, round(y_center - r_big, 2), round(z_center - r_big, 2)],
            [rp_in1, round(y_center + r_big, 2), round(z_center + r_big, 2)],
            uv_up=[8.2, 0.2, 11.8, 1.8],
            uv_down=[8.2, 2.2, 11.8, 3.8],
            uv_side=[8.0, 0.0, 12.0, 4.0],
            uv_end=[8.2, 0.2, 9.8, 3.8],
        ),
        make_element(
            f"{prefix}_right_plate_oct_b",
            [round(rp_in0 + 0.04, 2), round(y_center - r_big, 2), round(z_center - r_big, 2)],
            [round(rp_in1 - 0.04, 2), round(y_center + r_big, 2), round(z_center + r_big, 2)],
            uv_up=[12.2, 0.2, 15.8, 1.8],
            uv_down=[12.2, 2.2, 15.8, 3.8],
            uv_side=[12.0, 0.0, 16.0, 4.0],
            uv_end=[12.2, 0.2, 13.8, 3.8],
            rotation={
                "angle": 45,
                "axis": "x",
                "origin": [round((rp_in0 + rp_in1) * 0.5, 2), y_center, z_center],
            },
        ),
        make_element(
            f"{prefix}_right_plate_step_and_nut",
            [rp_out0, round(y_center - r_step, 2), round(z_center - r_step, 2)],
            [rp_out1, round(y_center + r_step, 2), round(z_center + r_step, 2)],
            uv_up=[12.5, 4.2, 15.5, 5.8],
            uv_down=[12.5, 6.2, 15.5, 7.8],
            uv_side=[12.0, 4.0, 16.0, 8.0],
            uv_end=[12.5, 4.2, 14.0, 7.8],
            rotation={
                "angle": 22.5,
                "axis": "x",
                "origin": [round((rp_out0 + rp_out1) * 0.5, 2), y_center, z_center],
            },
        ),
        make_element(
            f"{prefix}_spinlock_collars",
            [lp_nut0, round(y_center - r_nut, 2), round(z_center - r_nut, 2)],
            [rp_nut1, round(y_center + r_nut, 2), round(z_center + r_nut, 2)],
            uv_up=[0.2, 12.2, 7.8, 15.5],
            uv_down=[0.2, 12.2, 7.8, 15.5],
            uv_side=[0.2, 12.2, 3.2, 15.5],
            uv_end=[0.2, 12.2, 7.8, 15.5],
        ),
    ]


def build_simple_dumbbell() -> None:
    img = paint_simple_dumbbell_texture()
    img.save(BLOCK_TEXTURES / "simple_dumbbell.png", format="PNG")

    # 3 base elements + 8 front dumbbell elements + 7 rear dumbbell elements = 18 elements
    elements: list[dict[str, object]] = [
        make_element(
            "rubber_base",
            [2.0, 0.0, 3.0],
            [14.0, 1.0, 13.0],
            uv_up=[8.2, 8.2, 15.8, 12.8],
            uv_down=[8.2, 8.2, 15.8, 12.8],
            uv_side=[8.2, 8.5, 15.8, 10.5],
        ),
        make_element(
            "cushion_inlay",
            [2.5, 1.0, 3.5],
            [13.5, 1.4, 12.5],
            uv_up=[0.0, 0.0, 8.0, 8.0],
            uv_down=[8.2, 8.2, 15.8, 12.8],
            uv_side=[0.2, 0.2, 7.8, 1.2],
        ),
        make_element(
            "metal_nameplate",
            [6.4, 0.95, 12.15],
            [9.6, 1.5, 12.9],
            uv_up=[9.0, 13.2, 15.0, 15.6],
            uv_down=[9.0, 13.2, 15.0, 15.6],
            uv_side=[9.0, 13.2, 15.0, 15.6],
            rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 1.2, 12.5]},
        ),
    ]
    elements.extend(dumbbell_assembly_elements("front", 7.5, 2.75, 6.0))
    # Rear dumbbell slightly staggered right for natural composition (7 elements)
    elements.extend(dumbbell_assembly_elements("back", 8.5, 2.75, 10.0)[:-1])

    model = {
        "textures": {
            "particle": "earth_human:block/simple_dumbbell",
            "tex": "earth_human:block/simple_dumbbell",
        },
        "elements": elements,
    }
    (BLOCK_MODELS / "simple_dumbbell.json").write_text(
        json.dumps(model, indent=2) + "\n", encoding="utf-8"
    )


def build_resistance_band() -> None:
    img = paint_resistance_band_texture()
    img.save(BLOCK_TEXTURES / "resistance_band.png", format="PNG")

    # 16 detailed elements:
    # - 2 platform layers (footplate_base + footplate_tread)
    # - 2 stainless steel U-anchor parts (anchor_mount_plate + anchor_u_shackle)
    # - 6 angled 0.8x0.8 teal latex tube segments (3 per side forming a 3D arch)
    # - 6 D-handle parts (left/right diagonal strap arms + contoured EVA foam grip crossbars)
    model = {
        "textures": {
            "particle": "earth_human:block/resistance_band",
            "tex": "earth_human:block/resistance_band",
        },
        "elements": [
            make_element(
                "footplate_base",
                [2.5, 0.0, 2.5],
                [13.5, 0.9, 13.5],
                uv_up=[0.2, 0.2, 7.8, 7.8],
                uv_down=[0.2, 0.2, 7.8, 7.8],
                uv_side=[0.2, 0.2, 7.8, 1.4],
            ),
            make_element(
                "footplate_tread",
                [3.1, 0.9, 3.1],
                [12.9, 1.35, 12.9],
                uv_up=[0.0, 0.0, 8.0, 8.0],
                uv_down=[0.2, 0.2, 7.8, 7.8],
                uv_side=[0.5, 0.5, 7.5, 1.2],
            ),
            make_element(
                "anchor_mount_plate",
                [6.4, 1.35, 10.2],
                [9.6, 1.85, 12.4],
                uv_up=[8.5, 8.5, 15.5, 15.5],
                uv_down=[8.5, 8.5, 15.5, 15.5],
                uv_side=[8.5, 9.0, 15.5, 11.0],
            ),
            make_element(
                "anchor_u_shackle",
                [6.85, 1.65, 9.4],
                [9.15, 2.85, 11.3],
                uv_up=[9.0, 9.0, 15.0, 15.0],
                uv_down=[9.0, 9.0, 15.0, 15.0],
                uv_side=[9.0, 10.0, 15.0, 14.0],
                rotation={"angle": -22.5, "axis": "x", "origin": [8.0, 2.1, 10.35]},
            ),
            # Left 3-segment curved teal latex tube (0.8 x 0.8 cross section)
            make_element(
                "band_left_rear",
                [6.2, 1.75, 8.1],
                [7.0, 2.55, 10.3],
                uv_up=[8.5, 0.0, 10.5, 2.0],
                uv_down=[8.5, 2.0, 10.5, 4.0],
                uv_side=[8.5, 0.0, 15.5, 2.0],
                uv_end=[8.5, 0.0, 10.5, 2.0],
                rotation={"angle": 22.5, "axis": "y", "origin": [6.6, 2.15, 9.2]},
            ),
            make_element(
                "band_left_arch_mid",
                [5.1, 2.05, 6.1],
                [5.9, 2.85, 8.5],
                uv_up=[8.5, 2.0, 10.5, 4.0],
                uv_down=[8.5, 4.0, 10.5, 6.0],
                uv_side=[8.5, 2.0, 15.5, 4.0],
                uv_end=[8.5, 2.0, 10.5, 4.0],
                rotation={"angle": 22.5, "axis": "x", "origin": [5.5, 2.45, 7.3]},
            ),
            make_element(
                "band_left_front",
                [4.8, 1.85, 4.5],
                [5.6, 2.65, 6.4],
                uv_up=[8.5, 4.0, 10.5, 6.0],
                uv_down=[8.5, 6.0, 10.5, 8.0],
                uv_side=[8.5, 4.0, 15.5, 6.0],
                uv_end=[8.5, 4.0, 10.5, 6.0],
                rotation={"angle": -22.5, "axis": "x", "origin": [5.2, 2.25, 5.45]},
            ),
            # Right 3-segment curved teal latex tube (0.8 x 0.8 cross section)
            make_element(
                "band_right_rear",
                [9.0, 1.75, 8.1],
                [9.8, 2.55, 10.3],
                uv_up=[10.5, 0.0, 12.5, 2.0],
                uv_down=[10.5, 2.0, 12.5, 4.0],
                uv_side=[8.5, 0.0, 15.5, 2.0],
                uv_end=[10.5, 0.0, 12.5, 2.0],
                rotation={"angle": -22.5, "axis": "y", "origin": [9.4, 2.15, 9.2]},
            ),
            make_element(
                "band_right_arch_mid",
                [10.1, 2.05, 6.1],
                [10.9, 2.85, 8.5],
                uv_up=[10.5, 2.0, 12.5, 4.0],
                uv_down=[10.5, 4.0, 12.5, 6.0],
                uv_side=[8.5, 2.0, 15.5, 4.0],
                uv_end=[10.5, 2.0, 12.5, 4.0],
                rotation={"angle": 22.5, "axis": "x", "origin": [10.5, 2.45, 7.3]},
            ),
            make_element(
                "band_right_front",
                [10.4, 1.85, 4.5],
                [11.2, 2.65, 6.4],
                uv_up=[10.5, 4.0, 12.5, 6.0],
                uv_down=[10.5, 6.0, 12.5, 8.0],
                uv_side=[8.5, 4.0, 15.5, 6.0],
                uv_end=[10.5, 4.0, 12.5, 6.0],
                rotation={"angle": -22.5, "axis": "x", "origin": [10.8, 2.25, 5.45]},
            ),
            # Left hollow triangular D-handle (outer strap arm + inner strap arm + foam crossbar)
            make_element(
                "handle_left_strap_outer",
                [3.85, 1.55, 2.9],
                [4.4, 2.2, 4.8],
                uv_up=[0.5, 12.2, 3.5, 15.8],
                uv_down=[0.5, 12.2, 3.5, 15.8],
                uv_side=[0.5, 12.5, 6.5, 14.5],
                rotation={"angle": -22.5, "axis": "y", "origin": [4.1, 1.85, 3.85]},
            ),
            make_element(
                "handle_left_strap_inner",
                [6.0, 1.55, 2.9],
                [6.55, 2.2, 4.8],
                uv_up=[0.5, 12.2, 3.5, 15.8],
                uv_down=[0.5, 12.2, 3.5, 15.8],
                uv_side=[0.5, 12.5, 6.5, 14.5],
                rotation={"angle": 22.5, "axis": "y", "origin": [6.3, 1.85, 3.85]},
            ),
            make_element(
                "handle_left_foam_grip",
                [3.4, 1.45, 2.3],
                [7.0, 2.45, 3.3],
                uv_up=[0.0, 8.0, 8.0, 12.0],
                uv_down=[0.0, 8.0, 8.0, 12.0],
                uv_side=[0.0, 8.0, 8.0, 12.0],
                uv_end=[1.0, 8.5, 3.5, 11.5],
                rotation={"angle": 45, "axis": "x", "origin": [5.2, 1.95, 2.8]},
            ),
            # Right hollow triangular D-handle (inner strap arm + outer strap arm + foam crossbar)
            make_element(
                "handle_right_strap_inner",
                [9.45, 1.55, 2.9],
                [10.0, 2.2, 4.8],
                uv_up=[4.0, 12.2, 7.5, 15.8],
                uv_down=[4.0, 12.2, 7.5, 15.8],
                uv_side=[1.5, 12.5, 7.5, 14.5],
                rotation={"angle": -22.5, "axis": "y", "origin": [9.7, 1.85, 3.85]},
            ),
            make_element(
                "handle_right_strap_outer",
                [11.6, 1.55, 2.9],
                [12.15, 2.2, 4.8],
                uv_up=[4.0, 12.2, 7.5, 15.8],
                uv_down=[4.0, 12.2, 7.5, 15.8],
                uv_side=[1.5, 12.5, 7.5, 14.5],
                rotation={"angle": 22.5, "axis": "y", "origin": [11.9, 1.85, 3.85]},
            ),
            make_element(
                "handle_right_foam_grip",
                [9.0, 1.45, 2.3],
                [12.6, 2.45, 3.3],
                uv_up=[0.0, 8.0, 8.0, 12.0],
                uv_down=[0.0, 8.0, 8.0, 12.0],
                uv_side=[0.0, 8.0, 8.0, 12.0],
                uv_end=[1.0, 8.5, 3.5, 11.5],
                rotation={"angle": 45, "axis": "x", "origin": [10.8, 1.95, 2.8]},
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
    print(
        "Generated 3 opaque 64x64 block textures and 3 high-detail 3D station block models."
    )


if __name__ == "__main__":
    main()
