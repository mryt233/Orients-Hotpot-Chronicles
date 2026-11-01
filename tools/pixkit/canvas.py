"""在 PIL 图像上画像素。作者：mryt233

所有函数直接改传入的图（返回同一张图，方便链式）。画布大小不限，16×16 物品最常用。
"""
from PIL import Image

from .color import shade

N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
TRANSPARENT = (0, 0, 0, 0)


def blank(w=16, h=None, fill=TRANSPARENT):
    """新建 RGBA 画布，默认全透明。"""
    return Image.new("RGBA", (w, h or w), fill)


def outline_inner(im, color):
    """把不透明区域**最外一圈像素**改成描边色（形状不变大）。color 是 RGB。
    旧 gen_ingredients / gen_cutting / gen_meatballs 的写法。"""
    px = im.load()
    w, h = im.size
    solid = {(x, y) for y in range(h) for x in range(w) if px[x, y][3]}
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in N4):
            px[x, y] = tuple(color[:3]) + (255,)
    return im


def outline_outer(im, color):
    """在不透明区域**外面加一圈**描边（形状变大 1 像素）。color 是 RGBA。
    旧 gen_garden / gen_shrimp 的写法。"""
    src = im.copy()
    s, d = src.load(), im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if s[x, y][3] == 0 and any(0 <= x + dx < w and 0 <= y + dy < h and s[x + dx, y + dy][3] > 0
                                       for dx, dy in N4):
                d[x, y] = color
    return im


def sphere_level(x, y, cx, cy, r):
    """球面明暗分级：光从左上来，返回 0（最暗）～3（最亮）。旧 gen_garden / gen_shrimp 的 shade(x, y, ...)。"""
    lx, ly = (x - cx) / r, (y - cy) / r
    v = -0.75 * lx - 0.85 * ly
    edge = (lx * lx + ly * ly) ** 0.5
    if v > 0.55 and edge < 0.75:
        return 3
    if v > 0.0:
        return 2
    if v > -0.55 and edge < 0.95:
        return 1
    return 0


def speckle(im, rnd, p, k, fn=shade):
    """不透明像素以概率 p 乘上明暗系数 k（做气孔、颗粒）。rnd 是 random.Random，结果可复现。
    fn 选取整方式：color.shade（四舍五入，默认）或 color.shade_floor（向零取整）。"""
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if px[x, y][3] and rnd.random() < p:
                px[x, y] = fn(px[x, y][:3], k) + (px[x, y][3],)
    return im


def iso_cube(draw, cx, ty, w, h, top, left, right):
    """等角小方块（顶面菱形 + 左右两面），draw 是 ImageDraw.Draw。旧 gen_ingredients 的 cube。"""
    q = w / 4
    draw.polygon([(cx, ty), (cx + w / 2, ty + q), (cx, ty + 2 * q), (cx - w / 2, ty + q)], fill=top)
    draw.polygon([(cx - w / 2, ty + q), (cx, ty + 2 * q), (cx, ty + 2 * q + h), (cx - w / 2, ty + q + h)], fill=left)
    draw.polygon([(cx + w / 2, ty + q), (cx, ty + 2 * q), (cx, ty + 2 * q + h), (cx + w / 2, ty + q + h)], fill=right)


def from_rows(rows, palette, w=16, h=None):
    """用字符画画图：rows 每行一个字符串，palette 把字符映射成 RGBA（'.' 或没写的字符 = 透明）。
    例：from_rows([".XX.", "XXXX"], {"X": (226, 196, 110, 255)})"""
    im = blank(w, h)
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                px[x, y] = palette[ch]
    return im


def recolor_by_luma(im, colors):
    """按亮度把一张（通常是原版）贴图重新上色：colors 由暗到亮，亮度均分成 len(colors) 段。保留透明度。"""
    from .color import luma
    out = im.convert("RGBA").copy()
    px = out.load()
    w, h = out.size
    vals = [luma(px[x, y]) for y in range(h) for x in range(w) if px[x, y][3]]
    if not vals:
        return out
    lo, hi = min(vals), max(vals)
    n = len(colors)
    for y in range(h):
        for x in range(w):
            if px[x, y][3]:
                t = 0 if hi == lo else (luma(px[x, y]) - lo) / (hi - lo)
                px[x, y] = tuple(colors[min(n - 1, int(t * n))][:3]) + (px[x, y][3],)
    return out
