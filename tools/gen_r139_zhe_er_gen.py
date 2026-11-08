"""折耳根（鱼腥草）的四种样子、布围裙图标，以及布围裙的胸甲护甲层贴图（r139，2026-11-06）。

作者：mryt233（2026-11-06）

    python tools/gen_r139_zhe_er_gen.py            # 写资源，并出对照图
    python tools/gen_r139_zhe_er_gen.py preview    # 只出对照图，不写资源

5 个物品（贴图 + item/generated 模型 + 教程图标）：
  zhe_er_gen 生折耳根 / zhe_er_gen_slice 折耳根段 / cooked_zhe_er_gen_slice 熟折耳根段
  minced_zhe_er_gen 折耳根碎 / cloth_apron 布围裙
另出 textures/models/armor/cloth_apron_layer_1.png：照原版 leather_layer_1 的 64×32 人形布局
画的浅亚麻布胸甲 + 白色挂脖绳，供布围裙的 ArmorMaterial.Layer 使用（代码见 ClothApronItem）。

对照图写在系统临时目录（不占仓库），教程图标同步导出 32×32。
手画像素，照原版食物：深色同色系描边、三到四档明暗、左上亮右下暗。
作者：mryt233
"""
import math
import random
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

from pixkit import canvas, color, io, model as pm
from pixkit.canvas import outline_outer
from pixkit.paths import ASSETS, MOD, MODID
from pixkit.text import font

OUT = Path(tempfile.gettempdir()) / "r139_zhe_er"
ICONS = MOD / "tutorial/img/icons"
ARMOR_TEX = ASSETS / "textures/models/armor"

W = H = 16


# ------------------------------------------------------------------ 公共画法
def c(*rgb):
    """RGB → RGBA（物品贴图一律不透明，物品栏里才不发灰）。"""
    return tuple(rgb) + (255,)


def blank():
    return canvas.blank()


def band(u, pal):
    """u：+1 左上受光、-1 右下背光；pal 是由暗到亮的色阶。"""
    if u > 0.55:
        return pal[3]
    if u > 0.05:
        return pal[2]
    if u > -0.5:
        return pal[1]
    return pal[0]


def recenter(im):
    """把可见内容整体平移整数像素，让主体落在格子正中（贴图自检的「居中」项）。"""
    box = im.getbbox()
    if not box:
        return im
    dx = round(8 - (box[0] + box[2]) / 2)
    dy = round(8 - (box[1] + box[3]) / 2)
    if (dx == 0 and dy == 0) or box[0] + dx < 0 or box[2] + dx > W \
            or box[1] + dy < 0 or box[3] + dy > H:
        return im
    out = blank()
    src, dst = im.load(), out.load()
    for y in range(H):
        for x in range(W):
            if src[x, y][3]:
                dst[x + dx, y + dy] = src[x, y]
    return out


def finish(im, outline):
    """加一圈深色外描边，再按格子居中。"""
    outline_outer(im, outline)
    return recenter(im)


def capsule(px, cx, cy, ang, half, r, pal):
    """一截横切的根段：胶囊侧面 + 右端一个椭圆端面（端面比侧面亮一档），
    端面外面再包一圈淡褐的皮。ang 是长轴方向（弧度）。"""
    ca, sa = math.cos(ang), math.sin(ang)
    for y in range(H):
        for x in range(W):
            fx, fy = x + 0.5 - cx, y + 0.5 - cy
            u = fx * ca + fy * sa
            v = -fx * sa + fy * ca
            # 胶囊轮廓（左端圆弧，右端也要圆，端面叠在里面）
            if abs(u) <= half:
                if abs(v) > r:
                    continue
            elif (abs(u) - half) ** 2 + v * v > r * r:
                continue
            # 端面：偏右的椭圆，比侧面亮一档
            eu = (u - (half - 0.75)) / 0.95
            ev = v / (r - 0.05)
            e2 = eu * eu + ev * ev
            if e2 <= 1.0:
                px[x, y] = pal["face_hi"] if e2 < 0.34 else (pal["face"] if e2 < 0.76 else pal["face_dark"])
                continue
            # 端面外一圈薄皮（切口的皮环）
            if e2 <= 2.30 and u > half - 2.4:
                px[x, y] = pal["skin_hi"] if v < -0.15 else pal["skin"]
                continue
            # 侧面：圆柱明暗，上方亮
            px[x, y] = band(-(v / max(r, 0.8)) * 0.95,
                            [pal["dark"], pal["mid"], pal["light"], pal["light"]])


# ------------------------------------------------------------------ 生折耳根
ROOT = {
    "out": c(104, 90, 78),
    "dark": c(178, 166, 150),
    "mid": c(214, 206, 190),
    "light": c(236, 230, 215),
    "hi": c(250, 246, 236),
    "ring": c(176, 148, 112),
    "ring_hi": c(204, 178, 144),
    "ring_dark": c(142, 114, 86),
    "sprout_v": c(198, 168, 212),
    "sprout_g": c(164, 198, 184),
}
JOINTS = (0.20, 0.48, 0.76)


def _axis(t):
    """主根轴线：x 从左往右走，y 轻微起伏。"""
    return 1.8 + 12.4 * t, 8.3 + 0.75 * math.sin(t * math.pi * 2.0 - 0.7)


def _radius(t):
    """节间略细、节部略鼓。"""
    r = 1.98
    for j in JOINTS:
        r += 0.33 * math.exp(-((t - j) / 0.050) ** 2)
    return r


def zhe_er_gen():
    """生折耳根：一根横放的象牙白粗根，粗得像筷子；节部一圈淡褐色环，
    节上冒出一两点淡紫 / 淡青的嫩芽。"""
    im = blank()
    px = im.load()
    samples = [(k / 140.0,) + _axis(k / 140.0) for k in range(141)]
    for y in range(H):
        for x in range(W):
            best = None
            for (t, ax, ay) in samples:
                d2 = (x + 0.5 - ax) ** 2 + (y + 0.5 - ay) ** 2
                if best is None or d2 < best[0]:
                    best = (d2, t, ax, ay)
            d2, t, ax, ay = best
            r = _radius(t)
            if d2 > r * r:
                continue
            dx, dy = x + 0.5 - ax, y + 0.5 - ay
            u = -(dx * 0.30 + dy * 0.92) / max(r, 0.8)
            col = band(u, [ROOT["dark"], ROOT["mid"], ROOT["light"], ROOT["hi"]])
            for j in JOINTS:                     # 节部淡褐色环纹
                if abs(t - j) < 0.045:
                    col = ROOT["ring_hi"] if u > 0.20 else (ROOT["ring"] if u > -0.35 else ROOT["ring_dark"])
                    break
            px[x, y] = col
    # 嫩芽点：长在节的上侧，探出根外一像素
    for k, (j, col) in enumerate(((JOINTS[0], ROOT["sprout_v"]), (JOINTS[1], ROOT["sprout_g"]),
                                  (JOINTS[2], ROOT["sprout_v"]), (0.33, ROOT["sprout_g"]),
                                  (0.62, ROOT["sprout_v"]))):
        ax, ay = _axis(j)
        r = _radius(j)
        sx, sy = int(round(ax - 0.5)), int(round(ay - r - 0.45))
        if 0 <= sx < W and 1 <= sy < H:
            px[sx, sy] = col
    return finish(im, ROOT["out"])


# ------------------------------------------------------------------ 折耳根段 / 熟折耳根段
SLICE = {
    "out": c(102, 88, 74),
    "skin": c(184, 156, 118),
    "skin_hi": c(208, 182, 146),
    "dark": c(182, 170, 152),
    "mid": c(210, 200, 184),
    "light": c(230, 222, 206),
    "face_dark": c(228, 220, 202),
    "face": c(240, 235, 222),
    "face_hi": c(250, 247, 238),
}
SLICE_COOKED = {
    "out": c(126, 120, 110),
    "skin": c(178, 170, 152),
    "skin_hi": c(196, 190, 174),
    "dark": c(186, 181, 171),
    "mid": c(206, 202, 192),
    "light": c(222, 218, 208),
    "face_dark": c(224, 220, 210),
    "face": c(234, 231, 222),
    "face_hi": c(244, 241, 233),
}
# (中心 x, 中心 y, 角度, 半长, 半径)
SLICE_JOBS = ((5.6, 4.8, 0.46, 2.25, 1.62), (10.6, 6.6, 0.20, 2.25, 1.60),
              (7.9, 10.9, 0.38, 2.20, 1.58))
SLICE_JOBS_COOKED = ((5.9, 5.2, 0.30, 2.35, 1.68), (10.4, 7.2, 0.10, 2.35, 1.66),
                     (8.1, 11.2, 0.22, 2.30, 1.64))


def _slices(pal, jobs):
    im = blank()
    for cx, cy, ang, half, r in jobs:
        piece = blank()
        capsule(piece.load(), cx, cy, ang, half, r, pal)
        outline_outer(piece, pal["out"])
        im.alpha_composite(piece)
    return recenter(im)


def zhe_er_gen_slice():
    """折耳根段：三截斜放的短段，切口朝右，端面是比侧面亮一档的米白椭圆。"""
    return _slices(SLICE, SLICE_JOBS)


def cooked_zhe_er_gen_slice():
    """熟折耳根段：同样的短段，煮过后偏灰白、饱和降一档、身子更软更扁。"""
    return _slices(SLICE_COOKED, SLICE_JOBS_COOKED)


# ------------------------------------------------------------------ 折耳根碎
MINCED = {
    "out": c(118, 106, 90),
    "dark": c(190, 180, 162),
    "mid": c(214, 206, 190),
    "light": c(236, 231, 218),
    "hi": c(250, 246, 237),
    "straw": c(224, 210, 166),
    "green": c(168, 194, 128),
    "green_d": c(138, 164, 100),
}


def minced_zhe_er_gen():
    """折耳根碎：一小堆米白色的细碎末，颗粒一颗颗分明，中间夹着几点浅绿。"""
    im = blank()
    px = im.load()
    for y in range(H):
        for x in range(W):
            dx = (x + 0.5 - 8.0) / 5.7
            dy = (y + 0.5 - 10.7) / 3.6
            if y >= 5 and dx * dx + dy * dy <= 1.0:
                px[x, y] = MINCED["dark"]
    rnd = random.Random(139)
    for gy in range(5, 15):
        half = 5.4 * min(1.0, ((15.4 - gy) / 9.6) ** 0.42)
        for gx in (int(round(8.0 - half)), int(round(8.0 - half / 2)), 8,
                   int(round(8.0 + half / 2)), int(round(8.0 + half))):
            gx += rnd.randint(-1, 1)
            if not (0 <= gx < W and 0 <= gy < H) or not px[gx, gy][3]:
                continue
            top = rnd.choice([MINCED["hi"], MINCED["light"], MINCED["mid"], MINCED["straw"]])
            px[gx, gy] = top
            if gx + 1 < W and px[gx + 1, gy][3] and rnd.random() < 0.65:
                px[gx + 1, gy] = MINCED["light"]
            if gy + 1 < H and px[gx, gy + 1][3]:
                px[gx, gy + 1] = MINCED["dark"]
    # 几点浅绿：切碎时混进去的嫩芽，粒很小
    for gx, gy in ((6, 8), (9, 7), (7, 11), (10, 10), (11, 8), (5, 10)):
        if px[gx, gy][3]:
            px[gx, gy] = MINCED["green"] if (gx + gy) % 3 else MINCED["green_d"]
    return finish(im, MINCED["out"])


# ------------------------------------------------------------------ 布围裙（物品图标）
APRON = {
    "out": c(116, 102, 80),
    "dark": c(196, 184, 158),
    "mid": c(220, 210, 186),
    "light": c(236, 228, 208),
    "hi": c(249, 243, 228),
    "fold": c(176, 163, 138),
    "rope": c(250, 249, 242),
    "rope_dark": c(214, 208, 192),
}


def cloth_apron():
    """布围裙：淡燕麦色的亚麻布围裙，胸口一块方形布片，两条白色挂脖绳往上收拢，
    下摆略微展开，腰上一道褶皱暗部；布面哑光，没有塑料反光。"""
    im = blank()
    px = im.load()
    # 挂脖绳：从胸片两上角 (5,5)、(10,5) 往中间收拢到 (7,2)、(8,2)
    for i in range(4):
        t = i / 3.0
        lx = int(round(5.0 + (6.7 - 5.0) * t))
        ly = int(round(5.0 + (1.8 - 5.0) * t))
        rx = int(round(10.0 + (8.3 - 10.0) * t))
        px[lx, ly] = APRON["rope"]
        if ly + 1 <= 5:
            px[lx, ly + 1] = APRON["rope_dark"]
        px[rx, ly] = APRON["rope"]
        if ly + 1 <= 5:
            px[rx, ly + 1] = APRON["rope_dark"]
    # 胸片：y 5~9，宽 6（x 5~10）
    for y in range(5, 10):
        for x in range(5, 11):
            if y == 5:
                col = APRON["hi"] if x <= 8 else APRON["light"]
            elif x == 5:
                col = APRON["light"]
            elif x == 10 or y == 9:
                col = APRON["dark"]
            else:
                col = APRON["mid"] if (x + y) % 5 else APRON["light"]
            px[x, y] = col
    # 裙摆：从腰往下略微展开，底边有点垂下来
    for y in range(10, 15):
        t = (y - 10) / 4.0
        left = 4.6 - 1.9 * t
        right = 11.4 + 1.9 * t
        for x in range(W):
            fx = x + 0.5
            if fx < left or fx > right:
                continue
            if y == 14 and (x <= 3 or x >= 13):
                continue                       # 底边两角收圆
            if fx < left + 0.6:
                col = APRON["light"]
            elif fx > right - 0.6:
                col = APRON["dark"]
            else:
                col = APRON["mid"]
            px[x, y] = col
    # 腰上一道褶皱暗部 + 摆上两道竖褶
    for x in range(5, 11):
        if px[x, 10][3]:
            px[x, 10] = APRON["fold"]
    for x in (6, 9):
        for y in range(11, 15):
            if px[x, y][3]:
                px[x, y] = APRON["fold"]
    return finish(im, APRON["out"])


# ------------------------------------------------------------------ 布围裙护甲层（64×32）
LINEN = {
    "hi": c(250, 245, 230),
    "light": c(238, 230, 210),
    "mid": c(222, 212, 188),
    "dark": c(204, 192, 166),
    "fold": c(182, 169, 142),
    "rope": c(252, 251, 246),
    "rope_dark": c(228, 224, 214),
}


def _linen_rect(px, rect, tone, rnd):
    """铺一块亚麻布：左右两边各暗一档当布边，中间撒一点织纹（固定种子）。"""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if x == x0 or x == x0 + w - 1:
                px[x, y] = LINEN["dark"]
            elif rnd.random() < 0.16:
                px[x, y] = LINEN["dark"]
            else:
                px[x, y] = tone


def cloth_apron_layer_1():
    """布围裙的胸甲护甲层：照原版 leather_layer_1 的 64×32 人形布局，
    躯干和手臂都铺浅亚麻布，脖子一圈白色挂脖绳，胸口中间敞开露出皮肤。"""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(1391)
    # 躯干：原点 (16,16)，宽 8 高 12 深 4 —— 正面 / 背面 / 左右两侧
    _linen_rect(px, (20, 22, 8, 10), LINEN["light"], rnd)     # 正面（胸 → 腰）
    _linen_rect(px, (32, 22, 8, 10), LINEN["mid"], rnd)       # 背面
    _linen_rect(px, (16, 22, 4, 10), LINEN["mid"], rnd)       # 右侧
    _linen_rect(px, (28, 22, 4, 10), LINEN["mid"], rnd)       # 左侧
    # 挂脖绳：正面两条往上、背面和两侧各一条绕过去
    for x in (20, 21, 26, 27):
        px[x, 20] = LINEN["rope"]
        px[x, 21] = LINEN["rope"]
    for x in range(16, 40):
        if x in (20, 21, 26, 27):
            continue
        px[x, 20] = LINEN["rope"]
        px[x, 21] = LINEN["rope"]
    # 正面两道褶皱 + 腰上一道横褶
    for x in (23, 25):
        for y in range(24, 32):
            px[x, y] = LINEN["fold"]
    for x in range(20, 28):
        px[x, 30] = LINEN["fold"]
    # 手臂：原点 (40,16)，宽 4 高 12 深 4 —— 亚麻布短袖，腕口收一档暗边
    for rect, tone in (((44, 16, 4, 4), LINEN["light"]),    # 顶面
                       ((44, 20, 4, 12), LINEN["light"]),   # 正面
                       ((52, 20, 4, 12), LINEN["mid"]),     # 背面
                       ((40, 20, 4, 12), LINEN["mid"]),     # 右侧
                       ((48, 20, 4, 12), LINEN["mid"])):    # 左侧
        _linen_rect(px, rect, tone, rnd)
    for x in range(40, 56):
        px[x, 31] = LINEN["dark"]                           # 袖口 / 手腕收边
    return im


ITEMS = {
    "zhe_er_gen": zhe_er_gen,
    "zhe_er_gen_slice": zhe_er_gen_slice,
    "cooked_zhe_er_gen_slice": cooked_zhe_er_gen_slice,
    "minced_zhe_er_gen": minced_zhe_er_gen,
    "cloth_apron": cloth_apron,
}


# ------------------------------------------------------------------ 对照图
def sheet(path=None):
    path = Path(path) if path else (OUT / "sheet.png")
    path.parent.mkdir(parents=True, exist_ok=True)
    s = 8
    tile = W * s
    pad = 10
    labels = list(ITEMS)
    armor = cloth_apron_layer_1()
    ast = 6
    aw, ah = armor.width * ast, armor.height * ast
    width = pad * 2 + (len(labels) * (tile + pad)) + aw + 540
    height = 60 + tile + 40 + ah + 20 + 12 * 24
    board = Image.new("RGBA", (width, height), (52, 50, 47, 255))
    draw = ImageDraw.Draw(board)
    try:
        face = font(15)
        face_b = font(20, True)
    except FileNotFoundError:
        face = face_b = None
    if face_b:
        draw.text((pad, 8), "r139：折耳根四种样子 + 布围裙（16×16 物品，放大 8 倍）", font=face_b,
                  fill=(247, 240, 220))
    x = pad
    for name, fn in ((n, ITEMS[n]) for n in labels):
        im = fn()
        draw.rectangle((x - 2, 44, x + tile + 1, 44 + tile + 3), fill=(104, 104, 101))
        draw.rectangle((x, 46, x + tile - 1, 46 + tile - 1), fill=(139, 139, 139))
        board.alpha_composite(im.resize((tile, tile), Image.NEAREST), (x, 46))
        if face:
            w = draw.textbbox((0, 0), name, font=face)[2]
            draw.text((x + (tile - w) / 2, 46 + tile + 6), name, font=face, fill=(214, 210, 196))
        x += tile + pad
    # 护甲层整张
    ay0 = 46 + tile + 40
    board.alpha_composite(armor.resize((aw, ah), Image.NEAREST), (pad, ay0))
    if face:
        draw.text((pad, ay0 + ah + 4), "cloth_apron_layer_1 64×32（放大 6 倍）", font=face,
                  fill=(214, 210, 196))
    # 护甲层每个面单独放大，方便逐面看
    faces = [("body front", (20, 20, 8, 12)), ("body back", (32, 20, 8, 12)),
             ("body side", (16, 20, 4, 12)), ("arm front", (44, 20, 4, 12)),
             ("arm side", (40, 20, 4, 12)), ("arm back", (52, 20, 4, 12)),
             ("arm top", (44, 16, 4, 4))]
    fx0 = pad + aw + 16
    k = 12
    fy = ay0
    for name, (rx, ry, rw, rh) in faces:
        sub = armor.crop((rx, ry, rx + rw, ry + rh))
        big = sub.resize((rw * k, rh * k), Image.NEAREST)
        draw.rectangle((fx0 - 1, fy - 1, fx0 + big.width, fy + big.height), fill=(110, 110, 106))
        board.alpha_composite(big, (fx0, fy))
        if face:
            draw.text((fx0 + big.width + 8, fy + 4), name, font=face, fill=(206, 202, 190))
        fy += big.height + 12
    board.convert("RGB").save(path)
    return path


def install():
    for name, fn in ITEMS.items():
        im = fn()
        io.save_png(im, ASSETS / f"textures/item/{name}.png")
        io.write_json(ASSETS / f"models/item/{name}.json", pm.item_generated(name))
        io.save_png(im.resize((32, 32), Image.NEAREST), ICONS / f"{MODID}__{name}.png")
    io.save_png(cloth_apron_layer_1(), ARMOR_TEX / "cloth_apron_layer_1.png")
    return len(ITEMS)


def main(argv):
    only_preview = "preview" in argv
    print("对照图：", sheet())
    if only_preview:
        return 0
    count = install()
    print(f"已写入 {count} 个物品的贴图 / 物品模型 / 教程图标，以及 1 张护甲层贴图")
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main(sys.argv[1:]))
