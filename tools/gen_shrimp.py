"""虾、虾仁、虾滑（2026-10-26 用户：加虾和虾滑；钓鱼得虾；带壳虾在砧板上用菜刀剥成虾仁（去壳去头），虾仁用擀面杖拍成虾滑）。

    python tools/gen_shrimp.py            # 出对比图 tools/out/shrimp/sheet.png（放大 10 倍，旁边放原版鳕鱼作参照）
    python tools/gen_shrimp.py install    # 写进两份资源：物品贴图 + 物品模型；再写数据：切石机、熔炉 / 烟熏炉 / 营火配方、可切标签
六样：shrimp / cooked_shrimp（整只带壳的基围虾）、peeled_shrimp / cooked_peeled_shrimp（虾仁，去壳去头）、
shrimp_paste / cooked_shrimp_paste（虾滑：一颗球，生的粉灰、熟的白里透粉）。手画像素，照原版食物：深色描边、3~4 档颜色、左上亮右下暗。
作者：mryt233
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
OUT = MOD / "tools/out/shrimp"
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures"
ROOTS = [MOD / "common/src/main/resources/assets" / MODID]
T = (0, 0, 0, 0)


def blank():
    return Image.new("RGBA", (16, 16), T)


def outline(im, color):
    src = im.copy()
    s, d = src.load(), im.load()
    for y in range(16):
        for x in range(16):
            if s[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and s[x + dx, y + dy][3] > 0
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                d[x, y] = color
    return im


def c(*rgb):
    return tuple(rgb) + (255,)


# 生虾：青灰半透明；熟虾：橙红、肚子一道白
# 基围虾：生的青灰带一点绿、半透明感，壳上有深色小斑；熟的通体橙红
RAW = {"lo": c(104, 118, 112), "mid": c(146, 160, 152), "hi": c(188, 200, 192), "top": c(218, 226, 218),
       "band": c(112, 124, 118), "line": c(58, 66, 64), "tail": c(96, 118, 140), "eye": c(24, 24, 26),
       "speck": c(88, 82, 68), "leg": c(196, 204, 196), "tip": c(150, 176, 200)}
COOKED = {"lo": c(198, 66, 36), "mid": c(232, 106, 58), "hi": c(248, 150, 96), "top": c(254, 196, 150),
          "band": c(190, 60, 34), "line": c(112, 32, 16), "tail": c(222, 80, 40), "eye": c(40, 20, 16),
          "speck": c(204, 72, 40), "leg": c(250, 226, 190), "tip": c(250, 140, 110)}
MEAT_RAW = {"lo": c(184, 178, 176), "mid": c(208, 202, 198), "hi": c(228, 222, 218), "top": c(242, 238, 234),
            "band": c(190, 170, 170), "line": c(120, 108, 108), "vein": c(150, 120, 120)}
MEAT_COOKED = {"lo": c(232, 150, 120), "mid": c(250, 236, 228), "hi": c(255, 246, 240), "top": c(255, 252, 248),
               "band": c(240, 128, 92), "line": c(150, 70, 46), "vein": c(236, 110, 76)}


def body(px, pal, cx, cy, r, a0, a1, w0, w1, bands=True, outer_band=None):
    """沿一段圆弧画虾身：从 a0（头那端）到 a1（尾那端），粗细从 w0 渐变到 w1。
    明暗按离弧心远近（外侧背亮、内侧肚子暗）+ 左上来光；每隔一段画一道壳节（band）。"""
    n = 80
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0 + (a1 - a0) * t)
        bx, by = cx + r * math.cos(a), cy + r * math.sin(a)
        w = w0 + (w1 - w0) * t
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - bx, y + 0.5 - by
                if dx * dx + dy * dy <= w * w:
                    # 外侧（背）还是内侧（肚子）：投到半径方向上
                    radial = (dx * math.cos(a) + dy * math.sin(a)) / w
                    light = -0.6 * dx / w - 0.7 * dy / w
                    v = 0.55 * radial + 0.45 * light
                    k = "top" if v > 0.45 else "hi" if v > 0.05 else "mid" if v > -0.45 else "lo"
                    seg = (a0 + (a1 - a0) * t)
                    if bands and int(abs(seg - a0)) % 34 in (0, 1) and t > 0.12 and radial > -0.3:
                        k = "band"
                    if outer_band and radial > 0.35:
                        k = outer_band
                    if px[x, y][3] == 0 or t < 0.999:
                        px[x, y] = pal[k]


def tail_fan(px, pal, x, y, dirx, diry):
    """尾巴：末端张开的一小把扇子（3 像素宽）。"""
    nx, ny = -diry, dirx
    for k in (-1, 0, 1):
        for s in (1, 2):
            tx, ty = int(round(x + dirx * s + nx * k * (0.5 + s * 0.6))), int(round(y + diry * s + ny * k * (0.5 + s * 0.6)))
            if 0 <= tx < 16 and 0 <= ty < 16:
                px[tx, ty] = pal["tail"]


# 基围虾侧面（2026-10-26 用户定的第二版身子 + 「头做成修长、尖的」，再按用户画的线「尖尖往下收」）：头在右边、往右下拉长收成尖，背拱起来往左，
# 尾巴往下卷；沿这条线一串圆拼出虾身（头尖 → 尾），每点 (x, y, 粗细)；弯曲中心在下面
SHRIMP_PATH = [(15.4, 13.0, 0.4), (14.5, 11.5, 0.9), (13.3, 9.7, 1.4), (11.7, 7.8, 1.9), (9.7, 6.4, 2.1),
               (7.5, 5.9, 2.0), (5.4, 6.2, 1.9), (3.6, 7.2, 1.7), (2.2, 8.9, 1.5), (1.7, 10.8, 1.3),
               (2.1, 12.4, 1.1), (3.1, 13.6, 0.85), (4.4, 14.1, 0.6)]
BEND = (7.0, 11.2)


def shrimp(pal):
    """整只带壳虾，照基围虾（用户：主要特征是头尖、修长，有须）：头在右边拉长收成一根尖刺、黑眼睛，
    背拱起来、一节节壳带小斑点，尾巴往下卷、末端尾扇，肚子下一排小脚，长须从头顶顺着背往后甩。"""
    im = blank()
    px = im.load()
    pts = []
    for (x0, y0, w0), (x1, y1, w1) in zip(SHRIMP_PATH, SHRIMP_PATH[1:]):
        for i in range(12):
            t = i / 12
            pts.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w0 + (w1 - w0) * t))
    pts.append(SHRIMP_PATH[-1])
    for y in range(16):
        for x in range(16):
            fx, fy = x + 0.5, y + 0.5
            hit = [(bx, by, w) for bx, by, w in pts if (fx - bx) ** 2 + (fy - by) ** 2 <= w * w]
            if not hit:
                continue
            bx, by, w = min(hit, key=lambda q: (fx - q[0]) ** 2 + (fy - q[1]) ** 2)
            # 背（离弯曲中心远的一侧）亮、肚子暗，再加左上来光
            ox, oy = bx - BEND[0], by - BEND[1]
            n = math.hypot(ox, oy) or 1
            ww = max(w, 0.8)
            radial = ((fx - bx) * ox + (fy - by) * oy) / (n * ww)
            light = (-(fx - bx) * 0.6 - (fy - by) * 0.8) / ww
            v = 0.6 * radial + 0.4 * light
            px[x, y] = pal["top" if v > 0.5 else "hi" if v > 0.1 else "mid" if v > -0.4 else "lo"]
    # 壳节：身子那一段背上画几道深线（头那段不画）
    for bx, by, w in SHRIMP_PATH[4:11]:
        ox, oy = bx - BEND[0], by - BEND[1]
        n = math.hypot(ox, oy) or 1
        for d in (0.2, 0.9, 1.6):
            x, y = int(bx + ox / n * d), int(by + oy / n * d)
            if 0 <= x < 16 and 0 <= y < 16 and px[x, y][3]:
                px[x, y] = pal["band"]
    for x, y in [(8, 4), (4, 5), (1, 9)]:          # 小斑点
        if px[x, y][3]:
            px[x, y] = pal["speck"]
    outline(im, pal["line"])
    # 尾扇：身子末端往右张开成两瓣（上瓣往右上、下瓣往右下，中间一道缺口），根部深、瓣尖浅；画在描边之后，自己再描一圈
    fan = {(5, 14): "lo", (6, 13): "tail", (6, 14): "lo", (6, 15): "tail", (7, 12): "tip", (7, 13): "tail",
           (7, 15): "tail", (8, 12): "tip", (8, 15): "tip"}
    for (x, y), k in fan.items():
        px[x, y] = pal[k] if k in pal else pal["hi"]
    for x, y in [(9, 12), (8, 11), (7, 11), (9, 15), (8, 14), (6, 12)]:
        if px[x, y][3] == 0:
            px[x, y] = pal["line"]
    for x, y in [(10, 10), (8, 9), (6, 9), (4, 10)]:   # 肚子下的小脚
        if px[x, y][3] == 0:
            px[x, y] = pal["band"]
    px[12, 8] = pal["eye"]  # 头拉长后眼睛还在头根上
    # 长须：从头顶起，贴着背上方一格往后甩，一直甩到尾巴上头
    for x in range(12, 0, -1):
        top = next((y for y in range(16) if px[x, y][3]), 16)
        y = top - 2 if x < 12 else top - 1
        if 0 <= y < 16 and px[x, y][3] == 0:
            px[x, y] = pal["band"]
    return im


def shrimp_meat(pal, cooked):
    """虾仁：去壳去头，C 形的一截肉；生的灰白、背上一道淡淡的虾线，熟的白肉、背上一道道橙红纹。"""
    im = blank()
    px = im.load()
    cx, cy, r = 8.0, 8.4, 4.0
    body(px, pal, cx, cy, r, -40, 170, 2.6, 1.2, bands=cooked, outer_band=None)
    if cooked:
        # 熟虾仁背上的橙红纹：外侧一圈染红
        for y in range(16):
            for x in range(16):
                if px[x, y][3]:
                    dx, dy = x + 0.5 - cx, y + 0.5 - cy
                    d = math.hypot(dx, dy)
                    ang = math.degrees(math.atan2(dy, dx))
                    if d > r + 0.6 and int((ang + 400) / 22) % 2 == 0:
                        px[x, y] = pal["band"]
                    elif d > r + 0.6:
                        px[x, y] = pal["lo"]
    else:
        # 背上的虾线（去了线的浅痕）
        for deg in range(-30, 160, 6):
            a = math.radians(deg)
            x, y = int(cx + (r + 1.4) * math.cos(a)), int(cy + (r + 1.4) * math.sin(a))
            if px[x, y][3]:
                px[x, y] = pal["vein"]
    outline(im, pal["line"])
    return im


# 2026-10-26 用户：生虾滑再灰一些、熟的再粉红一点
PASTE_RAW = [c(140, 136, 136), c(168, 164, 163), c(192, 188, 187), c(212, 209, 208), c(228, 226, 225)]
PASTE_RAW_LINE = c(96, 90, 90)
PASTE_COOKED = [c(210, 136, 128), c(234, 168, 158), c(246, 194, 186), c(252, 214, 208), c(255, 234, 230)]
PASTE_COOKED_LINE = c(150, 84, 76)
PINK = c(240, 150, 130)


def shade(x, y, cx, cy, r):
    lx, ly = (x - cx) / r, (y - cy) / r
    v = -0.75 * lx - 0.85 * ly
    e = (lx * lx + ly * ly) ** 0.5
    return 3 if v > 0.55 and e < 0.75 else 2 if v > 0 else 1 if v > -0.55 and e < 0.95 else 0


def paste_ball(pal, line, specks, seed):
    """虾滑丸：一颗圆球（2026-10-26 用户：虾滑做成球形），球面明暗 + 表面几粒虾肉。"""
    rnd = random.Random(seed)
    im = blank()
    px = im.load()
    cx, cy, r = 8.0, 8.5, 5.6
    for y in range(16):
        for x in range(16):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                px[x, y] = pal[shade(x, y, cx - 0.5, cy - 0.5, r) + 1]
    # 一点高光
    px[5, 5], px[6, 5], px[5, 6] = pal[4], pal[4], pal[4]
    for _ in range(9):
        x, y = rnd.randint(4, 12), rnd.randint(5, 13)
        if px[x, y][3] and px[x, y] != pal[4]:
            px[x, y] = rnd.choice(specks)
    outline(im, line)
    return im


def shrimp_paste():
    """生虾滑：一颗粉灰色的虾泥球，带几粒粉色虾肉。"""
    return paste_ball(PASTE_RAW, PASTE_RAW_LINE, [c(206, 168, 164), c(180, 160, 158)], 11)


def cooked_shrimp_paste():
    """熟虾滑：煮好的一颗，白里透粉，带几粒橙红虾肉。"""
    return paste_ball(PASTE_COOKED, PASTE_COOKED_LINE, [c(238, 120, 100), c(226, 100, 80)], 5)


ITEMS = {
    "shrimp": lambda: shrimp(RAW),
    "cooked_shrimp": lambda: shrimp(COOKED),
    "peeled_shrimp": lambda: shrimp_meat(MEAT_RAW, False),
    "cooked_peeled_shrimp": lambda: shrimp_meat(MEAT_COOKED, True),
    "shrimp_paste": shrimp_paste,
    "cooked_shrimp_paste": cooked_shrimp_paste,
}


def sheet():
    OUT.mkdir(parents=True, exist_ok=True)
    tiles = [fn() for fn in ITEMS.values()] + [Image.open(VANILLA / "item/cod.png").convert("RGBA")]
    s = 10
    out = Image.new("RGBA", (len(tiles) * (16 * s + 12) + 12, 16 * s + 24), (139, 139, 139, 255))
    for i, t in enumerate(tiles):
        out.alpha_composite(t.resize((16 * s, 16 * s), Image.NEAREST), (12 + i * (16 * s + 12), 12))
    out.save(OUT / "sheet.png")
    print("sheet:", OUT / "sheet.png")


DATA = MOD / "common/src/main/resources/data"


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def install():
    for root in ROOTS:
        for name, fn in ITEMS.items():
            p = root / "textures/item" / (name + ".png")
            p.parent.mkdir(parents=True, exist_ok=True)
            fn().save(p)
            write_json(root / "models/item" / (name + ".json"),
                       {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{name}"}})
    rec = DATA / MODID / "recipe"
    # 剥虾：切石机一只出一个虾仁；砧板上菜刀切照切石机配方（CuttingBoardBlock.cutResult），所以虾要进可切标签
    write_json(rec / "peeled_shrimp.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"{MODID}:shrimp"},
                                            "result": {"id": f"{MODID}:peeled_shrimp", "count": 1}})
    # 烤熟：时间同丸子（小件，熔炉 5 秒、烟熏炉 2.5 秒、营火 15 秒）；锅里按烟熏炉的时间涮
    for raw in ("shrimp", "peeled_shrimp", "shrimp_paste"):
        for kind, suffix, time in (("smelting", "", 100), ("smoking", "_from_smoking", 50), ("campfire_cooking", "_from_campfire_cooking", 300)):
            write_json(rec / f"cooked_{raw}{suffix}.json", {"type": f"minecraft:{kind}", "category": "food",
                       "ingredient": {"item": f"{MODID}:{raw}"}, "result": {"id": f"{MODID}:cooked_{raw}"},
                       "experience": 0.35, "cookingtime": time})
    f = DATA / MODID / "tags/item/cuttable.json"
    tag = json.loads(f.read_text(encoding="utf-8"))
    if f"{MODID}:shrimp" not in tag["values"]:
        tag["values"].append(f"{MODID}:shrimp")
    write_json(f, tag)
    print("installed")


if __name__ == "__main__":
    sheet()
    if "install" in sys.argv:
        install()
