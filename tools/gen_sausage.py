"""火腿肠与猪肉末的造型预览（2026-10-29）。

作者：mryt233
原料方案：原版猪排用菜刀加工成猪肉末，与原版小麦合成火腿肠；不新增淀粉物品。
当前默认只输出已确认的包装预览；运行 `python tools/gen_sausage.py install` 才会写入正式资源、配方和 cuttable 标签。
运行：python tools/gen_sausage.py（默认红色包装，--plain 看最初三款裸肠对照）
"""
from __future__ import annotations

import json
import math
from pathlib import Path
import random
import sys

from PIL import Image, ImageDraw, ImageFont

MOD = Path(__file__).resolve().parent.parent
OUT = MOD / "tools/out/sausage"
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures/item"
TRANSPARENT = (0, 0, 0, 0)
STYLES = ("slim", "plump", "tied")
RAW = {
    "outline": (119, 63, 58),
    "dark": (189, 101, 91),
    "base": (226, 151, 139),
    "light": (244, 183, 169),
    "shine": (255, 213, 190),
}
COOKED = {
    "outline": (102, 56, 38),
    "dark": (163, 87, 55),
    "base": (201, 125, 83),
    "light": (227, 159, 111),
    "shine": (248, 193, 142),
}


def sausage(style: str, cooked: bool = False) -> Image.Image:
    """斜放的长条肠，三种粗细 / 端头，16×16 原版物品规格。"""
    pal = COOKED if cooked else RAW
    radius = 1.75 if style == "slim" else 2.25
    end = 6.1 if style != "tied" else 4.8
    direction = math.sqrt(0.5)
    solid: dict[tuple[int, int], tuple[float, float, float]] = {}
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8.0, y + 0.5 - 8.0
            along = (dx - dy) * direction
            across = (dx + dy) * direction
            cap = max(0.0, abs(along) - end)
            if cap * cap + across * across <= radius * radius:
                solid[x, y] = (along, across, cap)
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    px = im.load()
    for (x, y), (along, across, cap) in solid.items():
        if any((x + ox, y + oy) not in solid for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1))):
            color = pal["outline"]
        elif across < -0.7:
            color = pal["shine"] if -3.5 < along < 4.5 else pal["light"]
        elif across < 0.2:
            color = pal["light"]
        elif across > radius * 0.45 or cap > radius * 0.45:
            color = pal["dark"]
        else:
            color = pal["base"]
        px[x, y] = color + (255,)
    if style == "tied":
        # 两端收口，不画不可食用的塑料包装和金属卡扣。
        draw = ImageDraw.Draw(im)
        for x, y in ((2, 13), (13, 2)):
            draw.point((x, y), fill=pal["outline"] + (255,))
            draw.point((x + (1 if x < 8 else -1), y), fill=pal["light"] + (255,))
            draw.point((x, y + (-1 if x < 8 else 1)), fill=pal["base"] + (255,))
    return im


def wrapped_sausage() -> Image.Image:
    """用户选第 2 款稍粗圆头；改为大红包装与两端小银色封口。"""
    im = sausage("plump")
    colors = {
        RAW["outline"]: (108, 19, 24),
        RAW["dark"]: (166, 22, 30),
        RAW["base"]: (222, 35, 39),
        RAW["light"]: (246, 60, 55),
        RAW["shine"]: (255, 112, 92),
    }
    px = im.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                px[x, y] = colors[px[x, y][:3]] + (255,)
    # 用户澄清：两端各一整排银色，横跨肠身但只有一格宽，不是单点。
    silver = (208, 216, 216, 255)   # 用户：银色统一颜色，不分明暗。
    for x, y in ((3, 12), (12, 3)):
        for offset in (-1, 0, 1):
            px[x + offset, y + offset] = silver
    return im


def wrapped_preview() -> Path:
    """只看包装外观，不在预览阶段决定熟肠是否拆包装。"""
    OUT.mkdir(parents=True, exist_ok=True)
    im = wrapped_sausage()
    im.save(OUT / "wrapped_sausage_preview.png")
    sheet = Image.new("RGBA", (640, 440), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((28, 20), "火腿肠 · 大红色包装", font=font(25, True), fill=ink)
    draw.text((29, 62), "保留第 2 款稍粗圆头轮廓，两端加小银色封口", font=font(17), fill=dim)
    for x, y, size, label in ((75, 117, 224, "放大看像素"), (420, 160, 96, "物品栏比例参照")):
        draw.rectangle((x - 4, y - 4, x + size + 3, y + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, y - 2, x + size + 1, y + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, y))
        face = font(17)
        width = draw.textbbox((0, 0), label, font=face)[2]
        draw.text((x + (size - width) / 2, y + size + 15), label, font=face, fill=dim)
    draw.text((29, 403), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "wrapped_sheet.png"
    sheet.convert("RGB").save(path)
    return path


def peeled_sausage() -> Image.Image:
    """完全去掉包装的火腿肠（用户 2026-10-29：做个完全去掉皮的看看效果）。
    轮廓沿用第 2 款稍粗圆头，把大红包装和两端银封全部去掉，只剩肠体本身。
    用户补充：两头的尖尖是包装收口的造型，去掉包装后要一起削掉，做成圆头。"""
    im = sausage("plump")
    px = im.load()

    def solid(x: int, y: int) -> bool:
        return 0 <= x < 16 and 0 <= y < 16 and px[x, y][3] > 0

    # 沿长轴两端的收口尖：只跟一格相连的像素，削掉
    tips = [(x, y) for y in range(16) for x in range(16)
            if solid(x, y) and sum(solid(x + dx, y + dy)
                                   for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1))) <= 1]
    for x, y in tips:
        px[x, y] = TRANSPARENT

    colors = {
        RAW["outline"]: (124, 58, 62),
        RAW["dark"]: (185, 97, 99),
        RAW["base"]: (224, 140, 136),
        RAW["light"]: (243, 178, 168),
        RAW["shine"]: (255, 216, 202),
    }
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                px[x, y] = colors[px[x, y][:3]] + (255,)
    return im


def peeled_preview() -> Path:
    """包装版和去皮版并排对照。"""
    OUT.mkdir(parents=True, exist_ok=True)
    wrapped, peeled = wrapped_sausage(), peeled_sausage()
    wrapped.save(OUT / "wrapped_sausage_preview.png")
    peeled.save(OUT / "peeled_sausage_preview.png")
    sheet = Image.new("RGBA", (900, 480), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((30, 20), "火腿肠 · 去掉包装对照", font=font(25, True), fill=ink)
    draw.text((31, 60), "大红包装和两端银封全部去掉，收口的尖也削掉，只留圆头肠体",
              font=font(17), fill=dim)
    size = 216
    for im, cx, name in ((wrapped, 240, "现在：大红色包装"), (peeled, 660, "完全去掉包装")):
        x = cx - size // 2
        draw.rectangle((x - 4, 113, x + size + 3, 113 + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, 115, x + size + 1, 113 + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, 117))
        face = font(18)
        width = draw.textbbox((0, 0), name, font=face)[2]
        draw.text((cx - width / 2, 355), name, font=face, fill=ink)
        sheet.alpha_composite(im.resize((72, 72), Image.Resampling.NEAREST), (cx - 36, 392))
    draw.text((30, 452), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "peeled_sheet.png"
    sheet.convert("RGB").save(path)
    return path


SLICE = {
    "rim": (152, 74, 70),      # 肠衣那一圈
    "inner": (207, 124, 114),
    "base": (233, 156, 146),
    "light": (250, 199, 185),
    "fat": (255, 226, 210),    # 脂肪点
    "side": (118, 54, 52),     # 切片的厚度（侧壁）
}


def _slice_shape(cx: float, cy: float, rx: float, ry: float, deg: float = -45.0,
                 yscale: float = 1.0) -> dict:
    """一片火腿肠横截面：椭圆，默认斜 45°。
    yscale > 1 时只在垂直方向拉高，宽度不变（用户 2026-10-29：要「这样加高」）。
    返回 {(x, y): (along, across, t)}，t 越小越靠中心。"""
    a = math.radians(deg)
    ca, sa = math.cos(a), math.sin(a)
    out = {}
    for y in range(16):
        for x in range(16):
            dx = x + 0.5 - cx
            dy = (y + 0.5 - cy) / yscale
            along = dx * ca + dy * sa
            across = -dx * sa + dy * ca
            t = (along / rx) ** 2 + (across / ry) ** 2
            if t <= 1.0:
                out[x, y] = (along, across, t)
    return out


def _paint_slice(im: Image.Image, shape: dict, rnd: random.Random, side: bool = False,
                 pal: dict | None = None) -> None:
    """把一片画到图上：外圈肠衣、内圈肉色、左上高光，撒几颗脂肪点。
    side=True 时先在下方多画一圈深色，做出切片的厚度；pal 换一套配色（熟片用更深的）。"""
    pal = pal or SLICE
    px = im.load()
    if side:
        for x, y in shape:
            if 0 <= y + 1 < 16 and (x, y + 1) not in shape:
                px[x, y + 1] = pal["side"] + (255,)
    for (x, y), (along, across, t) in shape.items():
        # 真一像素描边：四邻里只要有不在这一片上的，就是轮廓（和整根火腿肠的写法一致）
        edge = any((x + ox, y + oy) not in shape
                   for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1)))
        if edge:
            color = pal["rim"]
        elif t > 0.58:
            color = pal["inner"]
        elif across < -0.6:
            color = pal["light"]
        else:
            color = pal["base"]
        px[x, y] = color + (255,)
    inner = [k for k in sorted(shape) if shape[k][2] < 0.40]
    for x, y in rnd.sample(inner, min(2, len(inner))):
        px[x, y] = pal["fat"] + (255,)


COOKED_SLICE = {
    "rim": (118, 52, 44),
    "inner": (172, 86, 68),
    "base": (205, 120, 94),
    "light": (228, 154, 124),
    "fat": (245, 198, 166),
    "side": (96, 40, 34),
}


def cooked_ham_slice() -> Image.Image:
    """熟火腿肠片：和火腿肠片同一个形状，换成加热后偏焦褐的配色。"""
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    rnd = random.Random(43)
    for dx in (-2.7, 0.0, 2.7):
        _paint_slice(im, _slice_shape(8.0 + dx, 8.0, 4.5, 4.3, -45, 1.5), rnd, pal=COOKED_SLICE)
    return im


def ham_slice() -> Image.Image:
    """定稿的火腿肠片（用户 2026-10-29 定）：片 4.5×4.3、三片横向排开、间距 2.7、高度 ×1.5。"""
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    rnd = random.Random(43)
    for dx in (-2.7, 0.0, 2.7):
        _paint_slice(im, _slice_shape(8.0 + dx, 8.0, 4.5, 4.3, -45, 1.5), rnd)
    return im


def sausage_slice() -> Image.Image:
    """单片火腿肠切片。"""
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    _paint_slice(im, _slice_shape(8.0, 8.0, 4.8, 3.4), random.Random(7))
    return im


def sausage_stack() -> Image.Image:
    """三片叠在一起：沿长轴错开，前面那片压住后面的。"""
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    rnd = random.Random(11)
    for cx, cy in ((9.6, 6.4), (8.0, 8.0), (6.4, 9.6)):
        _paint_slice(im, _slice_shape(cx, cy, 4.2, 3.0), rnd)
    return im


def slice_preview() -> Path:
    """单片和三片叠一起的对照。"""
    OUT.mkdir(parents=True, exist_ok=True)
    single, stack = sausage_slice(), sausage_stack()
    single.save(OUT / "sausage_slice_preview.png")
    stack.save(OUT / "sausage_stack_preview.png")
    sheet = Image.new("RGBA", (900, 470), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((30, 20), "火腿肠切片 · 单片 / 三片叠", font=font(25, True), fill=ink)
    draw.text((31, 60), "横截面是粉红色的肠体，外圈一圈深色肠衣，勾几颗脂肪点",
              font=font(17), fill=dim)
    size = 200
    for im, cx, name in ((single, 250, "单片"), (stack, 650, "三片叠一起")):
        x = cx - size // 2
        draw.rectangle((x - 4, 110, x + size + 3, 110 + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, 112, x + size + 1, 110 + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, 114))
        face = font(19)
        width = draw.textbbox((0, 0), name, font=face)[2]
        draw.text((cx - width / 2, 336), name, font=face, fill=ink)
        sheet.alpha_composite(im.resize((72, 72), Image.Resampling.NEAREST), (cx - 36, 372))
    draw.text((30, 440), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "slice_sheet.png"
    sheet.convert("RGB").save(path)
    return path


STACK_VARIANTS = [
    ("1  斜向错开", 7,
     [(9.6, 6.4, 4.2, 3.0, -45), (8.0, 8.0, 4.2, 3.0, -45), (6.4, 9.6, 4.2, 3.0, -45)]),
    ("2  上下摞起来", 13,
     [(8.0, 9.8, 4.4, 3.0, 0), (8.0, 8.0, 4.4, 3.0, 0), (8.0, 6.2, 4.4, 3.0, 0)]),
    ("3  扇形撒开", 17,
     [(6.8, 9.8, 4.2, 2.9, -74), (8.0, 8.0, 4.2, 2.9, -45), (9.2, 6.2, 4.2, 2.9, -16)]),
    ("4  品字形", 23,
     [(6.2, 9.4, 4.0, 2.8, -45), (9.8, 9.4, 4.0, 2.8, -45), (8.0, 6.6, 4.0, 2.8, -45)]),
    ("5  紧凑一沓", 29,
     [(8.9, 7.1, 4.5, 3.2, -45), (8.0, 8.0, 4.5, 3.2, -45), (7.1, 8.9, 4.5, 3.2, -45)]),
]


def stack_variant(index: int) -> Image.Image:
    """按 STACK_VARIANTS 里第 index 种方式叠片（列表从后往前画）。"""
    _, seed, layers = STACK_VARIANTS[index]
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    rnd = random.Random(seed)
    for cx, cy, rx, ry, deg in layers:
        _paint_slice(im, _slice_shape(cx, cy, rx, ry, deg), rnd)
    return im


def stacks_preview() -> Path:
    """五种叠放方式并排对照。"""
    OUT.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (900, 470), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((30, 20), "火腿肠切片 · 五种叠放方式", font=font(25, True), fill=ink)
    draw.text((31, 60), "都按 16×16 画，比较层次和辨识度；尚未写进游戏", font=font(16), fill=dim)
    size, step = 128, 170
    for i, (name, _, _) in enumerate(STACK_VARIANTS):
        im = stack_variant(i)
        im.save(OUT / f"stack_{i + 1}.png")
        x = 25 + i * step + (step - size) // 2
        draw.rectangle((x - 4, 118, x + size + 3, 118 + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, 120, x + size + 1, 118 + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, 122))
        face = font(15)
        width = draw.textbbox((0, 0), name, font=face)[2]
        mid = x + size / 2
        draw.text((mid - width / 2, 268), name, font=face, fill=ink)
        sheet.alpha_composite(im.resize((56, 56), Image.Resampling.NEAREST), (int(mid - 28), 300))
    draw.text((30, 440), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "stacks_sheet.png"
    sheet.convert("RGB").save(path)
    return path


HORIZONTAL_VARIANTS = [
    ("1  现在这版", 41, False,
     [(5.3, 8.0, 3.9, 2.8, -45), (8.0, 8.0, 3.9, 2.8, -45), (10.7, 8.0, 3.9, 2.8, -45)]),
    ("2  拉开一点", 42, False,
     [(4.6, 8.0, 3.7, 2.7, -45), (7.6, 8.0, 3.7, 2.7, -45), (10.6, 8.0, 3.7, 2.7, -45)]),
    ("3  压得更紧", 43, False,
     [(5.9, 8.0, 4.2, 3.0, -45), (8.0, 8.0, 4.2, 3.0, -45), (10.1, 8.0, 4.2, 3.0, -45)]),
    ("4  前大后小", 44, False,
     [(5.0, 8.0, 3.2, 2.4, -45), (8.0, 8.0, 3.6, 2.7, -45), (11.0, 8.0, 4.1, 3.1, -45)]),
    ("5  带厚度", 45, True,
     [(5.3, 8.0, 3.9, 2.8, -45), (8.0, 8.0, 3.9, 2.8, -45), (10.7, 8.0, 3.9, 2.8, -45)]),
]


def horizontal_variant(index: int) -> Image.Image:
    """斜片横排的第 index 种样式。"""
    _, seed, side, layers = HORIZONTAL_VARIANTS[index]
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    rnd = random.Random(seed)
    for cx, cy, rx, ry, deg in layers:
        _paint_slice(im, _slice_shape(cx, cy, rx, ry, deg), rnd, side)
    return im


def horizontal_preview() -> Path:
    """斜片横着排开的五种样式。"""
    OUT.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (900, 470), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((30, 20), "火腿肠切片 · 斜片横排五版", font=font(24, True), fill=ink)
    draw.text((31, 60), "三片斜放、沿水平排开，只改间距、大小和厚度", font=font(16), fill=dim)
    size, step = 128, 170
    for i, row in enumerate(HORIZONTAL_VARIANTS):
        name = row[0]
        im = horizontal_variant(i)
        im.save(OUT / f"horizontal_{i + 1}.png")
        x = 25 + i * step + (step - size) // 2
        draw.rectangle((x - 4, 118, x + size + 3, 118 + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, 120, x + size + 1, 118 + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, 122))
        face = font(15)
        width = draw.textbbox((0, 0), name, font=face)[2]
        mid = x + size / 2
        draw.text((mid - width / 2, 268), name, font=face, fill=ink)
        sheet.alpha_composite(im.resize((56, 56), Image.Resampling.NEAREST), (int(mid - 28), 300))
    draw.text((30, 440), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "horizontal_sheet.png"
    sheet.convert("RGB").save(path)
    return path


SPREADS = [
    ("1  大而斜", 6.0, 4.0, 2.0, 1.0),
    ("2  中而斜", 5.4, 3.8, 2.3, 1.0),
    ("3  上一版对照", 4.5, 4.3, 2.7, 1.5),
]


def spread_preview() -> Path:
    """片始终斜 45°、三片横向排开，只微调大小和高度。"""
    OUT.mkdir(parents=True, exist_ok=True)
    step = 170
    width = 25 + step * len(SPREADS) + 25
    sheet = Image.new("RGBA", (width, 470), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((30, 20), "火腿肠切片 · 找回斜度", font=font(24, True), fill=ink)
    draw.text((31, 60), "片保持 45°，靠把片本身拉长来变高；右边那版是做对照的拉伸版", font=font(15), fill=dim)
    size = 128
    for i, (name, rx, ry, gap, yscale) in enumerate(SPREADS):
        im = Image.new("RGBA", (16, 16), TRANSPARENT)
        rnd = random.Random(43)
        for dx in (-gap, 0.0, gap):
            _paint_slice(im, _slice_shape(8.0 + dx, 8.0, rx, ry, -45, yscale), rnd)
        im.save(OUT / f"spread_{i + 1}.png")
        x = 25 + i * step + (step - size) // 2
        draw.rectangle((x - 4, 118, x + size + 3, 118 + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, 120, x + size + 1, 118 + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, 122))
        face = font(15)
        width = draw.textbbox((0, 0), name, font=face)[2]
        mid = x + size / 2
        draw.text((mid - width / 2, 268), name, font=face, fill=ink)
        sheet.alpha_composite(im.resize((56, 56), Image.Resampling.NEAREST), (int(mid - 28), 300))
    draw.text((30, 440), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "spread_sheet.png"
    sheet.convert("RGB").save(path)
    return path


DIAGONALS = [
    ("1  右上到左下", [(10.2, 5.8, -45), (8.0, 8.0, -45), (5.8, 10.2, -45)]),
    ("2  左上到右下", [(5.8, 5.8, -45), (8.0, 8.0, -45), (10.2, 10.2, -45)]),
    ("3  斜着压紧", [(9.6, 6.4, -45), (8.0, 8.0, -45), (6.4, 9.6, -45)]),
    ("4  斜着散开", [(10.8, 5.2, -45), (8.0, 8.0, -45), (5.2, 10.8, -45)]),
    ("5  斜着扇开", [(7.2, 9.8, -64), (8.0, 8.0, -45), (8.8, 6.2, -26)]),
]


def diagonal_preview() -> Path:
    """加高后的斜片（垂直 ×1.5），比较几种斜着排列的方式。"""
    OUT.mkdir(parents=True, exist_ok=True)
    step = 170
    sheet = Image.new("RGBA", (25 + step * len(DIAGONALS) + 25, 470), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    draw.text((30, 20), "火腿肠切片 · 斜着摆五版", font=font(24, True), fill=ink)
    draw.text((31, 60), "片沿用高度 ×1.5 的加高形状，比较三片的斜向排列", font=font(16), fill=dim)
    size = 128
    for i, (name, centers) in enumerate(DIAGONALS):
        im = Image.new("RGBA", (16, 16), TRANSPARENT)
        rnd = random.Random(51)
        for cx, cy, deg in centers:
            _paint_slice(im, _slice_shape(cx, cy, 3.4, 3.2, deg, 1.5), rnd)
        im.save(OUT / f"diagonal_{i + 1}.png")
        x = 25 + i * step + (step - size) // 2
        draw.rectangle((x - 4, 118, x + size + 3, 118 + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, 120, x + size + 1, 118 + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, 122))
        face = font(15)
        width = draw.textbbox((0, 0), name, font=face)[2]
        mid = x + size / 2
        draw.text((mid - width / 2, 268), name, font=face, fill=ink)
        sheet.alpha_composite(im.resize((56, 56), Image.Resampling.NEAREST), (int(mid - 28), 300))
    draw.text((30, 440), "16×16 原版物品规格 · 造型预览，尚未写进游戏", font=font(15), fill=dim)
    path = OUT / "diagonal_sheet.png"
    sheet.convert("RGB").save(path)
    return path


def minced_pork() -> Image.Image:
    """低矮猪肉末，颜色参考现有猪肉丸；少量浅色脂肪粒，不画血液。"""
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(im)
    draw.polygon([(2, 9), (4, 6), (6, 6), (7, 5), (10, 6), (12, 7), (14, 10),
                  (13, 12), (10, 13), (5, 13), (2, 12)], fill=(232, 166, 153, 255))
    rnd = random.Random(29)
    src = im.copy()
    px = im.load()
    for y in range(16):
        for x in range(16):
            if not src.getpixel((x, y))[3]:
                continue
            edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16)
                       or not src.getpixel((x + dx, y + dy))[3]
                       for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)))
            if edge:
                color = (132, 78, 70) if y >= 10 else (166, 100, 89)
            elif y >= 11:
                color = rnd.choice(((194, 119, 106), (211, 138, 122), (232, 166, 153)))
            else:
                color = rnd.choice(((243, 190, 175), (232, 166, 153), (220, 147, 132), (251, 210, 188)))
            px[x, y] = color + (255,)
    # 低矮肉末的轮廓不变，只把图案放在物品格正中。
    box = im.getbbox()
    width, height = box[2] - box[0], box[3] - box[1]
    centered = Image.new("RGBA", (16, 16), TRANSPARENT)
    centered.paste(im.crop(box), ((16 - width) // 2, (16 - height) // 2))
    return centered


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    windows = Path("C:/Windows/Fonts") / ("msyhbd.ttc" if bold else "msyh.ttc")
    candidates = [windows, Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")]
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size)
    raise FileNotFoundError("预览标签需要微软雅黑或 Noto Sans CJK 字体")


def preview() -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (900, 650), (49, 49, 47, 255))
    draw = ImageDraw.Draw(sheet)
    ink, dim = (247, 240, 220), (192, 188, 176)
    title, text, small = font(25, True), font(18), font(15)

    def centered(x: int, y: int, label: str, face=text, fill=ink):
        width = draw.textbbox((0, 0), label, font=face)[2]
        draw.text((x - width / 2, y), label, font=face, fill=fill)

    def tile(im: Image.Image, x: int, y: int, size: int = 128):
        draw.rectangle((x - 4, y - 4, x + size + 3, y + size + 3), fill=(105, 105, 102))
        draw.rectangle((x - 2, y - 2, x + size + 1, y + size + 1), fill=(139, 139, 139))
        sheet.alpha_composite(im.resize((size, size), Image.Resampling.NEAREST), (x, y))

    draw.text((30, 18), "火腿肠 · 三种造型预览", font=title, fill=ink)
    draw.text((31, 57), "同一套 16×16 像素规格，只比较粗细和端头；尚未写进游戏", font=small, fill=dim)
    centers = (200, 460, 720)
    names = ("1  细长圆头", "2  粗一些、两端圆头", "3  两端收口")
    draw.text((25, 180), "生", font=title, fill=ink)
    draw.text((25, 362), "熟", font=title, fill=ink)
    for i, (style, cx, name) in enumerate(zip(STYLES, centers, names), start=1):
        centered(cx, 103, name)
        for cooked, y in ((False, 143), (True, 325)):
            im = sausage(style, cooked)
            im.save(OUT / f"option_{i}_{'cooked' if cooked else 'raw'}.png")
            tile(im, cx - 64, y)
    draw.line((30, 485, 870, 485), fill=(100, 96, 87), width=1)
    refs = [(minced_pork(), "猪肉末（中间原料）")]
    for filename, name in (("porkchop.png", "原版生猪排"), ("cooked_porkchop.png", "原版熟猪排")):
        refs.append((Image.open(VANILLA / filename).convert("RGBA"), name))
    for (im, name), cx in zip(refs, centers):
        tile(im, cx - 40, 510, 80)
        centered(cx, 606, name, small, dim)
    minced_pork().save(OUT / "minced_pork_preview.png")
    out = OUT / "sheet.png"
    sheet.convert("RGB").save(out)
    return out


DATA = MOD / "common/src/main/resources/data"
MODID = "orients_hotpot_chronicles"


def write_json(path: Path, data: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8"))


def install() -> None:
    """把已确认的包装造型和配方写入唯一资源根。"""
    roots = [MOD / "common/src/main/resources/assets" / MODID]
    for root in roots:
        for name, image in (("minced_pork", minced_pork()),
                            ("raw_ham_sausage", wrapped_sausage()),
                            ("ham_sausage", wrapped_sausage()),
                            ("ham_sausage_slice", ham_slice()),
                            ("cooked_ham_sausage_slice", cooked_ham_slice())):
            image.save(root / "textures/item" / f"{name}.png")
            write_json(root / "models/item" / f"{name}.json", {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{MODID}:item/{name}"},
            })
    rec = DATA / MODID / "recipe"
    write_json(rec / "minced_pork_from_stonecutting.json", {
        "type": "minecraft:stonecutting",
        "ingredient": {"item": "minecraft:porkchop"},
        "result": {"id": f"{MODID}:minced_pork", "count": 1},
    })
    write_json(rec / "raw_ham_sausage.json", {
        "type": "minecraft:crafting_shapeless", "category": "food",
        "ingredients": [
            {"item": f"{MODID}:minced_pork"},
            {"item": "minecraft:wheat"},
        ],
        "result": {"id": f"{MODID}:raw_ham_sausage", "count": 3},
    })
    # 生火腿肠 → 火腿肠；火腿肠片 → 熟火腿肠片（2026-10-29 用户：片还能再加热一次）
    for raw, cooked, base in ((f"{MODID}:raw_ham_sausage", f"{MODID}:ham_sausage", "ham_sausage"),
                              (f"{MODID}:ham_sausage_slice", f"{MODID}:cooked_ham_sausage_slice",
                               "cooked_ham_sausage_slice")):
        for kind, time, suffix in (("smelting", 100, ""), ("smoking", 50, "_from_smoking"),
                                   ("campfire_cooking", 300, "_from_campfire_cooking")):
            write_json(rec / f"{base}{suffix}.json", {
                "type": f"minecraft:{kind}", "category": "food",
                "ingredient": {"item": raw},
                "result": {"id": cooked},
                "experience": 0.35, "cookingtime": time,
            })
    # 改名前的旧 ID 配方要清掉，不然指向不存在的物品
    for suffix in ("", "_from_smoking", "_from_campfire_cooking"):
        stale = rec / f"cooked_ham_sausage{suffix}.json"
        if stale.exists():
            stale.unlink()
    # 火腿肠才能切片（用户 2026-10-29）：生火腿肠放不到砧板上，因为没有切石机配方也不在 cuttable 里。
    write_json(rec / "ham_sausage_slice_from_stonecutting.json", {
        "type": "minecraft:stonecutting",
        "ingredient": {"item": f"{MODID}:ham_sausage"},
        "result": {"id": f"{MODID}:ham_sausage_slice", "count": 3},
    })
    tag_path = DATA / MODID / "tags/item/cuttable.json"
    tag = json.loads(tag_path.read_text(encoding="utf-8"))
    for extra in ("minecraft:porkchop", f"{MODID}:ham_sausage"):
        if extra not in tag["values"]:
            tag["values"].append(extra)
    if f"{MODID}:cooked_ham_sausage" in tag["values"]:
        tag["values"].remove(f"{MODID}:cooked_ham_sausage")
    write_json(tag_path, tag)
    print("installed sausage resources and recipes")


if __name__ == "__main__":
    if "install" in sys.argv:
        install()
    elif "--peeled" in sys.argv:
        print(peeled_preview())
    elif "--slice" in sys.argv:
        print(slice_preview())
    elif "--stacks" in sys.argv:
        print(stacks_preview())
    elif "--horizontal" in sys.argv:
        print(horizontal_preview())
    elif "--spread" in sys.argv:
        print(spread_preview())
    elif "--diagonal" in sys.argv:
        print(diagonal_preview())
    elif "--plain" in sys.argv:
        print(preview())
    else:
        print(wrapped_preview())
