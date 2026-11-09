
# -*- coding: utf-8 -*-
"""布围裙三款的穿戴贴图与物品图标（r146b，2026-11-09）。
作者：mryt233

    python tools/gen_apron_styles.py            # 写资源 + 出对照板
    python tools/gen_apron_styles.py preview    # 只出对照板
    python tools/gen_apron_styles.py --armor-only  # 只写三张穿戴贴图（mryt233 改）

三款（从左到右对应某张选定参考图的 1 / 2 / 3）：
  1 cloth_apron              米白粗布（默认款，沿用原来的 ID）
  2 blue_print_cloth_apron   蓝印花（靛蓝底 + 白色十字小花）
  3 brown_trim_cloth_apron   棕边带兜（米白底 + 深棕滚边 + 方框口袋）

产物：
  textures/models/armor/<id>_layer_1.png（64×32，原版胸甲层 UV）
  textures/item/<id>.png（16×16 物品图标）
  models/item/<id>.json（item/generated）
  tutorial/img/icons/<模组>__<id>.png（32×32 教程图标）

穿戴贴图只画前片 + 挂脖带 + 腰后系带，**袖子和后背其余部分全透明**：
原版胸甲层的模型画不到腿，所以「只到腰」不加任何渲染代码，靠 UV 里不画来实现。
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

from pixkit import canvas, io, model as pm
from pixkit.paths import ASSETS, MOD, MODID
from pixkit.text import font

OUT = MOD / "build/r146b_preview"  # mryt233 改：预览统一留在本次验收目录（2026-11-09 目录名去掉工具名）
ICONS = MOD / "tutorial/img/icons"
ARMOR_TEX = ASSETS / "textures/models/armor"
ITEM_TEX = ASSETS / "textures/item"
MODELS = ASSETS / "models/item"

# 原版胸甲层（64×32）的 UV：和人形模型的躯干 / 手臂一一对应
BODY_FRONT = (20, 20, 8, 12)    # 躯干正面 u20..28，v20..32（胸 → 腰）
BODY_BACK = (32, 20, 8, 12)     # 躯干背面 u32..40
BODY_RIGHT = (16, 20, 4, 12)    # 躯干右侧 u16..20
BODY_LEFT = (28, 20, 4, 12)     # 躯干左侧 u28..32
# 手臂：u40..56，v16..32（顶面 44,16,4,4；正面 44,20；背面 52,20；右侧 40,20；左侧 48,20）
ARM_RECTS = [(44, 16, 4, 4), (44, 20, 4, 12), (52, 20, 4, 12), (40, 20, 4, 12), (48, 20, 4, 12)]


def c(*rgb):
    return tuple(rgb) + (255,)


# ---------------------------------------------------------------- 三款配色
# 色阶：outline 描边 / dark 暗 / mid 中 / light 亮 / hi 高光 / fold 褶
PALETTES = {
    "cloth_apron": {
        "outline": c(150, 132, 104), "dark": c(203, 187, 158), "mid": c(222, 209, 184),
        "light": c(234, 224, 202), "hi": c(244, 237, 220), "fold": c(184, 166, 136),
        "rope": c(248, 245, 233), "rope_dark": c(212, 202, 180),
        "tie": c(230, 219, 196),
    },
    "blue_print_cloth_apron": {
        "outline": c(31, 41, 68), "dark": c(42, 54, 88), "mid": c(51, 65, 106),
        "light": c(58, 74, 118), "hi": c(66, 84, 132), "fold": c(38, 49, 83),
        "rope": c(52, 66, 107), "rope_dark": c(38, 49, 83),
        "tie": c(46, 60, 100),
        "print": c(238, 232, 214),        # 白色十字小花
        "print_hi": c(250, 246, 236),
        "print_dark": c(150, 162, 190),
    },
    "brown_trim_cloth_apron": {
        "outline": c(74, 47, 25), "dark": c(203, 187, 158), "mid": c(222, 209, 184),
        "light": c(234, 224, 202), "hi": c(244, 237, 220), "fold": c(184, 166, 136),
        "rope": c(238, 228, 208), "rope_dark": c(196, 182, 154),
        "trim": c(100, 64, 35), "trim_dark": c(80, 51, 27), "trim_light": c(120, 78, 44),
        "tie": c(230, 219, 196),
    },
}


def blank64():
    return canvas.blank(64, 32)


# ---------------------------------------------------------------- 穿戴贴图
def _is_blank(px, x, y):
    return px[x, y][3] == 0


# ---------------------------------------------------------------- 穿戴贴图（r146g 新版）
# mryt233 改（2026-11-09，r146g）：围裙改用自己的穿戴模型（client/ApronModel），从胸口一直垂到膝盖上面。
# 贴图仍是 64×32、躯干沿用原版胸甲 UV（u16..40，v16..32），垂到腿前那片布（裙片）放在没用到的头部区域：
#   裙片方块 8×5×1，贴图起点 (0,0)：顶面 u1..8 v0、底面 u9..16 v0、右侧 u0 v1..5、正面 u1..8 v1..5、
#   左侧 u9 v1..5、背面（贴着腿的一面）u10..17 v1..5。
# 用户定的样子：正面上两行收窄（6 格宽）、往下满宽；侧面只包贴着正面的 1 格；不要腰带；
# 挂脖带从正面上沿两侧走过肩顶、在背面上沿接成一圈。
SKIRT_H = 5                       # 裙片高（像素）：腰 → 膝盖上面
PANEL_H = 12 + SKIRT_H            # 整片围裙（躯干正面 + 裙片）的高
SKIRT_FRONT = (1, 1)              # 裙片正面在贴图上的左上角
SKIRT_RIGHT = (0, 1)
SKIRT_LEFT = (9, 1)
SKIRT_BACK = (10, 1)
SKIRT_TOP = (1, 0)
SKIRT_BOTTOM = (9, 0)
BODY_TOP = (20, 16, 8, 4)         # 躯干顶面（肩顶）u20..28 v16..20


def _noise(x, y, salt=0):
    """固定的伪随机（同一位置每次一样），给粗布织纹用。"""
    n = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF


def apron_panel(style):
    """整片围裙正面（8 × PANEL_H），每格一个颜色或 None（透明）。行 0..11 = 躯干正面，12.. = 裙片。"""
    pal = PALETTES[style]
    grid = [[None] * 8 for _ in range(PANEL_H)]
    for row in range(PANEL_H):
        narrow = row < 2
        left, right = (1, 6) if narrow else (0, 7)
        for col in range(left, right + 1):
            n = _noise(col, row, len(style))
            colr = pal["mid"]
            if n < 0.07:
                colr = pal["dark"]
            elif n > 0.88:
                colr = pal["light"]
            # 左上受光、右下背光
            if col == left and row < 9:
                colr = pal["light"]
            if row == 0 and col < right:
                colr = pal["hi"]
            if col == right:
                colr = pal["dark"] if row < PANEL_H - 1 else pal["fold"]
            if row == PANEL_H - 1:
                colr = pal["fold"]                     # 下摆
            grid[row][col] = colr
    if "print" in pal:
        # 蓝印花：单像素白色碎花，隔行错开，避开边和下摆
        for row in range(1, PANEL_H - 1):
            for col in ((1, 4) if row % 4 == 1 else (3, 6) if row % 4 == 3 else ()):
                if grid[row][col] is not None:
                    grid[row][col] = pal["print"]
    if "trim" in pal:
        # 棕边：整片外沿一圈深棕滚边；裙片中间一个 4×3 小口袋，袋口一道棕边
        for row in range(PANEL_H):
            left, right = (1, 6) if row < 2 else (0, 7)
            grid[row][left] = pal["trim_light"] if row < PANEL_H - 1 else pal["trim_dark"]
            grid[row][right] = pal["trim_dark"]
            if row == 0 or row == PANEL_H - 1:
                for col in range(left, right + 1):
                    grid[row][col] = pal["trim"] if row == 0 else pal["trim_dark"]
        # 胸口收窄处的台阶（第 2 行最外两格）也包边
        grid[2][0] = pal["trim"]
        grid[2][7] = pal["trim_dark"]
        top = 12 + 1
        for col in range(2, 6):
            grid[top][col] = pal["trim"]
        for row in (top + 1, top + 2):
            grid[row][2] = pal["fold"]
            grid[row][5] = pal["fold"]
            for col in (3, 4):
                grid[row][col] = pal["light"] if row == top + 1 else pal["mid"]
        for col in range(2, 6):
            if top + 3 < PANEL_H - 1:
                grid[top + 3][col] = pal["fold"]
    return grid


def apron_layer(style):
    """一款围裙的穿戴贴图（64×32，r146g 新版，配 client/ApronModel）。"""
    pal = PALETTES[style]
    im = blank64()
    px = im.load()
    grid = apron_panel(style)
    edge = pal.get("trim_dark", pal["dark"])

    def put(x, y, col):
        if col is not None and 0 <= x < 64 and 0 <= y < 32:
            px[x, y] = col

    bx, by, _, _ = BODY_FRONT
    sx, sy = SKIRT_FRONT
    for row in range(PANEL_H):
        for col in range(8):
            if row < 12:
                put(bx + col, by + row, grid[row][col])
            else:
                put(sx + col, sy + row - 12, grid[row][col])
    # 侧面只包贴着正面的那 1 格（收窄的两行不包）
    rx, ry, rw, _ = BODY_RIGHT
    lx, ly, _, _ = BODY_LEFT
    for row in range(2, 12):
        put(rx + rw - 1, ry + row, edge if row < 11 else pal["fold"])
        put(lx, ly + row, edge if row < 11 else pal["fold"])
    for row in range(SKIRT_H):
        last = row == SKIRT_H - 1
        put(SKIRT_RIGHT[0], SKIRT_RIGHT[1] + row, pal["fold"] if last else edge)
        put(SKIRT_LEFT[0], SKIRT_LEFT[1] + row, pal["fold"] if last else edge)
    # 裙片顶面 / 底面 / 贴腿的背面
    for col in range(8):
        put(SKIRT_TOP[0] + col, SKIRT_TOP[1], grid[12][col])
        put(SKIRT_BOTTOM[0] + col, SKIRT_BOTTOM[1], pal["fold"])
        for row in range(SKIRT_H):
            put(SKIRT_BACK[0] + col, SKIRT_BACK[1] + row, pal["fold"])
    # 挂脖带：正面上沿两侧（第 1、6 列）→ 肩顶前后贯通 → 背面上沿接成一圈
    tx, ty, tw, td = BODY_TOP
    for row in range(td):
        put(tx + 1, ty + row, pal["rope"])
        put(tx + 6, ty + row, pal["rope_dark"])
    kx, ky, kw, _ = BODY_BACK
    for col in range(1, 7):
        put(kx + col, ky, pal["rope_dark"])
    return im


def apron_layer_old(style):
    """一款围裙的胸甲层（64×32，mryt233 改）。r146g 起不再使用，留作对照。

    胸片先宽 4 格，经 6 格过渡到 8 格；末行收下摆，光从左上落下。
    顶行细带接起胸片、两侧和背面；v29 腰带绕到背面小结。
    蓝印花每朵只占一格，棕边款下半部只留 4×3 的小口袋。
    只使用躯干四个侧面的 UV，头部、袖子及背面带子以外全部透明。
    """
    pal = PALETTES[style]
    im = blank64()
    px = im.load()
    x0, y0, w, h = BODY_FRONT

    def put(x, y, col):
        if 0 <= x < 64 and 0 <= y < 32:
            px[x, y] = col

    # mryt233 改：窄胸片逐级展开，暗边只占一格，不在胸前或腰间加粗横杠。
    for row in range(h):
        y = y0 + row
        inset = 2 if row < 3 else (1 if row == 3 else 0)
        left, right = x0 + inset, x0 + w - 1 - inset
        for x in range(left, right + 1):
            col = pal["mid"]
            if row == 0:
                col = pal["hi"] if x < right - 1 else pal["light"]
            elif row == h - 1:
                col = pal.get("trim_dark", pal["fold"])
            elif x == left:
                col = pal.get("trim_light", pal["fold"])
            elif x == right:
                col = pal.get("trim_dark", pal["outline"])
            elif x == left + 1 and row < 8:
                col = pal["light"]
            elif x == right - 1 and row >= 5:
                col = pal["dark"]
            elif (x - x0, row) in ((3, 2), (2, 4), (3, 4), (2, 7)):
                col = pal["light"]
            elif (x - x0, row) in ((4, 6), (4, 10)):
                col = pal["dark"]
            put(x, y, col)

    # mryt233 改：12 朵单像素碎花错行排布，彼此不相连，也不覆盖布边和下摆。
    if "print" in pal:
        for row, columns in ((1, (3,)), (3, (2, 5)), (5, (1, 3, 5)),
                             (7, (2, 4, 6)), (9, (1, 3, 5))):
            for column in columns:
                put(x0 + column, y0 + row, pal["print"])

    # mryt233 改：口袋限定 u22..26、v27..30（4×3），仅袋口用深棕，侧底缝压低对比。
    if "trim" in pal:
        for column in range(2, 6):
            put(x0 + column, y0 + 7, pal["trim"] if column < 5 else pal["trim_dark"])
            put(x0 + column, y0 + 9, pal["fold"])
        put(x0 + 2, y0 + 8, pal["fold"])
        put(x0 + 3, y0 + 8, pal["light"])
        put(x0 + 4, y0 + 8, pal["mid"])
        put(x0 + 5, y0 + 8, c(162, 140, 110))

    # mryt233 改：补齐正面顶行与胸片之间原先空着的两格，整圈带子保持一格高。
    for column in (0, 1, 6, 7):
        put(x0 + column, y0, pal["rope"] if column < 4 else pal["rope_dark"])
    for rx, ry, rw, _ in (BODY_RIGHT, BODY_LEFT, BODY_BACK):
        for x in range(rx, rx + rw):
            put(x, ry, pal["rope_dark"])

    # 腰带只绕侧面与背面，正面保留布面，袋口不被横褶切开。
    tie_y = y0 + 9
    for rx, _, rw, _ in (BODY_RIGHT, BODY_LEFT, BODY_BACK):
        for x in range(rx, rx + rw):
            put(x, tie_y, pal["tie"])
    bx0, _, _, _ = BODY_BACK
    put(bx0 + 2, tie_y - 1, pal["rope_dark"])
    put(bx0 + 5, tie_y - 1, pal["rope_dark"])
    put(bx0 + 3, tie_y, pal["rope"])
    put(bx0 + 4, tie_y, pal["rope"])
    put(bx0 + 3, tie_y + 1, pal["rope_dark"])
    put(bx0 + 5, tie_y + 1, pal["rope_dark"])
    return im


# ---------------------------------------------------------------- 物品图标（16×16）
def apron_icon(style):
    """平铺的围裙：上面一圈挂脖带、下面梯形裙身，照参考图的配色与花纹缩小画。"""
    pal = PALETTES[style]
    im = canvas.blank()
    px = im.load()

    def put(x, y, col):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = col

    # 挂脖带：左右两条竖带，顶部收成一条横带（参考图第 1 行）
    for y in range(3, 7):
        put(4, y, pal["rope"])
        put(11, y, pal["rope_dark"])
    for x in range(4, 12):
        put(x, 2, pal["rope"])
    put(4, 3, pal["rope_dark"])

    # 胸片：y 6..9，宽 6（x 5..10）
    for y in range(6, 10):
        for x in range(5, 11):
            if y == 6:
                col = pal["hi"] if x <= 8 else pal["light"]
            elif x == 5:
                col = pal["light"]
            elif x == 10 or y == 9:
                col = pal["dark"]
            else:
                col = pal["mid"] if (x + y) % 5 else pal["light"]
            put(x, y, col)
    # 裙摆：y 10..14，往下略微展开
    for y in range(10, 15):
        t = (y - 10) / 4.0
        left = 4.6 - 1.9 * t
        right = 11.4 + 1.9 * t
        for x in range(16):
            fx = x + 0.5
            if fx < left or fx > right:
                continue
            if y == 14 and (x <= 3 or x >= 13):
                continue
            if fx < left + 0.6:
                col = pal["light"]
            elif fx > right - 0.6:
                col = pal["dark"]
            else:
                col = pal["mid"]
            put(x, y, col)
    # 腰上褶 + 两道竖褶
    for x in range(5, 11):
        if px[x, 10][3]:
            put(x, 10, pal["fold"])
    for x in (6, 9):
        for y in range(11, 15):
            if px[x, y][3]:
                put(x, y, pal["fold"])

    # 蓝印花：十字小花
    if "print" in pal:
        def flower(mx, my):
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                if px[mx + dx, my + dy][3]:
                    put(mx + dx, my + dy, pal["print"] if (dx, dy) != (0, 0) else pal["print_hi"])
        flower(7, 7)
        flower(9, 11)
        flower(6, 13)
    # 棕边：滚边 + 方框口袋
    if "trim" in pal:
        for x in range(5, 11):
            if px[x, 10][3]:
                put(x, 10, pal["trim"])
        for y in range(11, 15):
            if px[5, y][3]:
                put(5, y, pal["trim"])
            if px[10, y][3]:
                put(10, y, pal["trim_dark"])
        for x in range(7, 10):
            put(x, 11, pal["trim"])
        for y in range(11, 13):
            put(7, y, pal["trim"])
            put(9, y, pal["trim_dark"])

    # 描边：外圈压暗一档（物品贴图习惯）
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    edge = PALETTES[style].get("trim_dark") or pal["outline"]
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = edge
    return im


STYLES = ("cloth_apron", "blue_print_cloth_apron", "brown_trim_cloth_apron")


def install(armor_only=False):
    for style in STYLES:
        io.save_png(apron_layer(style), ARMOR_TEX / f"{style}_layer_1.png")
        if armor_only:  # mryt233 改：并行开发时不触碰物品、模型与教程图标。
            continue
        icon = apron_icon(style)
        io.save_png(icon, ITEM_TEX / f"{style}.png")
        io.write_json(MODELS / f"{style}.json", pm.item_generated(style))
        io.save_png(icon.resize((32, 32), Image.NEAREST), ICONS / f"{MODID}__{style}.png")
    return len(STYLES)


def sheet(path=None):
    """对照板：三款穿戴贴图（放大 8 倍，垫棋盘格）+ 三款物品图标（放大 8 倍）。"""
    path = Path(path) if path else (OUT / "apron_styles.png")
    path.parent.mkdir(parents=True, exist_ok=True)
    K = 8
    pad = 16
    lw = 64 * K
    lh = 32 * K
    iw = 16 * K
    width = pad * 2 + len(STYLES) * (lw + pad)
    height = pad + 24 + lh + 30 + iw + 30
    board = Image.new("RGBA", (width, height), (46, 44, 42, 255))
    d = ImageDraw.Draw(board)
    try:
        face = font(16)
        title = font(20, True)
    except FileNotFoundError:
        face = title = None
    if title:
        d.text((pad, 6), "r146b 布围裙三款：穿戴贴图（上，64×32）+ 物品图标（下，16×16）", font=title,
               fill=(247, 240, 220))
    y0 = 34
    for i, style in enumerate(STYLES):
        x = pad + i * (lw + pad)
        for cy in range(0, lh, 16):
            for cx in range(0, lw, 16):
                if (cx // 16 + cy // 16) % 2 == 0:
                    d.rectangle([x + cx, y0 + cy, x + cx + 15, y0 + cy + 15], fill=(64, 62, 60, 255))
        board.alpha_composite(apron_layer(style).resize((lw, lh), Image.NEAREST), (x, y0))
        if face:
            d.text((x, y0 + lh + 6), style, font=face, fill=(220, 214, 200))
        iy = y0 + lh + 26
        d.rectangle([x, iy, x + iw - 1, iy + iw - 1], fill=(139, 139, 139, 255))
        board.alpha_composite(apron_icon(style).resize((iw, iw), Image.NEAREST), (x, iy))
    board.convert("RGB").save(path)
    return path


def main(argv):
    if "--armor-only" in argv:  # mryt233 改：此入口只写三张穿戴贴图，不另出预览。
        print(f"已写入 {install(armor_only=True)} 款穿戴贴图")
        return 0
    print("对照板：", sheet())
    if "preview" in argv:
        return 0
    n = install()
    print(f"已写入 {n} 款的穿戴贴图 / 物品贴图 / 物品模型 / 教程图标")
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main(sys.argv[1:]))
