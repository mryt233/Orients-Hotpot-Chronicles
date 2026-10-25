"""灶台正面（stove_front.png）：原版砖块 + 一个炉口拱门。

2026-10-23 用户：矮灶台看着太薄、灶膛口太大，改小一点，高灶台同步。原来的拱门占贴图第 5..15 行、宽 12，
灶台正面只取贴图第 5..16 行（align_stove_uv.py），拱门几乎顶满整面。现在拱门缩到第 9..15 行、宽 8（第 4..11 列），
上面露出更多砖。高灶台火口那一段用的是同一张贴图，自动跟着变。
**第一版 small（拱门第 9..15 行、宽 8）用户说太小**，留档在 archive；用户又说「改小一点，但不要那么小」，
游戏里用 medium（拱门第 7..15 行、宽 10），火焰范围见 gen_stove_fire.py 的 MASK。
存档在 archive/stove_mouth_small/：stove_front.png、点火动画 stove_front_lit.png（+ .mcmeta）、火焰范围 fire_mask.txt。
要换上：把这两张 png 拷进两份资源的 textures/block，gen_stove_fire.py 的 MASK 换成 fire_mask.txt，HotPotBlockEntity 炉口火苗 side 范围 0.36 → 0.3。

    python tools/gen_stove_front.py
作者：
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BRICKS = ROOT.parent / "viewer/cache/1.21.1/assets/minecraft/textures/block/bricks.png"   # 原版砖块（查看器缓存里的原版资源）
ARCHIVE = ROOT / "archive/stove_mouth_small"
GAME = [ROOT / "src/main/resources/assets/orients_hotpot_chronicles/textures/block",
        ROOT / "src/main/resources/resourcepacks/hotpot_assets/assets/orients_hotpot_chronicles/textures/block"]

# F 拱门边框（深砖色）  T 拱顶里面（暗红褐）  B 炉膛（近黑）  G 炉膛底（灰褐炉灰）  S 炉口下沿
SHAPES = {
    # 第一版：用户说太小，留档（只写到 archive）
    "small": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......FFFF......",
        ".....FTTTTF.....",
        "....FBBBBBBF....",
        "....FBBBBBBF....",
        "....FGGGGGGF....",
        "....FGGGGGGF....",
        "....SSSSSSSS....",
    ],
    # 用户：改小一点，但别像刚才那么小 → 拱门第 7..15 行、宽 10（原来第 5..15 行、宽 12），进游戏用这版
    "medium": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......FFFF......",
        ".....FTTTTF.....",
        "....FTTTTTTF....",
        "...FBBBBBBBBF...",
        "...FBBBBBBBBF...",
        "...FBBBBBBBBF...",
        "...FGGGGGGGGF...",
        "...FGGGGGGGGF...",
        "...SSSSSSSSSS...",
    ],
}
OUT = {"small": [ARCHIVE], "medium": GAME}
# 颜色取自原来的炉口贴图
COLORS = {
    "F": [(0x5E, 0x2A, 0x1A), (0x61, 0x2D, 0x1D), (0x5B, 0x27, 0x17), (0x65, 0x31, 0x21)],
    "T": [(0x38, 0x1A, 0x12), (0x36, 0x18, 0x10), (0x3B, 0x1D, 0x15), (0x40, 0x22, 0x1A)],
    "B": [(0x17, 0x12, 0x0F), (0x15, 0x10, 0x0D), (0x19, 0x14, 0x11), (0x1C, 0x17, 0x14)],
    "G": [(0x36, 0x28, 0x1E), (0x3E, 0x30, 0x26), (0x2E, 0x20, 0x16), (0x33, 0x25, 0x1B)],
    "S": [(0x5F, 0x2B, 0x1B), (0x58, 0x24, 0x14), (0x62, 0x2E, 0x1E), (0x5C, 0x28, 0x18)],
}


def draw(shape):
    rnd = random.Random(9)
    img = Image.open(BRICKS).convert("RGBA")
    px = img.load()
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch in COLORS:
                px[x, y] = rnd.choice(COLORS[ch]) + (255,)
    return img


def main():
    for name, shape in SHAPES.items():
        img = draw(shape)
        for d in OUT[name]:
            img.save(d / "stove_front.png")
    print("ok")


if __name__ == "__main__":
    main()
