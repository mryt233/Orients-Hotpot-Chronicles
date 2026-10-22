"""Generate the burning stove-mouth animation (stove_front_lit.png + .mcmeta).

Starts from the unlit front (stove_front.png) so bricks and the arch are untouched, and animates
only the fire pixels inside the mouth over a seamless 16-frame loop:
  * tongues - each column flickers on its own (sum of sines with whole cycles per loop),
              tallest in the middle; yellow core -> orange -> red tips
  * embers  - the coal bed at the bottom glows and dims unevenly
  * sparks  - now and then a tip breaks off for a frame
Re-running is safe: the mask below is the fire area of the original static lit texture.

    python tools/gen_stove_fire.py
"""
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
DIRS = [
    ROOT / "assets/orients_hotpot_chronicles/textures/block",
    ROOT / "resourcepacks/hotpot_assets/assets/orients_hotpot_chronicles/textures/block",
]
FRAMES = 16
FRAMETIME = 2

# fire area of the mouth (the pixels the old static lit texture painted)
MASK = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......####......",
    ".....######.....",
    "....########....",
    "...#########....",
    "...#########....",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "................",
]

BACK = (0x3A, 0x0C, 0x05)      # dark glowing back wall
RED = (0xD8, 0x40, 0x1F)
ORANGE = (0xF0, 0x8A, 0x2C)
YELLOW = (0xFB, 0xE0, 0x8A)
EMBER_DIM = (0xA8, 0x2E, 0x16)
EMBER_HOT = (0xF0, 0xA2, 0x3C)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3))


def wave(f, x, parts):
    """Sum of sines with whole cycles per loop, so frame 16 == frame 0."""
    return sum(a * math.sin(2 * math.pi * (k * f / FRAMES) + x * ph) for a, k, ph in parts)


def main():
    base = Image.open(DIRS[0] / "stove_front.png").convert("RGBA")
    cols = {}
    for y, row in enumerate(MASK):
        for x, ch in enumerate(row):
            if ch == "#":
                cols.setdefault(x, []).append(y)
    bottom = max(max(v) for v in cols.values())
    cx = (min(cols) + max(cols)) / 2
    half = (max(cols) - min(cols)) / 2

    sheet = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        img = base.copy()
        px = img.load()
        for x, ys in cols.items():
            top = min(ys)
            room = bottom - top + 1
            # taller in the middle, flickering per column
            shape = 1 - ((x - cx) / (half + 1)) ** 2 * 0.55
            n = 0.5 + 0.5 * wave(f, x, [(0.55, 2, 1.9), (0.3, 3, 0.7), (0.15, 5, 2.3)])
            height = max(2, round(room * shape * (0.55 + 0.45 * n)))
            for y in ys:
                d = bottom - y  # 0 at the bottom row
                if d <= 1:
                    # ember bed
                    glow = 0.5 + 0.5 * wave(f, x * 1.7 + d, [(1.0, 1, 1.3)])
                    c = mix(EMBER_DIM, EMBER_HOT, glow)
                elif d < height:
                    r = d / height  # 0 bottom of the flame .. 1 tip
                    core = abs(x - cx) <= half * 0.45
                    if r < 0.38 and core:
                        c = YELLOW
                    elif r < 0.72:
                        c = ORANGE
                    else:
                        c = RED
                else:
                    c = mix(BACK, RED, 0.25 * (1 - (d - height) / max(1, room - height)))
                px[x, y] = (*c, 255)
            # spark: a tip that breaks away for a frame
            spark_y = bottom - height - 1
            if wave(f, x, [(1.0, 3, 2.9)]) > 0.85 and spark_y >= top:
                px[x, spark_y] = (*YELLOW, 255)
        sheet.paste(img, (0, f * 16))

    meta = {"animation": {"frametime": FRAMETIME, "interpolate": False}}
    for d in DIRS:
        sheet.save(d / "stove_front_lit.png")
        (d / "stove_front_lit.png.mcmeta").write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    print("wrote stove_front_lit")


if __name__ == "__main__":
    main()
