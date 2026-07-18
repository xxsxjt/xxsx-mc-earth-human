#!/usr/bin/env python3
"""Render deterministic Earth Human UI previews without launching Minecraft."""

from __future__ import annotations

import json
import math
import pathlib
from typing import Callable

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont


ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "tmp" / "ui-audit-v2"
LANG_PATH = ROOT / "src" / "main" / "resources" / "assets" / "earth_human" / "lang" / "zh_cn.json"
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "earth_human" / "textures" / "item"

WIDTH = 320
HEIGHT = 240
SCALE = 2
PANEL_LEFT = 5
PANEL_TOP = 6
PANEL_WIDTH = 310
PANEL_HEIGHT = 228
CONTENT_LEFT = 21
CONTENT_RIGHT = 301
CONTENT_TOP = 71

BACKDROP = (16, 19, 20, 192)
FRAME = (17, 23, 25, 255)
SURFACE = (27, 35, 37, 255)
SURFACE_ALT = (36, 46, 48, 255)
EDGE = (102, 118, 115, 255)
ACCENT = (123, 182, 159, 255)
ACCENT_DARK = (56, 91, 80, 255)
MEDICAL_WHITE = (240, 242, 236, 255)
MUTED = (179, 191, 186, 255)
DANGER = (216, 90, 86, 255)
WARNING = (213, 164, 79, 255)


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        pathlib.Path(r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc"),
        pathlib.Path(r"C:\Windows\Fonts\simhei.ttf"),
        pathlib.Path(r"C:\Windows\Fonts\simsun.ttc"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


FONT = load_font(9)
FONT_SMALL = load_font(8)
FONT_BOLD = load_font(9, bold=True)
LANG = json.loads(LANG_PATH.read_text(encoding="utf-8-sig"))


def tr(key: str, *args: object) -> str:
    value = LANG.get(key, key)
    for arg in args:
        value = value.replace("%s", str(arg), 1)
    return value.replace("%%", "%")


def text_width(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont = FONT) -> int:
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0]


def fit(draw: ImageDraw.ImageDraw, text: str, width: int, font: ImageFont.ImageFont = FONT) -> str:
    if text_width(draw, text, font) <= width:
        return text
    suffix = "..."
    current = text
    while current and text_width(draw, current + suffix, font) > width:
        current = current[:-1]
    return current + suffix


def world_background() -> Image.Image:
    image = Image.new("RGBA", (WIDTH, HEIGHT), (117, 177, 206, 255))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, WIDTH, 88), fill=(119, 181, 211, 255))
    for x, y, w in [(18, 24, 42), (126, 16, 54), (238, 30, 45)]:
        draw.rectangle((x, y, x + w, y + 7), fill=(226, 238, 236, 235))
        draw.rectangle((x + 8, y - 4, x + w - 10, y + 7), fill=(226, 238, 236, 235))
    draw.polygon([(0, 108), (46, 73), (92, 103), (145, 62), (199, 106), (263, 72), (320, 102), (320, 178), (0, 178)],
                 fill=(77, 119, 76, 255))
    draw.polygon([(0, 133), (58, 96), (111, 124), (165, 87), (222, 124), (279, 91), (320, 120), (320, 190), (0, 190)],
                 fill=(94, 139, 78, 255))
    draw.rectangle((0, 151, WIDTH, HEIGHT), fill=(89, 125, 61, 255))
    for x in range(0, WIDTH, 16):
        shade = (78 + (x // 16) % 3 * 7, 111 + (x // 16) % 2 * 8, 55, 255)
        draw.rectangle((x, 151 + (x // 16) % 2 * 4, x + 15, HEIGHT), fill=shade)
    draw.rectangle((242, 112, 271, 151), fill=(104, 78, 51, 255))
    draw.rectangle((238, 102, 276, 117), fill=(54, 110, 52, 255))
    return image


def draw_button(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], label: str, active: bool = True) -> None:
    x, y, w, h = box
    fill = (83, 85, 84, 255) if active else (47, 52, 52, 255)
    draw.rectangle((x, y, x + w - 1, y + h - 1), fill=fill, outline=(13, 14, 14, 255))
    draw.line((x + 1, y + 1, x + w - 2, y + 1), fill=(169, 171, 167, 255))
    color = MEDICAL_WHITE if active else MUTED
    label = fit(draw, label, w - 8, FONT_SMALL)
    tw = text_width(draw, label, FONT_SMALL)
    draw.text((x + (w - tw) // 2, y + max(1, (h - 9) // 2)), label, font=FONT_SMALL, fill=color)


def draw_header(draw: ImageDraw.ImageDraw, active_tab: int) -> None:
    draw.rectangle((PANEL_LEFT, PANEL_TOP, PANEL_LEFT + PANEL_WIDTH - 1, PANEL_TOP + PANEL_HEIGHT - 1), fill=FRAME, outline=EDGE)
    draw.rectangle((PANEL_LEFT + 3, PANEL_TOP + 3, PANEL_LEFT + PANEL_WIDTH - 4, PANEL_TOP + 32), fill=SURFACE_ALT)
    draw.rectangle((PANEL_LEFT + 3, PANEL_TOP + 33, PANEL_LEFT + 6, PANEL_TOP + PANEL_HEIGHT - 5), fill=ACCENT_DARK)
    draw.rectangle((PANEL_LEFT + 7, PANEL_TOP + 60, PANEL_LEFT + PANEL_WIDTH - 4, PANEL_TOP + PANEL_HEIGHT - 35), fill=SURFACE)
    for y in range(PANEL_TOP + 71, PANEL_TOP + PANEL_HEIGHT - 35, 12):
        draw.line((PANEL_LEFT + 8, y, PANEL_LEFT + PANEL_WIDTH - 4, y), fill=(43, 61, 58, 100))
    draw.rectangle((PANEL_LEFT + 7, PANEL_TOP + 58, PANEL_LEFT + PANEL_WIDTH - 4, PANEL_TOP + 59), fill=ACCENT)
    mx, my = PANEL_LEFT + 13, PANEL_TOP + 10
    draw.rectangle((mx, my, mx + 16, my + 16), fill=ACCENT_DARK, outline=ACCENT)
    draw.rectangle((mx + 7, my + 3, mx + 9, my + 13), fill=MEDICAL_WHITE)
    draw.rectangle((mx + 3, my + 7, mx + 13, my + 9), fill=MEDICAL_WHITE)
    draw.text((PANEL_LEFT + 36, PANEL_TOP + 9), tr("screen.earth_human.status.title"), font=FONT_BOLD, fill=MEDICAL_WHITE)
    subtitle = fit(draw, tr("screen.earth_human.status.subtitle"), 80, FONT_SMALL)
    draw.text((PANEL_LEFT + 192, PANEL_TOP + 11), subtitle, font=FONT_SMALL, fill=MUTED)

    labels = [
        tr("screen.earth_human.status.tab.overview"),
        tr("screen.earth_human.status.tab.body"),
        tr("screen.earth_human.status.tab.care"),
        tr("screen.earth_human.status.tab.settings"),
    ]
    tab_width = 67
    for index, label in enumerate(labels):
        draw_button(draw, (PANEL_LEFT + 14 + index * 71, PANEL_TOP + 36, tab_width, 20), label, index != active_tab)


def draw_heading(draw: ImageDraw.ImageDraw, key: str) -> None:
    draw.rectangle((CONTENT_LEFT, CONTENT_TOP + 10, CONTENT_LEFT + 9, CONTENT_TOP + 11), fill=ACCENT)
    draw.text((CONTENT_LEFT + 15, CONTENT_TOP + 6), fit(draw, tr(key), 264), font=FONT_BOLD, fill=MEDICAL_WHITE)


def draw_meter(draw: ImageDraw.ImageDraw, x: int, y: int, width: int, label: str, ratio: float, value: str, color: tuple[int, int, int, int]) -> None:
    draw.text((x, y), label, font=FONT_SMALL, fill=MUTED)
    tw = text_width(draw, value, FONT_SMALL)
    draw.text((x + width - tw, y), value, font=FONT_SMALL, fill=MEDICAL_WHITE)
    draw.rectangle((x, y + 11, x + width - 1, y + 17), fill=(17, 23, 25, 255), outline=(70, 81, 79, 255))
    filled = max(0, round((width - 2) * max(0.0, min(1.0, ratio))))
    if filled:
        draw.rectangle((x + 1, y + 12, x + filled, y + 16), fill=color)
    for marker in range(1, 4):
        marker_x = x + width * marker // 4
        draw.line((marker_x, y + 12, marker_x, y + 16), fill=(50, 59, 58, 130))


def body_color(ratio: float) -> tuple[int, int, int, int]:
    if ratio <= 0.001:
        return (5, 7, 7, 255)
    red = round(214 - ratio * 132)
    green = round(60 + ratio * 132)
    blue = round(54 + ratio * 58)
    return red, green, blue, 255


def draw_body_figure(draw: ImageDraw.ImageDraw, x: int, y: int, scale: float = 1.0, values: list[float] | None = None) -> None:
    values = values or [1.0] * 3
    if len(values) < 3:
        values = (values + [1.0] * 3)[:3]
    width = round(38 * scale)
    height = round(60 * scale)
    draw.rectangle((x - 2, y - 2, x + width + 1, y + height + 1), fill=(16, 22, 23, 205), outline=(92, 108, 103, 210))
    marks = [(0, 0), (width - 4, 0), (0, height - 1), (width - 4, height - 1)]
    for mx, my in marks:
        draw.line((x + mx, y + my, x + mx + 3, y + my), fill=ACCENT)

    def segment(part: int, px: int, py: int, pw: int, ph: int) -> None:
        sx, sy = x + round(px * scale), y + round(py * scale)
        sw, sh = max(1, round(pw * scale)), max(1, round(ph * scale))
        draw.rectangle((sx - 1, sy - 1, sx + sw, sy + sh), fill=(7, 16, 14, 255))
        draw.rectangle((sx, sy, sx + sw - 1, sy + sh - 1), fill=body_color(values[part]))

    segment(0, 13, 2, 12, 11)
    segment(1, 10, 15, 18, 24)
    for args in [(4, 16, 6, 28), (28, 16, 6, 28), (11, 41, 7, 18), (20, 41, 7, 18)]:
        px, py, pw, ph = args
        segment(2, px, py, pw, ph)


def draw_footer(draw: ImageDraw.ImageDraw, tab: int) -> None:
    if tab == 0:
        draw_button(draw, (CONTENT_LEFT, PANEL_TOP + PANEL_HEIGHT - 27, 78, 18), tr("screen.earth_human.status.open_body"))
        draw_button(draw, (CONTENT_LEFT + 84, PANEL_TOP + PANEL_HEIGHT - 27, 78, 18), tr("screen.earth_human.status.open_care"))
    elif tab == 2:
        draw_button(draw, (PANEL_LEFT + PANEL_WIDTH - 158, PANEL_TOP + PANEL_HEIGHT - 27, 80, 18), tr("screen.earth_human.status.care.cancel"), False)
    draw_button(draw, (PANEL_LEFT + PANEL_WIDTH - 72, PANEL_TOP + PANEL_HEIGHT - 27, 60, 18), tr("screen.earth_human.status.close"))


def screen_base(tab: int) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    base = ImageEnhance.Brightness(world_background().filter(ImageFilter.GaussianBlur(2.2))).enhance(0.48)
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), BACKDROP)
    base.alpha_composite(overlay)
    draw = ImageDraw.Draw(base)
    draw_header(draw, tab)
    return base, draw


def render_overview() -> Image.Image:
    image, draw = screen_base(0)
    draw_heading(draw, "screen.earth_human.status.overview.heading")
    meter_width = 210
    meter_step = 19
    draw_meter(draw, CONTENT_LEFT, CONTENT_TOP + 17, meter_width, tr("screen.earth_human.status.hydration"), 0.84, "84%", ACCENT)
    draw_meter(draw, CONTENT_LEFT, CONTENT_TOP + 17 + meter_step, meter_width, tr("screen.earth_human.status.temperature"), 0.48, "37.0 C", ACCENT)
    draw_meter(draw, CONTENT_LEFT, CONTENT_TOP + 17 + meter_step * 2, meter_width, tr("screen.earth_human.status.fatigue"), 0.38, "38%", WARNING)
    draw_meter(draw, CONTENT_LEFT, CONTENT_TOP + 17 + meter_step * 3, meter_width, tr("screen.earth_human.status.air_quality"), 0.76, "76%", ACCENT)
    body_x = CONTENT_LEFT + meter_width + 12
    draw.rectangle((body_x, CONTENT_TOP + 15, body_x + 57, CONTENT_TOP + 98), fill=(20, 26, 28, 210), outline=ACCENT_DARK)
    label = tr("screen.earth_human.status.body_summary")
    draw.text((body_x + (58 - text_width(draw, label, FONT_SMALL)) // 2, CONTENT_TOP + 20), label, font=FONT_SMALL, fill=MUTED)
    draw_body_figure(draw, body_x + 14, CONTENT_TOP + 33, 0.82, [0.95, 0.82, 0.78])
    activity = tr("screen.earth_human.status.activity_line", tr("activity.earth_human.walking"), 46)
    draw.text((CONTENT_LEFT, CONTENT_TOP + 94), fit(draw, activity, meter_width), font=FONT_SMALL, fill=MEDICAL_WHITE)
    draw.rectangle((CONTENT_LEFT, CONTENT_TOP + 106, CONTENT_LEFT + meter_width - 1, CONTENT_TOP + 109), fill=(17, 23, 25, 255))
    draw.rectangle((CONTENT_LEFT + 1, CONTENT_TOP + 107, CONTENT_LEFT + 96, CONTENT_TOP + 108), fill=WARNING)
    note_y = CONTENT_TOP + 112
    draw.rectangle((CONTENT_LEFT, note_y, CONTENT_RIGHT, note_y + 18), fill=(33, 43, 44, 225))
    draw.rectangle((CONTENT_LEFT, note_y, CONTENT_LEFT + 3, note_y + 18), fill=ACCENT)
    draw.text((CONTENT_LEFT + 9, note_y + 5), tr("screen.earth_human.status.recommendation"), font=FONT_SMALL, fill=ACCENT)
    note = tr("screen.earth_human.status.recommend.exertion")
    draw.text((CONTENT_LEFT + 82, note_y + 5), fit(draw, note, 198, FONT_SMALL), font=FONT_SMALL, fill=MEDICAL_WHITE)
    draw_footer(draw, 0)
    return image


def render_body() -> Image.Image:
    image, draw = screen_base(1)
    draw_heading(draw, "screen.earth_human.status.body.heading")
    figure_width = 74
    figure_height = 114
    draw.rectangle((CONTENT_LEFT, CONTENT_TOP + 20, CONTENT_LEFT + figure_width - 1, CONTENT_TOP + 20 + figure_height - 1),
                   fill=(20, 26, 28, 215), outline=ACCENT_DARK)
    draw_body_figure(draw, CONTENT_LEFT + 11, CONTENT_TOP + 26, 1.35, [0.63, 0.78, 0.44])
    parts = ["头部", "躯干", "四肢"]
    values = [0.63, 0.78, 0.44]
    bars_x = CONTENT_LEFT + figure_width + 12
    for index, (part, value) in enumerate(zip(parts, values)):
        draw_meter(draw, bars_x, CONTENT_TOP + 18 + index * 18, CONTENT_RIGHT - bars_x,
                   part, value, f"{round(value * 100)}%", body_color(value))
    draw_footer(draw, 1)
    return image


CARE_ITEMS = {
    "hydrate": "filled_canteen",
    "bandage": "sterile_bandage",
    "first_aid": "basic_first_aid_kit",
    "warm": "heat_pack",
    "cool": "cooling_pack",
}


def load_icon(item_id: str, size: int = 16) -> Image.Image:
    path = TEXTURES / f"{item_id}.png"
    with Image.open(path) as opened:
        return opened.convert("RGBA").resize((size, size), Image.Resampling.NEAREST)


def render_care() -> Image.Image:
    image, draw = screen_base(2)
    draw_heading(draw, "screen.earth_human.status.care.heading")
    actions = [
        ("hydrate", tr("care.earth_human.hydrate"), "装满的水壶 · 1.6 秒", True),
        ("bandage", tr("care.earth_human.bandage"), "无菌绷带 · 2.0 秒", True),
        ("first_aid", tr("care.earth_human.first_aid"), "基础急救包 · 3.0 秒", True),
        ("warm", tr("care.earth_human.warm"), "缺少匹配物品", False),
        ("cool", tr("care.earth_human.cool"), "降温包 · 1.8 秒", True),
    ]
    for index, (action_id, title, detail, available) in enumerate(actions):
        row_y = CONTENT_TOP + 27 + index * 20
        draw.rectangle((CONTENT_LEFT, row_y, CONTENT_RIGHT, row_y + 18), fill=(27, 36, 38, 230))
        draw.rectangle((CONTENT_LEFT, row_y, CONTENT_LEFT + 2, row_y + 18), fill=ACCENT_DARK if available else (85, 93, 92, 255))
        draw.rectangle((CONTENT_LEFT + 6, row_y + 1, CONTENT_LEFT + 23, row_y + 18), fill=(17, 23, 25, 255), outline=EDGE)
        image.alpha_composite(load_icon(CARE_ITEMS[action_id]), (CONTENT_LEFT + 7, row_y + 2))
        if not available:
            draw.rectangle((CONTENT_LEFT + 7, row_y + 2, CONTENT_LEFT + 22, row_y + 17), fill=(0, 0, 0, 95))
        draw.text((CONTENT_LEFT + 29, row_y + 2), fit(draw, title, 145, FONT_SMALL), font=FONT_SMALL,
                  fill=MEDICAL_WHITE if available else MUTED)
        draw.text((CONTENT_LEFT + 29, row_y + 11), fit(draw, detail, 145, FONT_SMALL), font=FONT_SMALL,
                  fill=ACCENT if available else WARNING)
        draw_button(draw, (CONTENT_RIGHT - 67, row_y + 2, 62, 16), tr("screen.earth_human.status.care.start"), available)
    draw_footer(draw, 2)
    return image


def draw_checkbox(draw: ImageDraw.ImageDraw, x: int, y: int, label: str, checked: bool) -> None:
    draw.rectangle((x, y, x + 16, y + 16), fill=(44, 48, 48, 255), outline=(190, 192, 187, 255))
    if checked:
        draw.line((x + 4, y + 8, x + 7, y + 12), fill=ACCENT, width=2)
        draw.line((x + 7, y + 12, x + 13, y + 4), fill=ACCENT, width=2)
    draw.text((x + 21, y + 4), fit(draw, label, 108, FONT_SMALL), font=FONT_SMALL, fill=MEDICAL_WHITE)


def draw_slider(draw: ImageDraw.ImageDraw, x: int, y: int, width: int, label: str, ratio: float) -> None:
    draw.rectangle((x, y, x + width - 1, y + 19), fill=(70, 72, 71, 255), outline=(13, 14, 14, 255))
    handle_x = x + round(ratio * (width - 8))
    draw.rectangle((handle_x, y + 1, handle_x + 7, y + 18), fill=(162, 164, 160, 255), outline=(30, 31, 31, 255))
    label = fit(draw, label, width - 14, FONT_SMALL)
    draw.text((x + (width - text_width(draw, label, FONT_SMALL)) // 2, y + 5), label, font=FONT_SMALL, fill=MEDICAL_WHITE)


def render_settings() -> Image.Image:
    image, draw = screen_base(3)
    draw_heading(draw, "screen.earth_human.status.settings.heading")
    x = CONTENT_LEFT
    y = CONTENT_TOP + 19
    column_width = 135
    draw_checkbox(draw, x, y, tr("screen.earth_human.settings.vitals"), True)
    draw_checkbox(draw, x + 145, y, tr("screen.earth_human.settings.body"), True)
    draw_checkbox(draw, x, y + 25, tr("screen.earth_human.settings.numbers"), True)
    draw_button(draw, (x + 145, y + 22, column_width, 20), "识别配色：野外绿")
    draw_button(draw, (x, y + 51, 280, 20), "身体图位置：左下角")
    draw_slider(draw, x, y + 78, column_width, "身体图大小：100%", 0.29)
    draw_slider(draw, x + 145, y + 78, column_width, "身体图透明度：50%", 0.44)
    draw_footer(draw, 3)
    return image


def draw_vanilla_hud(draw: ImageDraw.ImageDraw) -> None:
    hotbar_x = 69
    hotbar_y = 218
    for slot in range(9):
        x = hotbar_x + slot * 20
        draw.rectangle((x, hotbar_y, x + 19, hotbar_y + 19), fill=(55, 55, 50, 190), outline=(20, 20, 18, 230))
    draw.rectangle((hotbar_x, hotbar_y, hotbar_x + 19, hotbar_y + 19), outline=(235, 235, 225, 255), width=2)
    for index in range(10):
        hx = 71 + index * 8
        draw.rectangle((hx, 205, hx + 5, 209), fill=(185, 34, 42, 255))
        fx = 244 + index * 7
        draw.rectangle((fx, 205, fx + 4, 210), fill=(169, 114, 55, 255))


def render_hud() -> Image.Image:
    image = world_background()
    draw = ImageDraw.Draw(image)
    draw_vanilla_hud(draw)
    draw_body_figure(draw, 6, HEIGHT - 60 - 6, 1.0, [0.82, 0.67, 0.44])
    x = WIDTH // 2 + 10
    y = 191
    widths = [42, 48, 47, 45]
    labels = ["水分 84", "体温 37.0", "疲劳 38", "空气 76"]
    ratios = [0.84, 0.48, 0.38, 0.76]
    colors = [ACCENT, ACCENT, WARNING, ACCENT]
    compact = WIDTH - x < sum(widths) + 14
    first_row = sum(widths[:3]) + 4
    total_width = first_row if compact else sum(widths) + 6
    draw.rectangle((x - 2, y - 1, x + total_width + 2, y + (26 if compact else 11)),
                   fill=(20, 26, 28, 215), outline=(82, 97, 93, 190))
    offset = 0
    for index, width in enumerate(widths):
        if compact and index == 3:
            cell_x, cell_y = x, y + 14
            offset = 0
        else:
            cell_x, cell_y = x + offset, y
        draw.text((cell_x + 1, cell_y), fit(draw, labels[index], width - 2, FONT_SMALL), font=FONT_SMALL, fill=MEDICAL_WHITE)
        draw.rectangle((cell_x, cell_y + 8, cell_x + width - 1, cell_y + 9), fill=(39, 48, 47, 255))
        fill = round(width * ratios[index])
        draw.rectangle((cell_x, cell_y + 8, cell_x + max(1, fill) - 1, cell_y + 9), fill=colors[index])
        if not (compact and index == 3):
            offset += width + 2
    return image


def render_inventory() -> Image.Image:
    """Preview the normal body HUD kept clickable while the inventory is open."""
    image = ImageEnhance.Brightness(world_background().filter(ImageFilter.GaussianBlur(2))).enhance(0.55)
    draw = ImageDraw.Draw(image)
    left, top, width, height = 42, 36, 176, 166
    draw.rectangle((left, top, left + width, top + height), fill=(198, 198, 190, 255), outline=(35, 35, 34, 255))
    draw.text((left + 8, top + 7), "玩家背包", font=FONT_BOLD, fill=(52, 53, 50, 255))
    # The real InventoryScreen still renders Minecraft's own player model here.
    # Keep this preview neutral so it cannot be mistaken for Earth Human replacing it.
    draw.rectangle((left + 8, top + 23, left + 70, top + 78), fill=(153, 153, 147, 255), outline=(73, 73, 68, 255))
    draw.ellipse((left + 33, top + 30, left + 45, top + 42), fill=(177, 143, 111, 255), outline=(79, 67, 58, 255))
    draw.rectangle((left + 29, top + 42, left + 49, top + 65), fill=(74, 103, 133, 255), outline=(53, 64, 75, 255))
    draw.rectangle((left + 24, top + 44, left + 29, top + 65), fill=(177, 143, 111, 255), outline=(79, 67, 58, 255))
    draw.rectangle((left + 49, top + 44, left + 54, top + 65), fill=(177, 143, 111, 255), outline=(79, 67, 58, 255))
    draw.rectangle((left + 30, top + 65, left + 38, top + 77), fill=(48, 56, 78, 255), outline=(42, 44, 55, 255))
    draw.rectangle((left + 40, top + 65, left + 48, top + 77), fill=(48, 56, 78, 255), outline=(42, 44, 55, 255))
    draw.text((left + 12, top + 82), "MC 原版玩家模型", font=FONT_SMALL, fill=(83, 84, 79, 255))
    for row in range(3):
        for column in range(2):
            x = left + 82 + column * 19
            y = top + 28 + row * 19
            draw.rectangle((x, y, x + 17, y + 17), fill=(117, 117, 111, 255), outline=(55, 55, 52, 255))
    for row in range(3):
        for column in range(9):
            x = left + 8 + column * 18
            y = top + 91 + row * 18
            draw.rectangle((x, y, x + 16, y + 16), fill=(117, 117, 111, 255), outline=(55, 55, 52, 255))

    values = [0.86, 0.72, 0.45]
    hover_x, hover_y = 6, HEIGHT - 60 - 6
    draw_body_figure(draw, hover_x, hover_y, 1.0, values)
    draw.rectangle((hover_x - 3, hover_y - 3, hover_x + 40, hover_y + 62), outline=ACCENT)

    tooltip_x, tooltip_y = hover_x + 45, hover_y - 4
    tooltip_width, tooltip_height = 148, 61
    draw.rectangle((tooltip_x, tooltip_y, tooltip_x + tooltip_width, tooltip_y + tooltip_height),
                   fill=(22, 22, 22, 245), outline=(218, 218, 210, 255))
    draw.text((tooltip_x + 5, tooltip_y + 4), tr("screen.earth_human.inventory.summary"),
              font=FONT_BOLD, fill=MEDICAL_WHITE)
    for index, (part, value) in enumerate(zip(["head", "torso", "limbs"], values)):
        line = tr("screen.earth_human.inventory.part_line",
                  tr(f"bodypart.earth_human.{part}"), round(value * 100))
        draw.text((tooltip_x + 5, tooltip_y + 16 + index * 10), line,
                  font=FONT_SMALL, fill=body_color(value))
    draw.text((tooltip_x + 5, tooltip_y + 48), tr("screen.earth_human.inventory.click_hint"),
              font=FONT_SMALL, fill=ACCENT)
    draw.polygon([(hover_x, hover_y), (hover_x + 5, hover_y + 9), (hover_x + 3, hover_y + 4)], fill=(245, 245, 240, 255))
    return image


def save_preview(name: str, render: Callable[[], Image.Image]) -> pathlib.Path:
    image = render()
    output = OUT / f"{name}.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    image.resize((WIDTH * SCALE, HEIGHT * SCALE), Image.Resampling.NEAREST).save(output)
    return output


def make_contact_sheet(paths: list[pathlib.Path]) -> pathlib.Path:
    columns = 3
    thumb_width = 320
    thumb_height = 240
    rows = math.ceil(len(paths) / columns)
    sheet = Image.new("RGBA", (columns * thumb_width, rows * (thumb_height + 22)), (19, 23, 24, 255))
    draw = ImageDraw.Draw(sheet)
    for index, path in enumerate(paths):
        with Image.open(path) as opened:
            thumb = opened.convert("RGBA").resize((thumb_width, thumb_height), Image.Resampling.LANCZOS)
        x = (index % columns) * thumb_width
        y = (index // columns) * (thumb_height + 22)
        sheet.alpha_composite(thumb, (x, y))
        draw.text((x + 6, y + thumb_height + 5), path.stem, font=FONT_SMALL, fill=MEDICAL_WHITE)
    output = OUT / "ui-contact-sheet.png"
    sheet.save(output)
    return output


def main() -> None:
    previews = [
        save_preview("overview", render_overview),
        save_preview("body", render_body),
        save_preview("care", render_care),
        save_preview("settings", render_settings),
        save_preview("hud", render_hud),
        save_preview("inventory", render_inventory),
    ]
    contact = make_contact_sheet(previews)
    for path in previews:
        print(path)
    print(contact)


if __name__ == "__main__":
    main()
