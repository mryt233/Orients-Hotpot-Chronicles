"""六张食材贴图重绘、三套候选展板及教程图标。作者：mryt233

在 mod 目录运行：python tools/gen_redo_6_textures.py
只写六张白名单贴图、对应教程图标及 build/boards/redo_6_textures.png。
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from check_textures import check_image
from pixkit import canvas, io
from pixkit.paths import ASSETS, MOD
from pixkit.texstats import stats


NAMES = (
    "raw_squid", "enoki_mushroom", "cooked_enoki_mushroom",
    "tofu_skin", "cooked_tofu_skin", "peanut",
)
LABELS = ("生鱿鱼", "金针菇", "熟金针菇", "腐竹", "熟腐竹", "花生")
ITEMS = ASSETS / "textures/item"
ICONS = MOD / "tutorial/assets/items"
BOARD = MOD / "build/boards/redo_6_textures.png"
SELECTED = dict(zip(NAMES, ("D", "A", "A", "B", "B", "B")))
DISPLAY_VARIANTS = {name: ("B", "C", "D") if name in ("raw_squid", "peanut")
                    else ("A", "B", "C") for name in NAMES}

# 手工像素图的六级珍珠粉紫色阶；深紫眼点独立于肉质高光。
SQUID_PALETTE = dict(zip("odmlhe", (
    (91, 58, 83, 255), (141, 91, 121, 255), (187, 133, 165, 255),
    (224, 183, 207, 255), (255, 239, 238, 255), (60, 39, 65, 255),
)))
SQUID_ROWS = {
    "A": (
        "", "...oo", "..ohloo", ".ohhllmo", "..olhllmo",
        "...olhllmo", "....olhllmo", ".....ollmmdo", "......omlddo",
        ".......ohoeo.oo", ".......omleoldo", "......odollodo",
        "......oo.olo.oo", ".........olo.oo", ".........oo", "",
    ),
    "B": (
        "", ".....oo", "..ooohmo", ".ohhllmo", ".olhhlhmo",
        ".omllhllmo", "..oodlhlmdo", ".....olhlmdo", "......olhmdo",
        ".......ohldo", "........oeo", "", "", "", "", "",
    ),
    "C": (
        "", "....oo", "..ooho", ".ohhlloo", ".olhhllmo",
        "..ollhllmo", "...olhllmdo", "....ollhlmdo", ".....ollmlddo",
        "......ohldeo.oo", "......oemleoldo", ".....oloohloodo",
        ".....oo.ollo.oo", ".......olo.oldo", ".......oo..oo", "",
    ),
}


def squid(variant: str) -> Image.Image:
    rows = SQUID_ROWS[variant]
    if len(rows) != 16 or any(len(row) > 16 for row in rows):
        raise ValueError("鱿鱼像素图超出 16×16")
    body = canvas.from_rows([row.ljust(16, ".") for row in rows], SQUID_PALETTE)
    if variant != "B":
        return body
    im = canvas.blank()
    draw = ImageDraw.Draw(im)
    paths = (
        [(10, 9), (12, 8), (13, 7), (14, 8), (13, 9)],
        [(10, 10), (12, 10), (14, 11), (13, 12)],
        [(10, 10), (11, 12), (13, 13), (12, 14)],
        [(9, 10), (9, 12), (10, 14), (9, 14)],
        [(8, 9), (7, 11), (6, 12), (6, 11)],
    )
    for path in paths:
        draw.line([(min(x + 1, 14), y) for x, y in path], fill=SQUID_PALETTE["o"])
        draw.line(path, fill=SQUID_PALETTE["m"])
        draw.point(path[0], fill=SQUID_PALETTE["l"])
    im.alpha_composite(body)
    draw = ImageDraw.Draw(im)
    draw.line([(8, 9), (9, 9), (10, 8)], fill=SQUID_PALETTE["h"])
    draw.point((8, 10), fill=SQUID_PALETTE["e"])
    draw.point((10, 9), fill=SQUID_PALETTE["e"])
    return im


def squid_pink() -> Image.Image:
    """mryt233 改：粉白胴体朝右上，宽耳鳍与左下五根弯腕分层绘制。"""
    o, d, m, l, h = "#8d4d57", "#c17f88", "#efb8be", "#ffd9d9", "#fff2ec"
    im = canvas.blank()
    draw = ImageDraw.Draw(im)
    arms = (
        [(4, 9), (2, 8), (1, 9), (1, 10)],
        [(4, 10), (3, 11), (1, 12), (2, 13)],
        [(5, 10), (4, 12), (3, 14), (2, 14)],
        [(6, 10), (6, 12), (5, 14), (6, 14)],
        [(7, 10), (8, 11), (8, 13), (7, 14)],
    )
    for path in arms:
        draw.line([(x + 1, y) for x, y in path], fill=o)
        draw.line(path, fill=m)
        draw.point(path[0], fill=l)
    fins = canvas.blank()
    fd = ImageDraw.Draw(fins)
    fd.polygon([(7, 1), (10, 2), (12, 2), (13, 4), (14, 7),
                (11, 7), (9, 5), (7, 4)], fill=d)
    fd.polygon([(8, 2), (10, 3), (12, 3), (12, 5), (11, 5), (9, 4)], fill=m)
    canvas.outline_inner(fins, (141, 77, 87))
    im.alpha_composite(fins)
    mantle = canvas.blank()
    md = ImageDraw.Draw(mantle)
    md.polygon([(11, 2), (12, 3), (11, 6), (8, 9), (7, 10),
                (4, 8), (5, 6), (8, 3)], fill=l)
    md.polygon([(11, 3), (11, 6), (8, 9), (7, 9), (7, 8), (10, 4)], fill=m)
    md.line([(9, 3), (7, 4), (6, 6), (5, 7)], fill=h, width=2)
    canvas.outline_inner(mantle, (141, 77, 87))
    im.alpha_composite(mantle)
    draw = ImageDraw.Draw(im)
    draw.line([(4, 8), (5, 9), (6, 10), (7, 10)], fill=d)
    draw.line([(4, 9), (5, 10), (6, 10)], fill=h)
    draw.point((4, 8), fill="#55383d")
    draw.point((7, 10), fill="#55383d")
    return im


def enoki(variant: str, cooked: bool) -> Image.Image:
    palettes = (
        ("#756344", "#af9d76", "#d7c99f", "#eee3c8", "#fff9e5", "#fffdf1"),
        ("#79502e", "#ad783e", "#c89a58", "#e7c07b", "#f9dda2", "#fff1c9"),
    )
    o, d, m, l, h, shine = palettes[cooked]
    tips = {
        "A": [(2, 5), (5, 2), (8, 3), (11, 2), (12, 6)],
        "B": [(2, 5), (4, 2), (7, 4), (9, 2), (12, 3), (5, 6), (12, 7)],
        "C": [(2, 6), (4, 3), (7, 2), (10, 3), (12, 5), (8, 6)],
    }[variant]
    im = canvas.blank()
    draw = ImageDraw.Draw(im)
    ends = []
    for index, (x, y) in enumerate(tips):
        if cooked:
            x += 1 if x < 4 else -1 if x > 11 else 0
            y += 1
        bx = 6 + index % 4
        bend = ((x + bx) // 2, 9 + (index % 2))
        path = [(x, y + 1), bend, (bx, 13)]
        draw.line(path, fill=o, width=2)
        draw.line(path, fill=l if index % 2 else h, width=1)
        ends.append((x, y))
    # 根蒂与前景菌柄连接成束，纵向纹理沿菌柄走向收拢。
    draw.polygon([(5, 11), (10, 11), (11, 13), (10, 14), (6, 14), (5, 13)], fill=o)
    draw.line([(6, 11), (7, 13), (9, 13)], fill=m, width=2)
    draw.line([(7, 11), (8, 13)], fill=l)
    draw.point((9, 12), fill=d)
    for x, y in ends:
        draw.line([(x, y - 1), (x + 1, y - 1)], fill=m)
        draw.line([(x - 1, y), (x + 2, y)], fill=o)
        draw.point((x, y), fill=shine)
        draw.point((x + 1, y), fill=h)
        draw.line([(x, y + 1), (x + 1, y + 1)], fill=d)
    return im


def tofu(variant: str, cooked: bool) -> Image.Image:
    colors = (
        ("#785025", "#a67530", "#c8973d", "#e5b64f", "#f6d47b", "#fff0b3"),
        ("#83603e", "#b08a58", "#cfad78", "#e7ca91", "#f8e2b0", "#fff3d2"),
    )
    o, d, m, l, h, shine = colors[cooked]
    polygons = {
        "A": [
            [(2, 6), (10, 1), (12, 3), (4, 9)],
            [(3, 10), (11, 5), (14, 7), (6, 13)],
            [(1, 11), (7, 8), (10, 11), (4, 14)],
        ],
        "B": [
            [(2, 6), (10, 1), (12, 3), (4, 9)],
            [(4, 9), (11, 4), (14, 6), (6, 12)],
            [(1, 11), (9, 6), (12, 9), (4, 14)],
        ],
        "C": [
            [(2, 6), (10, 1), (13, 4), (5, 9)],
            [(2, 11), (10, 6), (14, 9), (5, 14)],
        ],
    }[variant]
    im = canvas.blank()
    for vertices in polygons:
        if cooked:
            vertices = [(x - (1 if i == 0 and x > 1 else 0), y)
                        for i, (x, y) in enumerate(vertices)]
        part = canvas.blank()
        pd = ImageDraw.Draw(part)
        pd.polygon(vertices, fill=l)
        mask = part.getchannel("A")
        a, b, c, end = vertices
        # 双层折口与平行豆皮褶皱；后画的片段盖住前一片的边缘。
        ridge = [(a[0] + 1, a[1]), (b[0], b[1] + 1)]
        pd.line(ridge, fill=h)
        pd.line([(a[0] + 1, a[1] + 1), (b[0] + 1, b[1] + 1)], fill=d if not cooked else m)
        pd.line([(a[0] + 2, a[1] + 1), (b[0] + 1, b[1] + 2)], fill=l if not cooked else h)
        pd.line([(a[0] + 2, a[1] + 2), (c[0] - 1, c[1])], fill=m)
        pd.line([(a[0] + 1, a[1]), (end[0], end[1] - 1)], fill=d)
        pd.point((a[0] + 1, a[1] + 1), fill=shine)
        if cooked:
            pd.line([(b[0] - 2, b[1] + 2), (b[0] - 1, b[1] + 1)], fill=shine)
        # 细节只允许落在原轮廓内，避免折纹额外长出散点。
        part.putalpha(mask)
        canvas.outline_inner(part, (131, 96, 62) if cooked else (120, 80, 37))
        im.alpha_composite(part)
    return im


def peanut(variant: str) -> Image.Image:
    o, d, m, l, h, shine = "#725334", "#a17c49", "#c5a063", "#ddbd81", "#efd6a0", "#fff0c7"
    im = canvas.blank()
    draw = ImageDraw.Draw(im)
    shapes = {
        "A": [(8, 1), (11, 1), (13, 3), (13, 5), (10, 7), (9, 7), (8, 10),
              (6, 12), (3, 12), (1, 10), (1, 8), (3, 6), (6, 5), (6, 3)],
        "B": [(9, 1), (12, 1), (14, 3), (14, 5), (12, 7), (10, 7), (9, 9),
              (7, 11), (4, 11), (2, 9), (2, 7), (4, 5), (6, 5), (7, 3)],
        "C": [(5, 1), (8, 1), (10, 3), (10, 5), (8, 7), (9, 9),
              (9, 12), (7, 14), (4, 14), (2, 12), (2, 10), (4, 8), (4, 7), (3, 5), (3, 3)],
    }
    draw.polygon(shapes[variant], fill=m)
    draw.polygon([(9, 2), (12, 2), (13, 3), (12, 5), (9, 6), (8, 5), (8, 3)], fill=l)
    draw.polygon([(4, 6), (6, 6), (8, 8), (6, 10), (4, 10), (3, 9), (3, 7)], fill=l)
    canvas.outline_inner(im, (114, 83, 52))
    # 每个鼓包只留两条连贯壳脊，收腰处保留低亮度过渡。
    draw.line([(9, 3), (11, 2), (12, 3)], fill=h)
    draw.line([(9, 4), (11, 4), (12, 5)], fill=d)
    draw.line([(10, 3), (10, 5)], fill=h)
    draw.line([(4, 7), (5, 6), (6, 7)], fill=h)
    draw.line([(4, 9), (6, 9), (7, 8)], fill=d)
    draw.line([(5, 7), (5, 9)], fill=h)
    draw.line([(7, 6), (8, 7)], fill=d)
    draw.point((9, 2), fill=shine)
    draw.point((4, 7), fill=shine)
    if variant == "C":
        draw.polygon(shapes[variant], fill=l)
        canvas.outline_inner(im, (114, 83, 52))
        draw.line([(5, 3), (7, 2), (8, 3)], fill=h)
        draw.line([(5, 5), (7, 5), (8, 4)], fill=d)
        draw.line([(6, 3), (6, 5)], fill=h)
        draw.line([(4, 10), (6, 9), (7, 10)], fill=h)
        draw.line([(4, 12), (6, 12), (7, 11)], fill=d)
        draw.line([(5, 10), (5, 12)], fill=h)
        draw.line([(5, 7), (6, 8)], fill=d)
        draw.point((5, 3), fill=shine)
    if variant == "B":
        # mryt233 改：仅保留完整带壳花生，去除右下角红仁及半剥壳后重新居中。
        shifted = canvas.blank()
        shifted.alpha_composite(im, (-1, 2))
        return shifted
    # 半剥壳的红衣花生仁，乳白色断口与沙黄壳边形成清晰色差。
    kx, ky = 11, 10
    draw.polygon([(kx - 2, ky), (kx, ky - 2), (kx + 2, ky - 1),
                  (kx + 3, ky + 1), (kx + 2, ky + 3), (kx, ky + 3), (kx - 2, ky + 1)], fill=o)
    draw.polygon([(kx - 1, ky), (kx, ky - 1), (kx + 2, ky),
                  (kx + 2, ky + 2), (kx, ky + 2)], fill=h)
    draw.polygon([(kx - 1, ky), (kx, ky - 1), (kx + 1, ky - 1),
                  (kx + 2, ky), (kx + 1, ky + 2), (kx, ky + 2), (kx - 1, ky + 1)], fill="#783d43")
    draw.line([(kx, ky), (kx, ky + 1)], fill="#c57672", width=1)
    draw.point((kx + 1, ky), fill="#a85458")
    draw.point((kx, ky - 1), fill=shine)
    if variant == "C":
        draw.polygon([(11, 5), (12, 4), (13, 5), (13, 7), (12, 8), (11, 7)], fill="#783d43")
        draw.line([(12, 5), (12, 6)], fill="#c57672")
        draw.point((12, 5), fill=shine)
    return im


def peanut_open() -> Image.Image:
    """mryt233 改：同一只半剥花生壳托住两颗完整红衣仁，不放散落花生。"""
    im = canvas.blank()
    draw = ImageDraw.Draw(im)
    shell = [(9, 1), (12, 1), (14, 3), (14, 6), (12, 8), (10, 8),
             (8, 12), (6, 14), (3, 14), (1, 12), (1, 9), (3, 7), (6, 6), (7, 3)]
    draw.polygon(shell, fill="#c9a06a")
    canvas.outline_inner(im, (139, 105, 69))
    draw.polygon([(9, 2), (12, 2), (13, 3), (13, 6), (11, 7), (9, 7),
                  (7, 11), (6, 13), (3, 13), (2, 12), (2, 9), (4, 8), (7, 7), (8, 4)], fill="#80543e")
    draw.line([(2, 9), (4, 7), (6, 7), (8, 3), (10, 2), (12, 2)], fill="#f2d7a8")
    draw.line([(2, 12), (3, 13), (6, 13), (8, 10), (9, 8), (12, 7), (13, 5)], fill="#e7c48f")
    # 两颗种仁的高光沿左上侧相连，暗部贴着右下壳内壁。
    draw.polygon([(10, 3), (12, 3), (12, 5), (11, 7), (9, 7), (8, 6), (8, 5)], fill="#a65b50")
    draw.polygon([(10, 3), (11, 3), (11, 5), (10, 6), (9, 6), (9, 4)], fill="#d78a73")
    draw.line([(10, 3), (9, 4), (9, 5)], fill="#f1b497")
    draw.point((10, 3), fill="#ffe5c4")
    draw.polygon([(4, 8), (6, 8), (8, 9), (7, 11), (5, 12), (3, 12), (2, 11), (3, 9)], fill="#a65b50")
    draw.polygon([(4, 9), (6, 9), (7, 10), (5, 11), (3, 11), (3, 10)], fill="#d78a73")
    draw.line([(4, 9), (5, 9), (6, 9)], fill="#f1b497")
    draw.point((4, 9), fill="#ffe5c4")
    draw.line([(3, 13), (5, 13), (7, 12)], fill="#e7c48f")
    return im


def candidates() -> dict[str, dict[str, Image.Image]]:
    images = {v: dict(zip(NAMES, (
        squid(v), enoki(v, False), enoki(v, True),
        tofu(v, False), tofu(v, True), peanut(v),
    ))) for v in "ABC"}
    images["D"] = {"raw_squid": squid_pink(), "peanut": peanut_open()}
    return images


def validate(all_images: dict[str, dict[str, Image.Image]]) -> None:
    for variant, images in all_images.items():
        expected = {"raw_squid", "peanut"} if variant == "D" else set(NAMES)
        if set(images) != expected:
            raise ValueError("贴图集合不符合六项白名单")
        for name, im in images.items():
            s = stats(im)
            issues = check_image(im, f"item/{name}")
            if im.mode != "RGBA" or im.size != (16, 16) or s["semi"] or issues:
                raise ValueError(f"{variant}/{name}: {issues or '尺寸、模式或透明度不合规'}")
            if any(c < 1 for c in s["bbox"][:2]) or any(c > 15 for c in s["bbox"][2:]):
                raise ValueError(f"{variant}/{name}: 四周需要至少一像素透明边距")
            solid = [(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]]
            mass = [sum(p[axis] for p in solid) / len(solid) - 7.5 for axis in (0, 1)]
            if any(abs(offset) > 1 for offset in mass):
                raise ValueError(f"{variant}/{name}: 像素重心偏移 {mass} 超过 1px")
            if "tofu_skin" in name and not 95 <= s["opaque"] <= 130:
                raise ValueError(f"{variant}/{name}: 腐竹有效像素 {s['opaque']}，需要 95～130")


def font(size: int) -> ImageFont.ImageFont:
    for path in (Path("C:/Windows/Fonts/msyh.ttc"), Path("C:/Windows/Fonts/simhei.ttf"),
                 Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")):
        if path.is_file():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def board(all_images: dict[str, dict[str, Image.Image]]) -> Image.Image:
    width, height = 1536, 2490
    out = Image.new("RGB", (width, height), "#171b22")
    draw = ImageDraw.Draw(out)
    title, normal, small = font(30), font(18), font(15)
    draw.text((32, 22), "食材贴图重绘 · 选定方案与重绘对比", font=title, fill="#fff1d7")
    draw.text((32, 66), "作者：mryt233  |  16×16 原生像素  |  左上受光  |  深色槽位 / 浅色背景", font=normal, fill="#bac2cb")
    draw.text((32, 106), "已采用：金针菇 A / 腐竹 B / 粉白鱿鱼 D / 完整带壳花生 B", font=normal, fill="#efd19b")
    for index, (name, label) in enumerate(zip(NAMES, LABELS)):
        y = 150 + index * 377
        draw.text((32, y), f"{index + 1:02d}  {label}  /  {name}.png", font=normal, fill="#f0e5d5")
        for vi, variant in enumerate(DISPLAY_VARIANTS[name]):
            x, top = 32 + vi * 500, y + 32
            im = all_images[variant][name]
            selected = variant == SELECTED[name]
            draw.rounded_rectangle((x, top, x + 472, top + 325), radius=10,
                                   fill="#242b34", outline="#dbb877" if selected else "#414953", width=2)
            caption = ("新方案 D" if variant == "D" else f"方案 {variant}")
            if name == "peanut" and variant == "B":
                caption += " · 仅保留带壳花生"
            draw.text((x + 14, top + 9), caption + ("  ·  已采用" if selected else ""),
                      font=normal, fill="#f5d395" if selected else "#d4d9df")
            for bi, light in enumerate((False, True)):
                by = top + 45 + bi * 129
                draw.text((x + 12, by + 6), "浅色" if light else "槽位", font=small, fill="#adb7c4")
                bx = x + 66
                for scale, area in ((1, 44), (4, 88), (8, 152)):
                    fill = "#f1e7d7" if light else "#373737"
                    draw.rectangle((bx, by, bx + area - 1, by + 111), fill=fill)
                    if not light:
                        draw.line([(bx, by + 111), (bx, by), (bx + area - 1, by)], fill="#191919", width=2)
                        draw.line([(bx, by + 111), (bx + area - 1, by + 111), (bx + area - 1, by)], fill="#8b8b8b", width=2)
                    side = scale * 16
                    iy = by + (112 - side) // 2 if side < 112 else by - 8
                    # 8 倍图完整显示在 152 像素高的独立区域。
                    if scale == 8:
                        draw.rectangle((bx, by - 8, bx + area - 1, by + 119), fill=fill)
                    enlarged = im.resize((side, side), Image.Resampling.NEAREST)
                    out.paste(enlarged, (bx + (area - side) // 2, iy), enlarged)
                    draw.text((bx + area + 5, by + 44), f"{scale}×", font=small, fill="#aeb9c6")
                    bx += area + 37
            s = stats(im)
            draw.text((x + 14, top + 305), f"{s['opaque']} px  ·  {s['colors']} 色  ·  偏移 {s['center']}  ·  半透明 0", font=small, fill="#b4bfcb")
    draw.text((32, 2445), "最近邻放大  |  生熟造型分别绘制  |  教程图标同步为 32×32", font=normal, fill="#bac2cb")
    return out


def fingerprints() -> dict[Path, str]:
    paths = [*(ASSETS / "textures").rglob("*.png"), *ICONS.glob("*.png")]
    return {p: hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}


def main() -> None:
    before = fingerprints()
    all_images = candidates()
    validate(all_images)
    comparison = board(all_images)
    allowed = {folder / f"{name}.png" for folder in (ITEMS, ICONS) for name in NAMES}
    selected = {name: all_images[SELECTED[name]][name] for name in NAMES}
    outputs = {ITEMS / f"{n}.png": im for n, im in selected.items()}
    outputs.update({ICONS / f"{n}.png": im.resize((32, 32), Image.Resampling.NEAREST)
                    for n, im in selected.items()})
    if set(outputs) != allowed:
        raise ValueError("输出路径超出六项白名单")
    for path, im in outputs.items():
        if path.is_file():
            with Image.open(path) as current:
                if current.mode == im.mode and current.size == im.size and current.tobytes() == im.tobytes():
                    continue
        io.save_png(im, path)
    io.save_png(comparison, BOARD)
    after = fingerprints()
    unexpected = [p.name for p in before.keys() | after.keys()
                  if p not in allowed and before.get(p) != after.get(p)]
    if unexpected:
        raise RuntimeError(f"白名单以外贴图发生变化：{unexpected}")
    print("作者：mryt233；20 张候选自检通过；金针菇 A、腐竹 B、鱿鱼 D、完整带壳花生 B 已同步。")
    for name, im in selected.items():
        s = stats(im)
        print(f"{name}: {s['opaque']} px, {s['colors']} 色, 偏移 {s['center']}, 半透明 {s['semi']}")
    print(f"其余 {len(before.keys() - allowed)} 张原有贴图指纹一致。")
    print("展板：build/boards/redo_6_textures.png")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
