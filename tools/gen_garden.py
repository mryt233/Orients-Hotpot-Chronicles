"""橙子、大蒜、蒜泥、熟海带（2026-10-25 用户：橙子饮料要有原料；大蒜 + 蒜泥进蘸料台）；大蒜作物画法留着（用户：作物先保留，install crop 才装）。
辣椒（2026-11-05，r136a）：红汤辣度系统的调味料，打草掉，拿着右键红汤锅加辣。

    python tools/gen_garden.py            # 出对比图 tools/out/garden/sheet.png（放大 8 倍，旁边放原版苹果 / 胡萝卜作参照）
    python tools/gen_garden.py install    # 写进两份资源：物品贴图 + 物品模型 + 作物方块贴图
手画像素，照原版食物的画法：深色描边、3~4 档颜色、左上亮右下暗。
作者：mryt233
"""
import json
import sys
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
OUT = MOD / "tools/out/garden"
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures"
ROOTS = [MOD / "common/src/main/resources/assets" / MODID]

T = (0, 0, 0, 0)


def blank():
    return Image.new("RGBA", (16, 16), T)


def outline(im, color):
    """不透明像素外面描一圈（原版物品的画法）。"""
    src = im.copy()
    s, d = src.load(), im.load()
    for y in range(16):
        for x in range(16):
            if s[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and s[x + dx, y + dy][3] > 0
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                d[x, y] = color
    return im


def shade(x, y, cx, cy, r):
    """球面明暗：左上亮、右下暗，返回 0（最暗）~ 3（最亮）。"""
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


# ------------------------------------------------------------------ 橙子
ORANGE = [(196, 92, 10, 255), (232, 128, 22, 255), (250, 160, 40, 255), (255, 200, 96, 255)]
ORANGE_OUT = (110, 50, 8, 255)
LEAF = [(52, 104, 26, 255), (86, 148, 40, 255)]
STEM_C = (92, 64, 30, 255)


def orange():
    im = blank()
    px = im.load()
    cx, cy, r = 7.5, 9.0, 5.6
    for y in range(16):
        for x in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px[x, y] = ORANGE[shade(x, y, cx, cy, r)]
    # 果皮上的小点（橙子皮的毛孔）
    for x, y in [(5, 8), (9, 11), (6, 12), (10, 8), (8, 6)]:
        if px[x, y][3]:
            px[x, y] = ORANGE[max(0, [o[:3] for o in ORANGE].index(px[x, y][:3]) - 1)]
    outline(im, ORANGE_OUT)
    # 顶上的果蒂和一片叶子
    px[7, 3] = STEM_C
    px[8, 2], px[9, 2], px[10, 1] = LEAF[1], LEAF[1], LEAF[1]
    px[9, 3], px[10, 2], px[11, 1] = LEAF[0], LEAF[0], LEAF[0]
    return im


# ------------------------------------------------------------------ 大蒜
GARLIC = [(176, 160, 150, 255), (214, 204, 192, 255), (238, 232, 222, 255), (252, 250, 244, 255)]
GARLIC_OUT = (98, 82, 80, 255)
PURPLE = (150, 96, 136, 255)
ROOT = (170, 150, 112, 255)


SEAM_PURPLE = (160, 104, 140, 255)   # 饱满大蒜瓣缝底部的紫


def garlic():
    """一头饱满的蒜（2026-10-26 用户：要饱满一点，看了预览选这版；调整）：圆鼓鼓的蒜头分 4 瓣，每瓣自己有明暗、
    瓣缝深一点、靠底带紫（紫皮蒜），顶上细细的收口，底下一撮须根。"""
    im = blank()
    px = im.load()
    cx, cy, rx, ry = 8.0, 9.0, 6.6, 5.0
    lobes = [-1.0, -0.5, 0.0, 0.5, 1.0]          # 瓣的分界（u 坐标）：4 瓣
    for y in range(16):
        for x in range(16):
            fx, fy = x + 0.5, y + 0.5
            t = (fy - cy) / ry
            if abs(t) <= 1:
                w = rx * (1 - t * t) ** 0.5
                u = (fx - cx) / w if w > 0.3 else 0
                if abs(u) <= 1:
                    k = min(3, int((u + 1) / 0.5))            # 第几瓣
                    lc = cx + w * (lobes[k] + lobes[k + 1]) / 2  # 这一瓣的中线
                    lvl = shade(x, y, lc - 0.6, cy - 0.8, 3.4)    # 每瓣自己的明暗：鼓起来
                    lvl = max(0, min(3, lvl + (1 if t < -0.2 else 0) - (1 if t > 0.6 else 0)))
                    edge = min(abs(u - lobes[k]), abs(u - lobes[k + 1])) * w
                    if edge < 0.5 and 0 < k + (u > lobes[k] + 0.25) and abs(u) < 0.9 and t > -0.75:
                        px[x, y] = SEAM_PURPLE if t > 0.45 else GARLIC[0]
                    else:
                        px[x, y] = GARLIC[lvl]
    # 顶上的收口（细尖）
    for y, half in ((1, 0.5), (2, 0.5), (3, 1.0), (4, 1.5)):
        for x in range(16):
            if abs(x + 0.5 - cx) <= half and not px[x, y][3]:
                px[x, y] = GARLIC[2] if x + 0.5 < cx else GARLIC[1]
    outline(im, GARLIC_OUT)
    for x in (6, 8, 9, 11):
        px[x, 15] = ROOT
    return im


# 蒜泥两套颜色（2026-10-26 用户照两张参考图选的）：生蒜末用在蒜泥上；金黄的「黄金蒜」用户说保留，还没做成物品
MINCED = [(176, 150, 86, 255), (212, 188, 118, 255), (232, 214, 150, 255), (244, 232, 186, 255), (252, 248, 222, 255)]
MINCED_OUT = (128, 100, 52, 255)
GOLDEN = [(150, 92, 30, 255), (190, 128, 50, 255), (220, 160, 76, 255), (240, 192, 110, 255), (252, 226, 160, 255)]
GOLDEN_OUT = (96, 56, 16, 255)


def minced_heap(pal, line, seed=3):
    """一小堆剁碎的蒜：堆成小山，表面一粒粒的碎块（2×1 / 1×2 / 2×2 的亮面，右下贴一道深缝）。"""
    import random
    rnd = random.Random(seed)
    im = blank()
    px = im.load()
    cx, cy = 8.0, 11.0
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - cx) / 6.6, (y + 0.5 - cy) / 4.6
            top = y + 0.5 >= 4.2 + 2.2 * abs(x + 0.5 - cx) / 6.6 * 2.4 - 0.2   # 山形的上沿
            if dx * dx + max(0, dy) ** 2 <= 1 and top and y <= 14:
                px[x, y] = pal[shade(x, y, 7.0, 9.0, 6.4) + 1]
    for _ in range(34):
        x, y = rnd.randint(2, 13), rnd.randint(5, 13)
        shape = rnd.choice([[(0, 0), (1, 0)], [(0, 0), (0, 1)], [(0, 0), (1, 0), (0, 1), (1, 1)], [(0, 0)]])
        cells = [(x + a, y + b) for a, b in shape]
        if all(0 <= cx_ < 16 and 0 <= cy_ < 16 and px[cx_, cy_][3] for cx_, cy_ in cells):
            i = pal.index(px[x, y]) if px[x, y] in pal else 2
            for c in cells:
                px[c] = pal[min(4, i + 1)]
            for a, b in shape:
                d = (x + a + 1, y + b + 1)
                if 0 <= d[0] < 16 and 0 <= d[1] < 16 and px[d][3] and d not in cells:
                    px[d] = pal[max(0, i - 2)]
    outline(im, line)
    return im


def garlic_paste():
    """蒜泥：一小堆米黄色的生蒜末（照用户的参考照片）。"""
    return minced_heap(MINCED, MINCED_OUT)


def golden_garlic():
    """黄金蒜（炸蒜粒）：同样一小堆，金黄色。用户 2026-10-26 说保留，还没有这个物品，不安装。"""
    return minced_heap(GOLDEN, GOLDEN_OUT)


# ------------------------------------------------------------------ 大蒜作物（4 个阶段，照原版作物：交叉面片、透明底）
SHOOT = [(46, 98, 30, 255), (74, 136, 42, 255), (110, 172, 64, 255)]


def garlic_stage(stage):
    im = blank()
    px = im.load()
    # 每株几根细长的蒜苗叶，阶段越大越高、越多
    height = [4, 7, 11, 13][stage]
    stems = [(3, 0), (7, 1), (11, 0)] if stage < 2 else [(2, 0), (5, 1), (8, 0), (11, 1), (13, 0)]
    for i, (x, lean) in enumerate(stems):
        h = height - (i % 2) * 2
        for k in range(h):
            y = 15 - k
            xx = x + (lean if k > h // 2 else 0)
            if 0 <= xx < 16 and 0 <= y < 16:
                px[xx, y] = SHOOT[2] if k > h * 0.7 else (SHOOT[1] if k > h * 0.3 else SHOOT[0])
    if stage == 3:
        # 成熟：地面上露出几个白色蒜头
        for bx in (3, 9):
            for dx, dy, c in [(0, 0, GARLIC[2]), (1, 0, GARLIC[3]), (2, 0, GARLIC[1]),
                              (0, 1, GARLIC[1]), (1, 1, GARLIC[2]), (2, 1, GARLIC[0])]:
                px[bx + dx, 13 + dy] = c
            px[bx + 1, 12] = PURPLE
    return im


# ------------------------------------------------------------------ 熟海带（2026-10-25 用户：海带在火锅里煮出熟海带）
KELP = [(30, 70, 34, 255), (46, 98, 44, 255), (70, 128, 56, 255), (118, 170, 92, 255)]
KELP_OUT = (18, 44, 22, 255)


KELP_SCALE = 0.75   # 2026-10-26 用户：熟海带缩小一点、样子不变：整条按这个比例往中心缩


def cooked_kelp():
    """煮熟的海带：一条宽宽的深绿色海带片斜着放，边上起伏（波浪边），中间一道亮光（煮过发亮），比原版海带深、比干海带绿。"""
    im = blank()
    px = im.load()
    import math
    for y in range(16):
        for x in range(16):
            # 先把像素坐标按 KELP_SCALE 往中心放大（= 图案缩小），再沿左下→右上的对角线：u = 沿带子方向，v = 离中线的距离
            sx, sy = 7.5 + (x - 7.5) / KELP_SCALE, 7.5 + (y - 7.5) / KELP_SCALE
            u = (sx + (15 - sy)) / 2.0
            v = (sx - (15 - sy)) / 1.414
            wave = 0.9 * math.sin(u * 1.3)
            half = 2.6 + 0.6 * math.sin(u * 2.1 + 1.0)
            if 1.5 <= u <= 13.5 and abs(v - wave) <= half:
                t = (v - wave) / half          # -1（上边）..1（下边）
                c = KELP[3] if -0.25 <= t <= 0.05 and int(u) % 4 != 0 else                     KELP[2] if t < -0.25 else (KELP[1] if t < 0.6 else KELP[0])
                px[x, y] = c
    outline(im, KELP_OUT)
    return im


# ------------------------------------------------------------------ 番茄、锅底料（2026-10-25：番茄锅底、三鲜锅底）
TOMATO = [(150, 22, 18, 255), (200, 40, 28, 255), (232, 72, 48, 255), (255, 150, 120, 255)]
TOMATO_OUT = (90, 14, 10, 255)


def tomato():
    """番茄：扁一点的红圆球，顶上一个绿色的星形蒂。"""
    im = blank()
    px = im.load()
    cx, cy = 7.5, 9.0
    for y in range(16):
        for x in range(16):
            if ((x - cx) / 6.0) ** 2 + ((y - cy) / 5.2) ** 2 <= 1:
                px[x, y] = TOMATO[shade(x, y, cx, cy, 6.0)]
    outline(im, TOMATO_OUT)
    for x, y in [(6, 4), (7, 4), (8, 4), (9, 4), (7, 3), (5, 5), (10, 5), (7, 2)]:
        px[x, y] = LEAF[1] if y < 4 else LEAF[0]
    return im


# ------------------------------------------------------------------ 辣椒（2026-11-05，r136a：红汤辣度系统的调味料）
CHILI = [(146, 16, 12, 255), (188, 28, 20, 255), (220, 52, 32, 255), (242, 96, 66, 255)]
CHILI_OUT = (78, 10, 8, 255)
CHILI_STEM = [(48, 96, 30, 255), (74, 132, 44, 255)]


def chili_pepper():
    """1 号候选（默认）：一根斜放弯钩鲜红朝天椒（16×16，原版像素风）。
    从左上果蒂斜向右下再微微往上翘尖，修长利落（不会像草莓），居中对齐，深红描边 + 左上高光 + 翠绿果蒂与弯柄。"""
    im = blank()
    px = im.load()
    # 用中心线采样点画一根修长弯曲的红椒（果蒂在左上 (5,4)，椒身弯向右下，尖端在 (13,12)）
    import math
    for y in range(16):
        for x in range(16):
            fx, fy = x + 0.5, y + 0.5
            # 参数 t in [0, 1] 沿椒身弧线
            best_t, best_d = 0.0, 99.0
            for i in range(41):
                t = i / 40.0
                # 弧线：从 (5.2, 4.8) 弯向 (8.2, 9.6) 再收尖到 (12.8, 12.2)
                ax = 5.2 + 4.8 * t + 2.8 * (t ** 1.6)
                ay = 4.8 + 9.2 * t - 1.8 * (t ** 2.2)
                d = math.hypot(fx - ax, fy - ay)
                if d < best_d:
                    best_d, best_t = d, t
            # 半径从果肩 2.15 收细到椒尖 0.55
            radius = 2.15 * (1.0 - 0.74 * (best_t ** 0.85))
            if best_t < 0.08:
                radius *= 0.75 + 0.25 * (best_t / 0.08)
            if best_d <= radius:
                # 左上侧受光亮，右下侧背光暗
                light = (fx - (5.2 + 7.6 * best_t)) * 0.45 + (fy - (4.8 + 7.4 * best_t)) * 0.55
                lvl = 2 if light < -0.25 else (1 if light < 0.45 else 0)
                if -0.95 < light < -0.35 and 0.12 < best_t < 0.68:
                    lvl = 3
                px[x, y] = CHILI[lvl]
    outline(im, CHILI_OUT)
    # 翠绿果萼包住左上果肩 + 弯向上的小绿柄
    for x, y in [(4, 4), (5, 4), (6, 4), (4, 5), (5, 5)]:
        px[x, y] = CHILI_STEM[0]
    for x, y in [(3, 3), (4, 3), (3, 2), (4, 2)]:
        px[x, y] = CHILI_STEM[1]
    for x, y in [(2, 3), (3, 4), (5, 3)]:
        px[x, y] = (30, 62, 18, 255)
    return im


def chili_pepper_v2():
    """2 号候选：竖向微弯的饱满牛角椒（果蒂在右上，椒尖朝左下弯）。"""
    im = blank()
    px = im.load()
    import math
    for y in range(16):
        for x in range(16):
            fx, fy = x + 0.5, y + 0.5
            best_t, best_d = 0.0, 99.0
            for i in range(41):
                t = i / 40.0
                ax = 9.8 - 2.2 * t - 2.6 * (t ** 1.8)
                ay = 4.5 + 8.8 * t
                d = math.hypot(fx - ax, fy - ay)
                if d < best_d:
                    best_d, best_t = d, t
            radius = 2.35 * (1.0 - 0.75 * (best_t ** 0.9))
            if best_d <= radius:
                light = (fx - (9.8 - 4.8 * best_t)) * 0.6 + (fy - (4.5 + 8.8 * best_t)) * 0.3
                lvl = 2 if light < -0.2 else (1 if light < 0.5 else 0)
                if -1.0 < light < -0.35 and 0.12 < best_t < 0.65:
                    lvl = 3
                px[x, y] = CHILI[lvl]
    outline(im, CHILI_OUT)
    for x, y in [(9, 4), (10, 4), (11, 4), (9, 5), (10, 5)]:
        px[x, y] = CHILI_STEM[0]
    for x, y in [(10, 3), (11, 3), (11, 2)]:
        px[x, y] = CHILI_STEM[1]
    for x, y in [(9, 3), (11, 1), (12, 2)]:
        px[x, y] = (30, 62, 18, 255)
    return im


def chili_pepper_v3():
    """3 号候选：两根依偎的小米辣 / 朝天椒（一长一短斜放，层次丰富）。"""
    im = chili_pepper()
    px = im.load()
    # 在右上侧添一小根副椒
    for x, y in [(8, 4), (9, 5), (10, 6), (11, 7), (12, 8)]:
        px[x, y] = CHILI[2]
    for x, y in [(9, 4), (10, 5), (11, 6), (12, 7)]:
        px[x, y] = CHILI[1]
    for x, y in [(7, 3), (8, 3)]:
        px[x, y] = CHILI_STEM[1]
    for x, y in [(7, 2), (9, 3), (10, 4), (11, 5), (12, 6), (13, 7), (13, 8)]:
        if not px[x, y][3]:
            px[x, y] = CHILI_OUT
    return im


def hotpot_base(top, mid, low, flecks):
    """一块锅底料（2026-10-26 用户：做成立方体，要看着是立体的，有圆角，不能完全方）：一块圆角的方块，
    斜着往下看（同原版方块在物品栏里的角度）：顶面亮、左面中、右面暗，棱和角是圆的、颜色过渡；顶上一道油光，撒几粒料。
    做法：圆角立方体的距离场，按物品栏视角每个像素 4×4 采样打光线，按表面朝向混三面的颜色。"""
    import math
    im = blank()
    px = im.load()
    bx, by, bz, r = 0.62, 0.50, 0.62, 0.12          # 半边长（略扁的一块）和圆角半径
    d = (-1 / math.sqrt(3),) * 3                       # 视线：从右上前往左下后看
    right = (1 / math.sqrt(2), 0, -1 / math.sqrt(2))
    up = (-1 / math.sqrt(6), 2 / math.sqrt(6), -1 / math.sqrt(6))

    def sdf(p):
        q = [abs(p[0]) - bx + r, abs(p[1]) - by + r, abs(p[2]) - bz + r]
        out = math.sqrt(sum(max(v, 0) ** 2 for v in q))
        return out + min(max(q), 0) - r

    def normal(p):
        e = 1e-3
        g = [sdf([p[0] + e * (i == 0), p[1] + e * (i == 1), p[2] + e * (i == 2)]) -
             sdf([p[0] - e * (i == 0), p[1] - e * (i == 1), p[2] - e * (i == 2)]) for i in range(3)]
        n = math.sqrt(sum(v * v for v in g)) or 1
        return [v / n for v in g]

    span = 1.95                                         # 16 像素对应的世界宽度
    for y in range(16):
        for x in range(16):
            acc, hits = [0.0, 0.0, 0.0], 0
            for sy in range(4):
                for sx in range(4):
                    u = ((x + (sx + 0.5) / 4) / 16 - 0.5) * span
                    v = (0.5 - (y + (sy + 0.5) / 4) / 16) * span - 0.08
                    o = [right[i] * u + up[i] * v - d[i] * 3 for i in range(3)]
                    t_ = 0.0
                    for _ in range(64):
                        p = [o[i] + d[i] * t_ for i in range(3)]
                        dist = sdf(p)
                        if dist < 1e-3:
                            n = normal(p)
                            # 三面按朝向加权混色：顶（+y）、左（+z）、右（+x）
                            w = [max(n[1], 0) ** 6, max(n[2], 0) ** 6, max(n[0], 0) ** 6]   # 6 次方：面上纯色，只在圆棱上过渡
                            s = sum(w) or 1
                            for i in range(3):
                                acc[i] += (top[i] * w[0] + mid[i] * w[1] + low[i] * w[2]) / s
                            hits += 1
                            break
                        t_ += dist
                        if t_ > 6:
                            break
            if hits >= 8:
                px[x, y] = tuple(round(v / hits) for v in acc) + (255,)
    # 顶面靠左上一道油光
    shine = tuple(min(255, v + 34) for v in top[:3]) + (255,)
    for x, y in ((5, 4), (6, 4), (7, 3), (4, 5)):
        if px[x, y][3]:
            px[x, y] = shine
    for x, y, c in flecks:
        if px[x, y][3]:
            px[x, y] = c
    outline(im, tuple(max(0, v - 90) for v in low[:3]) + (255,))
    return im


def tomato_hotpot_base():
    return hotpot_base((232, 84, 48, 255), (200, 56, 34, 255), (160, 38, 24, 255),
                       [(5, 5, (242, 196, 74, 255)), (10, 6, (242, 196, 74, 255)), (6, 9, (120, 20, 12, 255)),
                        (11, 11, (120, 20, 12, 255)), (8, 5, (104, 170, 58, 255))])


def three_fresh_hotpot_base():
    return hotpot_base((240, 224, 176, 255), (214, 194, 142, 255), (184, 162, 112, 255),
                       [(5, 5, (138, 90, 52, 255)), (10, 5, (236, 136, 48, 255)), (7, 6, (62, 122, 58, 255)),
                        (5, 10, (138, 90, 52, 255)), (11, 9, (236, 136, 48, 255))])


# ------------------------------------------------------------------ 火锅指南（2026-10-25 游戏内教程书）
def hotpot_guide():
    """一本书：深红封面（照原版书的样子斜放），白色书页边，封面正中一口黑铁锅、锅里红汤、上面两缕白汽。"""
    im = blank()
    px = im.load()
    COVER = [(96, 22, 18, 255), (138, 34, 26, 255), (170, 52, 38, 255)]
    PAGE, PAGE_LO = (236, 230, 214, 255), (196, 188, 168, 255)
    OUT = (52, 12, 10, 255)
    # 书身：左下到右上斜放的长方形（照原版书的轮廓），右下有一条书页
    for y in range(2, 15):
        for x in range(2, 14):
            if x + (15 - y) >= 4 and x - y <= 10 and x + (15 - y) <= 24 and y - x <= 11:
                px[x, y] = COVER[1]
    for y in range(2, 15):
        for x in range(2, 14):
            if px[x, y][3] and (x == 2 or y == 2 or px[x - 1, y][3] == 0 or px[x, y - 1][3] == 0):
                px[x, y] = COVER[2]
    for k in range(0, 10):          # 书页（右下边一条）
        x, y = 4 + k, 14 - (k > 8)
        if 0 <= x < 16:
            px[x, 14] = PAGE if k % 3 else PAGE_LO
    for y in range(5, 14):
        px[13, y] = PAGE if y % 3 else PAGE_LO
    # 封面上的小火锅：黑锅身 + 红汤 + 两缕汽
    for x in range(5, 11):
        px[x, 9] = (40, 40, 44, 255)
        px[x, 10] = (40, 40, 44, 255)
    for x in range(6, 10):
        px[x, 8] = (206, 52, 32, 255)
        px[x, 11] = (28, 28, 30, 255)
    px[4, 9], px[11, 9] = (40, 40, 44, 255), (40, 40, 44, 255)
    for x, y in [(6, 6), (7, 5), (9, 6), (8, 5)]:
        px[x, y] = (236, 236, 236, 255)
    outline(im, OUT)
    return im


# ------------------------------------------------------------------ 汤底桶（2026-10-26 用户：照原版空桶 / 水桶 / 岩浆桶的样子）
def soup_bucket(ramp, bits):
    """原版空桶（item/bucket.png）的桶身不动，桶口那一圈（原版水桶里画水的地方）换成汤：按水的明暗套汤色，再撒两三粒料。"""
    empty = Image.open(VANILLA / "item/bucket.png").convert("RGBA")
    water = Image.open(VANILLA / "item/water_bucket.png").convert("RGBA")
    im = empty.copy()
    px, ep, wp = im.load(), empty.load(), water.load()
    cells = []
    for y in range(16):
        for x in range(16):
            if wp[x, y][3] and wp[x, y] != ep[x, y]:
                cells.append((x, y))
    lum = {c: sum(wp[c][:3]) / 3 for c in cells}
    lo, hi = min(lum.values()), max(lum.values())
    for c in cells:
        t = (lum[c] - lo) / max(1, hi - lo)
        k = 0 if t < 0.34 else (1 if t < 0.7 else 2)
        px[c] = ramp[k] + (255,)
    for i, (x, y) in enumerate(sorted(cells, key=lambda c: (c[1] * 7 + c[0] * 3) % 11)[:len(bits)]):
        px[x, y] = bits[i] + (255,)
    return im


def tomato_soup_bucket():
    return soup_bucket(((176, 44, 24), (226, 84, 46), (255, 150, 96)), [(242, 196, 74), (104, 170, 58)])


def three_fresh_soup_bucket():
    return soup_bucket(((190, 162, 104), (226, 208, 160), (250, 240, 210)), [(138, 90, 52), (236, 136, 48), (62, 122, 58)])


ITEMS = {"orange": orange, "tomato_soup_bucket": tomato_soup_bucket, "three_fresh_soup_bucket": three_fresh_soup_bucket, "hotpot_guide": hotpot_guide, "tomato": tomato, "tomato_hotpot_base": tomato_hotpot_base,
         "three_fresh_hotpot_base": three_fresh_hotpot_base, "garlic": garlic, "garlic_paste": garlic_paste, "cooked_kelp": cooked_kelp,
         "chili_pepper": chili_pepper}


def sheet():
    OUT.mkdir(parents=True, exist_ok=True)
    tiles = [("bucket", Image.open(VANILLA / "item/water_bucket.png").convert("RGBA")),
             ("lava", Image.open(VANILLA / "item/lava_bucket.png").convert("RGBA")),
             ("tomato_soup_bucket", tomato_soup_bucket()), ("three_fresh_soup_bucket", three_fresh_soup_bucket()),
             ("hotpot_guide", hotpot_guide()), ("book", Image.open(VANILLA / "item/book.png").convert("RGBA")),
             ("tomato", tomato()), ("tomato_hotpot_base", tomato_hotpot_base()),
             ("three_fresh_hotpot_base", three_fresh_hotpot_base()),
             ("orange", orange()), ("apple", Image.open(VANILLA / "item/apple.png").convert("RGBA")),
             ("garlic", garlic()), ("garlic_paste", garlic_paste()), ("golden_garlic", golden_garlic()),
             ("potato", Image.open(VANILLA / "item/potato.png").convert("RGBA")),
             ("cooked_kelp", cooked_kelp()), ("kelp", Image.open(VANILLA / "item/kelp.png").convert("RGBA")),
             ("dried_kelp", Image.open(VANILLA / "item/dried_kelp.png").convert("RGBA")),
             ("chili_pepper", chili_pepper())]
    tiles += [("garlic_stage%d" % i, garlic_stage(i)) for i in range(4)]
    tiles += [("carrots_stage3", Image.open(VANILLA / "block/carrots_stage3.png").convert("RGBA"))]
    s = 8
    out = Image.new("RGBA", (len(tiles) * (16 * s + 8) + 8, 16 * s + 16), (139, 139, 139, 255))
    for i, (_, t) in enumerate(tiles):
        out.alpha_composite(t.resize((16 * s, 16 * s), Image.NEAREST), (8 + i * (16 * s + 8), 8))
    out.save(OUT / "sheet.png")
    print("sheet:", OUT / "sheet.png", "|", " ".join(n for n, _ in tiles))


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
        # 大蒜作物：用户 2026-10-25 说先保留不做（大蒜先打草掉），画法留着，以后要做再装
        for i in (range(4) if "crop" in sys.argv else ()):
            p = root / "textures/block" / ("garlic_stage%d.png" % i)
            p.parent.mkdir(parents=True, exist_ok=True)
            garlic_stage(i).save(p)
            write_json(root / "models/block" / ("garlic_stage%d.json" % i),
                       {"parent": "minecraft:block/crop", "textures": {"crop": f"{MODID}:block/garlic_stage{i}"}})
        if "crop" not in sys.argv:
            continue
        write_json(root / "blockstates/garlic.json",
                   {"variants": {"age=%d" % i: {"model": f"{MODID}:block/garlic_stage{i}"} for i in range(4)}})
    print("installed")


if __name__ == "__main__":
    sheet()
    if "install" in sys.argv:
        install()
