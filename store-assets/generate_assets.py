"""
Generates the Play Console store listing graphics for Retro Pocket Pinball:

    icon-512.png                 512x512  hi-res app icon (opaque, no alpha)
    feature-graphic-1024x500.png 1024x500 store listing feature graphic

Everything is drawn procedurally (no external art assets) using the same
palette and "chrome ball + reactor ring + flippers" motif as the in-app
adaptive icon (app/src/main/res/drawable/ic_launcher_*.xml), so the Play
Store listing matches the app itself.

Requires: Pillow, numpy (both already available in this project's Python).

Usage:
    python generate_assets.py

Edit the CONFIG section below (colors, tagline, title) and re-run any time
you want to tweak the look — everything else regenerates from scratch.
"""

from __future__ import annotations

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# ---------------------------------------------------------------------------
# Config — tweak freely and re-run.
# ---------------------------------------------------------------------------

TITLE_LINE_1 = "RETRO POCKET"
TITLE_LINE_2 = "PINBALL"
TAGLINE = "CLASSIC ARCADE PINBALL"

BG_TOP = (17, 22, 52)          # dark navy, matching the in-game "void" backdrop
BG_MID = (9, 12, 30)
BG_BOTTOM = (3, 4, 12)
KEY_LIGHT = (111, 140, 255)    # cool cyan-blue key light, upper-left

RING_TOP = (77, 225, 255)      # #4DE1FF neon cyan
RING_MID = (179, 136, 255)     # #B388FF neon violet
RING_BOTTOM = (255, 77, 157)   # #FF4D9D neon magenta

BALL_HIGHLIGHT = (255, 255, 255)
BALL_MID = (211, 222, 236)     # #D3DEEC
BALL_SHADE = (126, 140, 166)   # #7E8CA6
BALL_DARK = (42, 51, 70)       # #2A3346

FLIPPER_COLOR = (255, 194, 61)  # #FFC23D neon amber

FONT_BOLD = r"C:\Windows\Fonts\consolab.ttf"   # Consolas Bold: matches the in-app monospace look
FONT_FALLBACK = r"C:\Windows\Fonts\arialbd.ttf"

SUPERSAMPLE = 4  # render at N x target resolution, then downsample for antialiasing


# ---------------------------------------------------------------------------
# Small gradient / compositing helpers (all numpy, operate on float RGB 0..255)
# ---------------------------------------------------------------------------

def _lerp(a, b, t):
    t = np.clip(t, 0.0, 1.0)
    return a + (b - a) * t[..., None]


def linear_gradient(w, h, p0, p1, stops):
    """stops: list of (t, (r,g,b)) with t in [0,1] ascending."""
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    length_sq = dx * dx + dy * dy
    t = ((xs - p0[0]) * dx + (ys - p0[1]) * dy) / length_sq
    t = np.clip(t, 0.0, 1.0)
    out = np.zeros((h, w, 3), dtype=np.float32)
    for (t0, c0), (t1, c1) in zip(stops[:-1], stops[1:]):
        seg = (t - t0) / max(t1 - t0, 1e-6)
        seg_color = _lerp(np.array(c0, dtype=np.float32), np.array(c1, dtype=np.float32), seg)
        mask = (t >= t0) & (t <= t1)
        out[mask] = seg_color[mask]
    return out


def radial_falloff(w, h, cx, cy, r0, r1):
    """1 at distance<=r0, smoothly fading to 0 at distance>=r1."""
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2)
    t = np.clip((r1 - d) / max(r1 - r0, 1e-6), 0.0, 1.0)
    return t * t * (3 - 2 * t)  # smoothstep


def add_glow(base_rgb, w, h, cx, cy, r0, r1, color, strength):
    """Additive glow: brightens toward `color` within the falloff, capped by `strength`."""
    a = radial_falloff(w, h, cx, cy, r0, r1) * strength
    return base_rgb + np.array(color, dtype=np.float32)[None, None, :] * a[..., None]


def vignette(base_rgb, w, h, cx, cy, r0, r1, strength=0.55):
    a = 1.0 - radial_falloff(w, h, cx, cy, r0, r1)
    return base_rgb * (1.0 - a[..., None] * strength)


def to_image(rgb_float):
    return Image.fromarray(np.clip(rgb_float, 0, 255).astype(np.uint8), mode="RGB")


def load_font(size):
    try:
        return ImageFont.truetype(FONT_BOLD, size)
    except OSError:
        return ImageFont.truetype(FONT_FALLBACK, size)


# ---------------------------------------------------------------------------
# Shared motif: dark cabinet background + reactor ring + chrome ball + flippers
# ---------------------------------------------------------------------------

def draw_backdrop(w, h, key_x_frac=0.28, key_y_frac=0.22):
    bg = linear_gradient(w, h, (0, 0), (w, h), [(0.0, BG_TOP), (0.5, BG_MID), (1.0, BG_BOTTOM)])
    bg = add_glow(bg, w, h, w * key_x_frac, h * key_y_frac, 0, max(w, h) * 0.85, KEY_LIGHT, 0.16)
    bg = vignette(bg, w, h, w * 0.5, h * 0.54, min(w, h) * 0.12, min(w, h) * 0.75, strength=0.60)
    return bg


def draw_reactor_ball(canvas_rgb, w, h, cx, cy, ring_outer, ring_inner, ball_r):
    """Composites the ring + chrome ball motif onto canvas_rgb (numpy float HxWx3) in place-ish, returns new array."""
    out = canvas_rgb.copy()

    # --- soft glow behind the ring, additive ---
    out = add_glow(out, w, h, cx, cy, ring_inner * 0.6, ring_outer * 2.6, RING_MID, 0.35)

    # --- ring: annulus with a vertical cyan -> violet -> magenta gradient ---
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    dist = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2)
    feather = max(w, h) * 0.0025
    outer_mask = np.clip((ring_outer - dist) / feather, 0.0, 1.0)
    inner_mask = np.clip((dist - ring_inner) / feather, 0.0, 1.0)
    ring_alpha = np.clip(outer_mask, 0, 1) * np.clip(inner_mask, 0, 1)
    ring_t = np.clip((ys - (cy - ring_outer)) / (2 * ring_outer), 0.0, 1.0)
    upper = ring_t < 0.5
    seg_top = _lerp(np.array(RING_TOP, dtype=np.float32), np.array(RING_MID, dtype=np.float32), ring_t * 2)
    seg_bottom = _lerp(np.array(RING_MID, dtype=np.float32), np.array(RING_BOTTOM, dtype=np.float32), (ring_t - 0.5) * 2)
    ring_color = np.where(upper[..., None], seg_top, seg_bottom)
    out = out * (1 - ring_alpha[..., None]) + ring_color * ring_alpha[..., None]

    # --- ball: soft contact shadow, then radial-gradient chrome sphere ---
    shadow_off = ball_r * 0.22
    shadow_a = radial_falloff(w, h, cx + shadow_off, cy + shadow_off, ball_r * 0.3, ball_r * 1.55) * 0.55
    out = out * (1 - shadow_a[..., None])

    hl_x, hl_y = cx - ball_r * 0.32, cy - ball_r * 0.32
    ball_mask = np.clip((ball_r - dist) / feather, 0.0, 1.0)
    hd = np.sqrt((xs - hl_x) ** 2 + (ys - hl_y) ** 2) / (ball_r * 1.5)
    hd = np.clip(hd, 0.0, 1.0)
    seg1 = hd < 0.35
    seg2 = hd < 0.7  # (>=0.35 handled by precedence in the nested where below)
    seg1c = _lerp(np.array(BALL_HIGHLIGHT, dtype=np.float32), np.array(BALL_MID, dtype=np.float32), hd / 0.35)
    seg2c = _lerp(np.array(BALL_MID, dtype=np.float32), np.array(BALL_SHADE, dtype=np.float32), (hd - 0.35) / 0.35)
    seg3c = _lerp(np.array(BALL_SHADE, dtype=np.float32), np.array(BALL_DARK, dtype=np.float32), (hd - 0.7) / 0.3)
    ball_color = np.where(seg1[..., None], seg1c, np.where(seg2[..., None], seg2c, seg3c))
    out = out * (1 - ball_mask[..., None]) + ball_color * ball_mask[..., None]

    # specular dot
    spec_a = radial_falloff(w, h, cx - ball_r * 0.32, cy - ball_r * 0.36, ball_r * 0.05, ball_r * 0.22) * 0.9
    out = out * (1 - spec_a[..., None]) + np.array(BALL_HIGHLIGHT, dtype=np.float32) * spec_a[..., None]

    return out


def draw_flippers(img, cx, cy, span):
    """Draws the two cradling flipper wedges with PIL polygons (crisp flat shapes)."""
    draw = ImageDraw.Draw(img, "RGBA")
    s = span / 60.0  # original vector design was authored at ~60-unit spacing

    def scaled(points):
        return [(cx + (px - 54) * s, cy + (py - 52) * s) for px, py in points]

    left = scaled([(25.35, 84.36), (48.41, 69.82), (51.59, 74.18), (30.65, 91.64)])
    right = scaled([(82.65, 84.36), (59.59, 69.82), (56.41, 74.18), (77.35, 91.64)])

    for poly in (left, right):
        draw.polygon(poly, fill=(*FLIPPER_COLOR, 255))

    draw.line([scaled([(27.4, 83.9)])[0], scaled([(47.6, 71.4)])[0]],
              fill=(255, 255, 255, 180), width=max(1, int(span * 0.02)))
    draw.line([scaled([(80.6, 83.9)])[0], scaled([(60.4, 71.4)])[0]],
              fill=(255, 255, 255, 180), width=max(1, int(span * 0.02)))


# ---------------------------------------------------------------------------
# Icon: 512x512, full-bleed square, no transparency
# ---------------------------------------------------------------------------

def build_icon(size=512):
    ss = size * SUPERSAMPLE
    bg = draw_backdrop(ss, ss, key_x_frac=0.28, key_y_frac=0.22)
    cx, cy = ss * 0.5, ss * 0.485
    bg = draw_reactor_ball(bg, ss, ss, cx, cy, ring_outer=ss * 0.222, ring_inner=ss * 0.176, ball_r=ss * 0.148)
    img = to_image(bg).convert("RGBA")
    draw_flippers(img, cx, cy, span=ss * 0.56)
    img = img.convert("RGB")
    img = img.resize((size, size), Image.LANCZOS)
    return img


# ---------------------------------------------------------------------------
# Feature graphic: 1024x500, no transparency
# ---------------------------------------------------------------------------

def build_feature_graphic(w=1024, h=500):
    ss = SUPERSAMPLE
    W, H = w * ss, h * ss
    bg = draw_backdrop(W, H, key_x_frac=0.16, key_y_frac=0.28)

    cx, cy = W * 0.205, H * 0.52
    ring_outer = H * 0.34
    bg = draw_reactor_ball(bg, W, H, cx, cy, ring_outer=ring_outer, ring_inner=ring_outer * 0.79, ball_r=ring_outer * 0.66)
    img = to_image(bg).convert("RGBA")
    draw_flippers(img, cx, cy, span=ring_outer * 2.55)

    draw = ImageDraw.Draw(img, "RGBA")
    text_x = int(W * 0.40)

    f_title = load_font(int(H * 0.185))
    f_title2 = load_font(int(H * 0.235))
    f_tag = load_font(int(H * 0.085))

    y = int(H * 0.26)
    draw.text((text_x, y), TITLE_LINE_1, font=f_title, fill=(255, 255, 255, 255))
    y += int(H * 0.205)
    draw.text((text_x, y), TITLE_LINE_2, font=f_title2, fill=(*RING_TOP, 255))
    y += int(H * 0.255)

    # small accent rule + tagline, echoing the in-app HUD chip style
    rule_w = draw.textlength(TAGLINE, font=f_tag) + int(H * 0.06)
    draw.rounded_rectangle(
        [text_x, y, text_x + rule_w, y + int(H * 0.13)],
        radius=int(H * 0.03),
        fill=(255, 255, 255, 18),
    )
    draw.text((text_x + int(H * 0.03), y + int(H * 0.028)), TAGLINE, font=f_tag, fill=(255, 194, 61, 255))

    img = img.convert("RGB")
    img = img.resize((w, h), Image.LANCZOS)
    return img


if __name__ == "__main__":
    import pathlib

    out_dir = pathlib.Path(__file__).parent

    icon = build_icon(512)
    icon.save(out_dir / "icon-512.png", format="PNG")
    print("Wrote", out_dir / "icon-512.png", icon.size)

    feature = build_feature_graphic(1024, 500)
    feature.save(out_dir / "feature-graphic-1024x500.png", format="PNG")
    print("Wrote", out_dir / "feature-graphic-1024x500.png", feature.size)
