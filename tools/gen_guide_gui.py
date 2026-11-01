"""「火锅指南」新界面的贴图（2026-10-26 用户：照真的指南来做——摊开的两页书、左边分类书签、顶上条目书签，
要有立体感，书皮是红色的，和物品图标一样）。

    python tools/gen_guide_gui.py            # 写贴图
    python tools/gen_guide_gui.py --preview  # 另外画一张放大的预览图到 build/guide_gui_preview.png

贴图 textures/gui/guide_book.png（512×256，两份资源目录都写），各部分位置和 GuideBookScreen 里的常量对应：
  (0,0)   272×180  摊开的书（右下再带 3 像素半透明投影，整块 275×183）：红皮革封皮（亮边 / 暗边做出厚度）、两边纸页（靠书脊变暗做出弯曲）、
                   外侧和底下一层层的书页边（书的厚度）、中间书脊的阴影
  (280,0) 28×24    左边书签（没选中），(280,24) 选中的；书签从书后面伸出来，右边 4 像素压在封皮下面
  (312,0) 24×24    顶上书签（没选中），(312,24) 选中的
  (340,0) 18×18    物品格；(340,18) 12×8 向右箭头；(360,0) 9×9 / (360,9) 翻页小三角（左 / 右，悬停变亮在 (370,*)）
颜色取自物品图标 hotpot_guide.png：描边 #340c0a、皮面 #8a221a、亮边 #aa3426、纸 #ece6d6 / #c4bca8。
作者：mryt233
"""
import random
import sys
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
ROOTS = [MOD / "common/src/main/resources/assets" / MODID]

OUTLINE = (0x34, 0x0c, 0x0a)
LEATHER = (0x8a, 0x22, 0x1a)
LEATHER_HI = (0xaa, 0x34, 0x26)
LEATHER_LO = (0x6a, 0x18, 0x12)
PAPER = (0xf1, 0xea, 0xd8)
PAPER_EDGE = (0xc4, 0xbc, 0xa8)
PAPER_LINE = (0xdc, 0xd3, 0xbc)

W, H = 272, 180          # 书本身；投影再往右下多 3 像素


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def shade(c, k):
    return tuple(max(0, min(255, round(x * k))) for x in c)


def leather(rnd, base=LEATHER):
    """皮革：在底色上撒一点明暗颗粒（固定种子，每次生成一样）。"""
    r = rnd.random()
    return shade(base, 0.94 if r < 0.18 else 1.05 if r > 0.86 else 1.0)


def book(img):
    px = img.load()
    rnd = random.Random(7)
    # 投影：书往右下错 3 像素的半透明黑影（书「浮」在背景上）
    for y in range(3, H + 3):
        for x in range(3, W + 3):
            px[x, y] = (0, 0, 0, 90)
    # 封皮：整块皮革，外圈描边；左上两圈亮边、右下两圈暗边（光从左上来）
    for y in range(H):
        for x in range(W):
            if x in (0, W - 1) or y in (0, H - 1):
                c = OUTLINE
            elif x == 1 or y == 1:
                c = LEATHER_HI
            elif x == 2 or y == 2:
                c = mix(LEATHER_HI, LEATHER, 0.5)
            elif x == W - 2 or y == H - 2:
                c = LEATHER_LO
            elif x == W - 3 or y == H - 3:
                c = mix(LEATHER_LO, LEATHER, 0.5)
            else:
                c = leather(rnd)
            px[x, y] = c + (255,)
    cx = W // 2
    # 书脊：中间一条凹下去的皮（上下露出来的部分）
    for y in range(1, H - 1):
        for x, c in ((cx - 2, LEATHER_LO), (cx - 1, OUTLINE), (cx, OUTLINE), (cx + 1, LEATHER_LO)):
            px[x, y] = c + (255,)

    # 纸：左页 x0..cx-1，右页 cx+1..x1。下面垫着 4 层书页（往外、往下各错 1 像素，看得见一条条页边），
    # 最上面那页靠书脊往下弯：上缘、下缘靠书脊都收 1 像素，纸面靠书脊渐暗
    top, bottom = 7, H - 11
    layers = 4
    for side in (-1, 1):
        x0, x1 = (12, cx - 1) if side < 0 else (cx + 1, W - 12)
        for k in range(layers, 0, -1):
            lx0, lx1 = (x0 - k, x1) if side < 0 else (x0, x1 + k)
            for y in range(top + 1, bottom + k + 1):
                for x in range(lx0, lx1):
                    edge = x == lx0 if side < 0 else x == lx1 - 1
                    # 页边一层亮一层暗，看得出是一张张纸叠起来的
                    c = ((0xb0, 0xa4, 0x88) if k % 2 else (0xe2, 0xd9, 0xc0)) if (edge or y == bottom + k) else (0xd8, 0xce, 0xb4)
                    px[x, y] = shade(c, 1 - k * 0.03) + (255,)
        for x in range(x0, x1):
            d = (x - x0) if side > 0 else (x1 - 1 - x)             # 离书脊多远
            dip = 1 if d < 6 else 0                                  # 靠书脊上下各收 1 像素
            t = max(0.0, 1 - d / 16)                                # 靠书脊 16 像素渐暗
            for y in range(top + dip, bottom - dip):
                c = mix(PAPER, (0xb4, 0xa8, 0x8e), t * t * 0.9)
                if y == top + dip:
                    c = shade(c, 1.03)                               # 上缘亮一线
                elif y == bottom - dip - 1:
                    c = shade(c, 0.86)                               # 下缘暗一线（纸有厚度）
                px[x, y] = c + (255,)
    # 两页在书脊处的深缝
    for y in range(top + 2, bottom - 2):
        px[cx - 1, y] = (0x7a, 0x6c, 0x56, 255)
        px[cx, y] = (0x4a, 0x34, 0x26, 255)


def tab(img, ox, oy, w, h, horizontal, selected):
    """书签：皮革小片，圆一点的外角，亮边暗边。selected 用纸色（像那一页被翻开）。"""
    px = img.load()
    rnd = random.Random(ox * 31 + oy)
    base = PAPER if selected else LEATHER
    hi = (0xff, 0xfa, 0xec) if selected else LEATHER_HI
    lo = PAPER_EDGE if selected else LEATHER_LO
    for y in range(h):
        for x in range(w):
            # 外角：左边书签圆左上 / 左下；顶上书签圆左上 / 右上
            corners = [(0, 0), (0, h - 1)] if horizontal else [(0, 0), (w - 1, 0)]
            if (x, y) in corners:
                continue
            edge = (x == 0 or y == 0 or y == h - 1) if horizontal else (y == 0 or x == 0 or x == w - 1)
            near_corner = any(abs(x - cxx) + abs(y - cyy) == 1 for cxx, cyy in corners)
            if edge or near_corner:
                px[ox + x, oy + y] = OUTLINE + (255,)
            elif (horizontal and (y == 1 or x == 1)) or (not horizontal and (y == 1 or x == 1)):
                px[ox + x, oy + y] = hi + (255,)
            elif (horizontal and y == h - 2) or (not horizontal and x == w - 2):
                px[ox + x, oy + y] = lo + (255,)
            else:
                px[ox + x, oy + y] = (base if selected else leather(rnd)) + (255,)


def slot(img, ox, oy):
    px = img.load()
    for y in range(18):
        for x in range(18):
            c = (0x8b, 0x8b, 0x8b)
            if x == 17 or y == 17:
                c = (0xff, 0xff, 0xff)
            if (x == 0 or y == 0) and x < 17 and y < 17:
                c = (0x37, 0x37, 0x37)
            px[ox + x, oy + y] = c + (255,)


def arrow(img, ox, oy, color):
    px = img.load()
    rows = ["....##......", "....###.....", "########....", "#########...", "#########...", "########....",
            "....###.....", "....##......"]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                px[ox + x, oy + y] = color + (255,)


def turn(img, ox, oy, right, color):
    px = img.load()
    for y in range(9):
        half = 4 - abs(y - 4)
        for k in range(half + 1):
            x = 2 + k if right else 6 - k
            px[ox + x, oy + y] = color + (255,)


def build():
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    book(img)
    tab(img, 280, 0, 28, 24, True, False)
    tab(img, 280, 24, 28, 24, True, True)
    tab(img, 312, 0, 24, 24, False, False)
    tab(img, 312, 24, 24, 24, False, True)
    slot(img, 340, 0)
    arrow(img, 340, 18, (0x7a, 0x60, 0x40))
    for i, c in enumerate([(0x8a, 0x22, 0x1a), (0xce, 0x34, 0x20)]):
        turn(img, 360 + i * 10, 0, False, c)
        turn(img, 360 + i * 10, 9, True, c)
    return img


ICONS = ["hotpot_guide", "tomato_hotpot_base", "iron_kitchen_knife", "garlic_paste", "bamboo_chopsticks", "orange", "tofu"]


def icon(name):
    return Image.open(ROOTS[0] / f"textures/item/{name}.png").convert("RGBA")


def preview(img, out):
    """拼一张示意：书 + 左边 7 个书签（第 2 个选中、第 4 个悬停伸出）+ 顶上 5 个书签，放大 3 倍。"""
    canvas = Image.new("RGBA", (W + 60, H + 50), (0x3a, 0x3a, 0x40, 255))
    bx, by = 34, 30
    for i in range(7):
        sel, hover = i == 1, i == 3
        ty = by + 10 + i * 24
        sprite = img.crop((280, 24 if sel else 0, 308, 48 if sel else 24))
        tx = bx - 24 - (4 if sel or hover else 0)
        canvas.alpha_composite(sprite, (tx, ty))
        canvas.alpha_composite(icon(ICONS[i]), (tx + 5, ty + 4))
    for i in range(5):
        sel = i == 0
        sprite = img.crop((312, 24 if sel else 0, 336, 48 if sel else 24))
        tx, ty = bx + 14 + i * 26, by - 20 - (3 if sel else 0)
        canvas.alpha_composite(sprite, (tx, ty))
        canvas.alpha_composite(icon(ICONS[i + 1]), (tx + 4, ty + 4))
    canvas.alpha_composite(img.crop((0, 0, W + 3, H + 3)), (bx, by))
    big = canvas.resize((canvas.width * 3, canvas.height * 3), Image.NEAREST)
    out.parent.mkdir(parents=True, exist_ok=True)
    big.save(out)
    print("预览:", out)


def main():
    img = build()
    for root in ROOTS:
        f = root / "textures/gui/guide_book.png"
        f.parent.mkdir(parents=True, exist_ok=True)
        img.save(f)
    print("已生成 textures/gui/guide_book.png")
    if "--preview" in sys.argv:
        preview(img, MOD / "build/guide_gui_preview.png")


if __name__ == "__main__":
    main()
