"""葱花、香菜的物品贴图（r103 预览，用户：打草掉落，能吃，回 1 点饥饿值，蘸料里只提味）。

    python tools/gen_garnish.py            # 出对比图 tools/out/garnish/sheet.png（每样两版 + 原版参照）
    python tools/gen_garnish.py install    # 写进两份资源：物品 小葱 / 香菜，蘸料台里的 葱花 / 香菜碎末
手画像素：深色描边、3~4 档绿色，照原版物品（甜浆果、小麦种子）的画法。
作者：
"""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
OUT = MOD / "tools/out/garnish"
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft/textures/item"
ROOTS = [MOD / "src/main/resources/assets" / MODID, MOD / "src/main/resources/resourcepacks/hotpot_assets/assets" / MODID]

OUT_DK = (34, 70, 20, 255)
G1, G2, G3, G4 = (60, 120, 34, 255), (92, 156, 44, 255), (134, 194, 66, 255), (182, 224, 110, 255)
WH, WH_DK = (236, 240, 214, 255), (196, 204, 170, 255)
STEM = (110, 150, 60, 255)


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def ring(px, x, y, light=True):
    """一小段切开的葱圈：3×3 的环，中间空，左上亮右下暗。"""
    cells = [(0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (0, 2), (1, 2), (2, 2)]
    for dx, dy in cells:
        c = G3 if (dx + dy) <= 1 else (G2 if (dx + dy) <= 2 else G1)
        if not light:
            c = WH if (dx + dy) <= 1 else WH_DK
        px[x + dx, y + dy] = c
    px[x + 1, y + 1] = (0, 0, 0, 0)


def outline(im):
    """给不透明像素外面描一圈深色边（原版物品的画法）。"""
    src = im.copy()
    s, d = src.load(), im.load()
    for y in range(16):
        for x in range(16):
            if s[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and s[x + dx, y + dy][3] > 0
                                       and s[x + dx, y + dy] != (0, 0, 0, 0)
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                # 葱圈中间的洞不描
                if not (all(0 <= x + dx < 16 and 0 <= y + dy < 16 and s[x + dx, y + dy][3] > 0
                            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))):
                    d[x, y] = OUT_DK
    return im


def scallion_1():
    """一小堆切好的葱圈：绿圈为主，夹两个白色葱白圈。"""
    im = blank()
    px = im.load()
    for x, y, light in [(2, 6, True), (6, 3, True), (10, 5, True), (4, 10, False), (8, 9, True), (11, 10, True), (7, 12, False)]:
        ring(px, x, y, light)
    return outline(im)


def scallion_2():
    """一根葱斜着切了几刀：左下葱白、右上葱叶，刀口处断开。"""
    im = blank()
    px = im.load()
    segs = [(2, 12, WH), (4, 10, WH), (6, 8, G3), (8, 6, G2), (10, 4, G2), (12, 2, G1)]
    for i, (x, y, c) in enumerate(segs):
        for dx in range(2):
            for dy in range(2):
                px[x + dx, y + dy] = c if (dx + dy) else (G4 if c != WH else (250, 252, 240, 255))
    return outline(im)


def cilantro_1():
    """一枝香菜：细茎往下，上面三簇锯齿圆叶。"""
    im = blank()
    px = im.load()
    for y in range(8, 15):
        px[7 + (y > 11), y] = STEM
    leaves = {(4, 3): G3, (5, 3): G2, (3, 4): G2, (4, 4): G3, (5, 4): G2, (4, 5): G1, (5, 5): G2, (6, 6): STEM,
              (9, 2): G3, (10, 2): G3, (8, 3): G2, (9, 3): G4, (10, 3): G3, (11, 3): G2, (9, 4): G2, (10, 4): G1, (8, 5): STEM,
              (6, 7): G3, (7, 7): G2, (8, 7): G3, (9, 7): G2, (7, 6): G3, (8, 6): G4, (7, 8): G1, (8, 8): STEM,
              (11, 6): G2, (12, 6): G3, (12, 7): G2, (10, 7): STEM}
    for (x, y), c in leaves.items():
        px[x, y] = c
    return outline(im)


def cilantro_2():
    """切碎的香菜末：一小堆深浅绿色的碎叶，夹几段浅色茎。"""
    im = blank()
    px = im.load()
    pts = {(4, 7): G3, (5, 7): G2, (6, 6): G4, (7, 6): G3, (8, 7): G2, (9, 6): G3, (10, 7): G2, (11, 8): G3,
           (3, 9): G2, (4, 8): G1, (5, 9): G3, (6, 8): G2, (7, 8): G4, (8, 9): G1, (9, 8): G3, (10, 9): G2, (12, 9): G2,
           (4, 10): G2, (5, 10): G1, (6, 10): STEM, (7, 10): G3, (8, 10): G2, (9, 10): STEM, (10, 10): G3, (11, 10): G1,
           (5, 11): G3, (6, 11): G2, (7, 11): G1, (8, 11): G3, (9, 11): G2, (10, 11): G2}
    for (x, y), c in pts.items():
        px[x, y] = c
    return outline(im)


def scallion_whole():
    """一整根小葱（用户：打草掉的是小葱，放进蘸料台才变葱花）：左下白色葱白带几根须，往右上是两片分开的绿叶。"""
    im = blank()
    px = im.load()
    # 须
    for x, y in [(1, 15), (2, 15), (1, 14), (3, 15)]:
        px[x, y] = WH_DK
    # 葱白（2 像素粗，斜着往上）
    for i, (x, y) in enumerate([(2, 13), (3, 12), (4, 11), (5, 10)]):
        px[x, y] = (250, 252, 240, 255)
        px[x + 1, y] = WH if i < 3 else G4
        px[x, y + 1] = WH_DK if i else WH
    # 葱叶：从 (6,9) 起分成两片
    for i, (x, y) in enumerate([(6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (11, 4), (12, 3), (13, 2)]):
        px[x, y] = G3 if i % 3 else G4
        px[x + 1, y] = G2
    for i, (x, y) in enumerate([(7, 9), (8, 9), (9, 8), (10, 8), (11, 7), (12, 7), (13, 6)]):
        px[x, y] = G2 if i % 2 else G3
        px[x, y + 1] = G1
    # 两片叶子之间夹出来的单个透明格补成深绿（不然像个洞）
    for y in range(1, 15):
        for x in range(1, 15):
            if px[x, y][3] == 0 and all(px[x + dx, y + dy][3] > 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = G1
    return outline(im)


def minced_a():
    """碎末 A：散开的小片叶子（每片 2×2 / 3 格的小三叶），中间夹几段浅色茎，不堆成一团。"""
    im = blank()
    px = im.load()
    leaf = [((0, 0), G3), ((1, 0), G2), ((0, 1), G2), ((1, 1), G1)]
    for ox, oy in [(2, 4), (7, 3), (11, 5), (4, 8), (9, 8), (13, 9), (2, 11), (7, 12), (11, 12)]:
        for (dx, dy), c in leaf:
            px[ox + dx, oy + dy] = c
        px[ox, oy] = G4 if (ox + oy) % 2 else G3
    for x, y in [(6, 6), (12, 8), (5, 11), (10, 10)]:
        px[x, y] = STEM
    return outline(im)


def minced_b():
    """碎末 B：几片锯齿边的小圆叶（香菜叶的样子）交叠成一小撮，比 C2 叶形更清楚。"""
    im = blank()
    px = im.load()
    def lobe(ox, oy, hi):
        cells = {(1, 0): G2, (0, 1): G2, (1, 1): hi, (2, 1): G2, (1, 2): G1, (0, 2): G1, (2, 0): G3}
        for (dx, dy), c in cells.items():
            px[ox + dx, oy + dy] = c
    for ox, oy, hi in [(3, 6, G4), (6, 5, G3), (9, 6, G4), (5, 8, G3), (8, 8, G4), (11, 8, G3), (7, 10, G3)]:
        lobe(ox, oy, hi)
    for x, y in [(4, 10), (10, 11), (12, 6)]:
        px[x, y] = STEM
    return outline(im)


def minced_c():
    """碎末 C：切得很细的末，深浅绿点撒成一片（像撒在菜上的香菜末），没有描边。"""
    im = blank()
    px = im.load()
    import random
    rnd = random.Random(7)
    for _ in range(46):
        x, y = rnd.randint(2, 13), rnd.randint(4, 12)
        if (x - 7.5) ** 2 / 36 + (y - 8) ** 2 / 16 <= 1:
            px[x, y] = rnd.choice([G1, G2, G2, G3, G3, G4, STEM])
    return im


MINCED = [minced_a, minced_b, minced_c]
SCALLION = [scallion_1, scallion_2]
CILANTRO = [cilantro_1, cilantro_2]


def sheet():
    OUT.mkdir(parents=True, exist_ok=True)
    # 标签用编号（PIL 默认字体没有中文）：S1 S2 = 葱花两版，C1 C2 = 香菜两版，后面是原版参照
    # 用户定：物品 = 整根小葱 + 一枝香菜（C1）；放进蘸料台后显示成葱花（S1）和香菜碎末（C2）
    tiles = [("scallion", scallion_whole()), ("cilantro (C1)", cilantro_1()),
             ("in station: S1", scallion_1()), ("old C2", cilantro_2()),
             ("minced A", minced_a()), ("minced B", minced_b()), ("minced C", minced_c())]
    for name in ("sweet_berries", "wheat_seeds", "beetroot_seeds"):
        f = VANILLA / f"{name}.png"
        if f.exists():
            tiles.append((name, Image.open(f).convert("RGBA")))
    scale, pad = 12, 24
    w = len(tiles) * (16 * scale + pad) + pad
    im = Image.new("RGBA", (w, 16 * scale + 2 * pad + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    for i, (label, t) in enumerate(tiles):
        x = pad + i * (16 * scale + pad)
        im.alpha_composite(t.resize((16 * scale, 16 * scale), Image.NEAREST), (x, pad))
        d.text((x, pad + 16 * scale + 6), label, fill=(0, 0, 0, 255))
    im.save(OUT / "sheet.png")
    print(OUT / "sheet.png")


def install():
    """物品：小葱、香菜；蘸料台里的样子：葱花、香菜碎末（方块贴图）。"""
    for root in ROOTS:
        (root / "textures/item").mkdir(parents=True, exist_ok=True)
        (root / "textures/block").mkdir(parents=True, exist_ok=True)
        scallion_whole().save(root / "textures/item/scallion.png")
        cilantro_1().save(root / "textures/item/cilantro.png")
        scallion_1().save(root / "textures/block/chopped_scallion.png")
        minced_a().save(root / "textures/block/minced_cilantro.png")   # 用户选 A
        for item in ("scallion", "cilantro"):
            (root / "models/item").mkdir(parents=True, exist_ok=True)
            model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{item}"}}
            (root / f"models/item/{item}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "install":
        install()
    else:
        sheet()
