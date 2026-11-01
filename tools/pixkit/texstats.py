"""贴图统计：色板、明暗、描边、占位、半透明。原版参考库（vanilla_ref.py）和贴图自检（check_textures.py）共用。
作者：mryt233（2026-11-01）
"""
import colorsys
from collections import Counter

from PIL import Image

from .color import luma

N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def first_frame(im):
    """动画贴图（竖叠多帧）只取第一帧；不是动画就原样返回。"""
    w, h = im.size
    if h > w and h % w == 0:
        return im.crop((0, 0, w, w))
    return im


def hls(c):
    h, l, s = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
    return h * 360, s, l


def _same_hue(c, mh, ms):
    h, s, _ = hls(c)
    if ms < 0.08:                      # 主色几乎是灰的：同组 = 也是灰的
        return s < 0.15
    return s >= 0.08 and min(abs(h - mh), 360 - abs(h - mh)) < 25


def stats(im, top=12):
    """统计一张贴图（动画只看第一帧）。im 可以是路径或 PIL 图。字段：
    size 原尺寸、frames 帧数、opaque 不透明像素数、fill 填充率、semi 半透明像素数、colors 颜色数、
    palette 前 top 种颜色（暗→亮，[颜色, 像素数]）、main 主色、hue 主色相、luma 亮度范围、contrast 对比度、
    bbox 可见范围、center 可见范围中心偏离画布中心（像素，+ 为右 / 下）、
    outline {edge_ratio 边缘与内部亮度比, dark 是否深色描边}、shades 主色相那组有几级明暗。"""
    if not isinstance(im, Image.Image):
        im = Image.open(im)
    raw = im.convert("RGBA")
    w, h = raw.size
    frames = h // w if h > w and h % w == 0 else 1
    im = first_frame(raw)
    w, h = im.size
    px = im.load()
    opaque = [(x, y) for y in range(h) for x in range(w) if px[x, y][3] > 0]
    semi = sum(1 for (x, y) in opaque if px[x, y][3] < 255)
    s = {"size": [raw.size[0], raw.size[1]], "frames": frames, "opaque": len(opaque),
         "fill": round(len(opaque) / (w * h), 3), "semi": semi}
    if not opaque:
        s.update(colors=0, palette=[], main=None, hue=None, luma=[0, 0], contrast=0, bbox=None,
                 center=[0, 0], outline=None, shades=0)
        return s
    cnt = Counter(px[x, y][:3] for (x, y) in opaque)
    xs = [x for x, _ in opaque]
    ys = [y for _, y in opaque]
    lumas = [luma(c) for c in cnt]
    main = cnt.most_common(1)[0][0]
    mh, ms, _ = hls(main)
    solid = set(opaque)
    edge = {p for p in opaque if any((p[0] + dx, p[1] + dy) not in solid for dx, dy in N4)}
    inner = [p for p in opaque if p not in edge]
    el = sum(luma(px[p]) for p in edge) / len(edge)
    il = sum(luma(px[p]) for p in inner) / len(inner) if inner else el
    ratio = round(el / il, 3) if il else 1.0
    s.update(
        colors=len(cnt),
        palette=[[list(c), n] for c, n in sorted(cnt.most_common(top), key=lambda kv: luma(kv[0]))],
        main=list(main),
        hue=round(mh),
        luma=[round(min(lumas)), round(max(lumas))],
        contrast=round(max(lumas) - min(lumas)),
        bbox=[min(xs), min(ys), max(xs) + 1, max(ys) + 1],
        center=[round((min(xs) + max(xs) + 1) / 2 - w / 2, 2), round((min(ys) + max(ys) + 1) / 2 - h / 2, 2)],
        outline={"edge_ratio": ratio, "dark": ratio < 0.75},
        shades=sum(1 for c in cnt if _same_hue(c, mh, ms)),
    )
    return s
