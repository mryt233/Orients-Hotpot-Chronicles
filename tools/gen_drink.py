"""果橙多（r99，用户要的饮料，外观照现实里的 2 升装橙汁瓶，名字避开真品牌）：贴图 + 模型。

    python tools/gen_drink.py
a 是第一版（按用户最早给的 33×30×18 厘米，后来用户更正为 12×10×30）；
b 是按照片里瓶子的比例（细高）做的：第一版 3×2、6 像素高，用户说太小、像洗洁精，改成 4×3 瓶身、近 10 像素高，有瓶肩、瓶颈、提手环、橙子标签。都放在盘子格里由渲染器挪到桌面。
作者：
"""
import json
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]

O, OL, OD = (247, 160, 20, 255), (255, 200, 70, 255), (212, 118, 12, 255)
R, W, G = (205, 32, 30, 255), (250, 246, 236, 255), (96, 160, 44, 255)
CAP = (236, 140, 10, 255)


def texture():
    im = Image.new("RGBA", (16, 16), O)
    px = im.load()
    # 正面 0..5 × 0..6（从上往下）：瓶肩亮一点、红标签、白字、下半截瓶身深一点
    for x in range(6):
        px[x, 0] = OL
        px[x, 1] = R
        px[x, 2] = R
        px[x, 4] = O
        px[x, 5] = OD
    px[1, 1], px[2, 1], px[3, 2] = W, W, W          # 标签上的白字
    for x in range(6):
        px[x, 3] = OL if x in (1, 2) else O      # 橙子图案的亮面
    px[4, 3] = G                                   # 叶子
    # 侧面 8..11 × 0..6
    for y in range(6):
        for x in range(8, 11):
            px[x, y] = OL if x == 8 and y < 4 else (OD if y == 5 else O)
    # 瓶盖 12..14 × 0..2
    for y in range(2):
        for x in range(12, 14):
            px[x, y] = CAP if y else OL
    return im


def face(uv, tex="#d"):
    return {"uv": uv, "texture": tex}


def bottle(w, d, h):
    """瓶身 w×d、高 h，上面一圈瓶肩 + 瓶盖 + 提手；中心在 (8, 8)，底在 y=0。"""
    x0, x1, z0, z1 = 8 - w / 2, 8 + w / 2, 8 - d / 2, 8 + d / 2
    body_h = h - 1.2
    els = [{
        "from": [x0, 0, z0], "to": [x1, body_h, z1],
        "faces": {
            "north": face([0, 0, w, 6]), "south": face([0, 0, w, 6]),
            "east": face([8, 0, 8 + d, 6]), "west": face([8, 0, 8 + d, 6]),
            "up": face([0, 0, w, d]), "down": face([0, 4, w, 4 + d]),
        }}]
    # 瓶肩（窄一圈）
    els.append({"from": [x0 + 0.5, body_h, z0 + 0.5], "to": [x1 - 0.5, body_h + 0.5, z1 - 0.5],
                "faces": {f: face([0, 0, 1, 1]) for f in ["north", "south", "east", "west", "up"]}})
    # 瓶盖
    els.append({"from": [7.25, body_h + 0.5, 7.25], "to": [8.75, h, 8.75],
                "faces": {f: face([12, 0, 14, 2]) for f in ["north", "south", "east", "west", "up"]}})
    # 提手（照片里瓶颈上那个橙色提手）
    els.append({"from": [8.75, body_h + 0.25, 7.75], "to": [9.75, h - 0.25, 8.25],
                "faces": {f: face([12, 1, 13, 2]) for f in ["north", "south", "east", "west", "up"]}})
    return {"parent": "minecraft:block/block",
            "textures": {"d": f"{MODID}:block/orange_drink", "particle": f"{MODID}:block/orange_drink"},
            "elements": els}



FRUIT, FRUIT_HI, LABEL_Y = (255, 150, 10, 255), (255, 215, 90, 255), (246, 206, 70, 255)
GLASS, GLASS_HI, GLASS_LO = (250, 170, 30, 255), (255, 214, 110, 255), (226, 130, 14, 255)


GLASS_HI2 = (255, 228, 150, 255)
SLICE, SLICE_IN = (255, 176, 36, 255), (255, 226, 140, 255)   # 橙子切面：外圈、果瓣




def texture_b(im):
    """整张贴图给 b 用。正面 0..6、侧面 6..11，都是 15 行，一行 = 模型里 1 像素高（第 0 行是 y 14..15，
    往下到第 14 行 y 0..1）。顶 / 底 11..16 × 0..5，瓶盖 11..13 × 6..8，提环 14..16 × 6..8。
    配色：用户试过「MC 风格多档深浅 + 杂点」两版后，说还是最早这套平涂配色好看，下半截的「回」字防滑纹也保留。
    名称牌按用户后来定的：居中、只在正面和背面。"""
    px = im.load()
    Gl, Hi, Lo, H2 = GLASS, GLASS_HI, GLASS_LO, GLASS_HI2
    front = {
        14: [Hi] * 6, 13: [Hi] * 6,                    # 瓶颈（瓶盖另有贴图）
        12: [H2, Hi, Hi, Hi, Hi, Gl],                  # 上半截顶边
        11: [Gl] * 6,
        10: [Gl, R, W, W, R, Gl],                      # 红色名称牌（居中）
        9: [Gl, R, R, W, R, Gl],
        8: [Gl, FRUIT_HI, FRUIT, FRUIT, G, Gl],        # 橙子 + 叶子
        7: [Gl, FRUIT, FRUIT_HI, FRUIT, G, Gl],
        6: [Gl, G, G, G, Gl, Gl],                      # 绿色小字
        5: [Gl] * 6,
        4: [Gl, Hi, Hi, Hi, Hi, Gl],                   # 「回」字防滑纹
        3: [Gl, Hi, Gl, Gl, Hi, Gl],
        2: [Gl, Hi, Gl, Gl, Hi, Gl],
        1: [Gl, Hi, Hi, Hi, Hi, Gl],
        0: [Lo] * 6,
    }
    side = {14: [Hi] * 5, 13: [Hi] * 5, 12: [H2, Hi, Hi, Gl, Gl],
            # 侧面上半截也加「回」字（0.1.2-alpha 后用户要求，调整；和下半截同样式）
            11: [Hi, Gl, Gl, Gl, Gl], 10: [Gl, Hi, Hi, Hi, Gl], 9: [Gl, Hi, Gl, Hi, Gl],
            8: [Gl, Hi, Gl, Hi, Gl], 7: [Gl, Hi, Hi, Hi, Gl], 6: [Hi, Gl, Gl, Gl, Gl], 5: [Hi, Gl, Gl, Gl, Gl],
            4: [Gl, Hi, Hi, Hi, Gl], 3: [Gl, Hi, Gl, Hi, Gl], 2: [Gl, Hi, Gl, Hi, Gl], 1: [Gl, Hi, Hi, Hi, Gl],
            0: [Lo] * 5}
    for y in range(15):
        row = 14 - y
        for x, c in enumerate(front[y]):
            px[x, row] = c
        for x, c in enumerate(side[y]):
            px[6 + x, row] = c
    for y in range(0, 5):
        for x in range(11, 16):
            px[x, y] = Hi if (x + y) % 3 else Gl
    for y in range(6, 8):
        for x in range(11, 13):
            px[x, y] = OL if y == 6 else CAP
        for x in range(14, 16):
            px[x, y] = CAP


def texture_empty(full):
    """喝完的空瓶：瓶身换成很淡的透明感橙色，名称牌和图案留着。"""
    im = full.copy()
    px = im.load()
    pale = {GLASS: (255, 236, 196, 255), GLASS_HI: (255, 246, 222, 255), GLASS_LO: (240, 214, 170, 255),
            GLASS_HI2: (255, 250, 236, 255)}
    for y in range(16):
        for x in range(16):
            if px[x, y] in pale:
                px[x, y] = pale[px[x, y]]
    return im


def bottle_b():
    """2 升装，现实 12×10×30 厘米 → 瓶身 6×5、总高 15（用户：再大两圈，比例照旧）。
    下半截防滑方格、腰部收进去、上半截红色名称牌 + 橙子；上半截饱满到顶、一级台阶就到瓶口；提环是放下来平躺在瓶顶的 U 形。"""
    def uv_front(y0, y1):
        return [0, 15 - y1, 6, 15 - y0]

    def uv_side(y0, y1):
        return [6, 15 - y1, 11, 15 - y0]

    def box(fr, to, kind="body"):
        y0, y1 = fr[1], to[1]
        if kind != "body":
            uv = [11, 6, 13, 8] if kind == "cap" else [14, 6, 16, 8]
            faces = {k: {"uv": uv, "texture": "#d"} for k in ["north", "south", "east", "west", "up", "down"]}
        else:
            ff = {"uv": uv_front(y0, y1), "texture": "#d"}
            sf = {"uv": uv_side(y0, y1), "texture": "#d"}
            tf = {"uv": [11, 0, 16, 5], "texture": "#d"}
            faces = {"north": ff, "south": ff, "east": sf, "west": sf, "up": tf, "down": tf}
        return {"from": fr, "to": to, "faces": faces}

    # 腰用瓶底那一行的深色（按高度取的话会取到上面的小黄标）
    waist = box([5.5, 6, 6], [10.5, 6.5, 10])
    for k, f in waist["faces"].items():
        if k in ("north", "south"):
            f["uv"] = [0, 14, 6, 14.5]
        elif k in ("east", "west"):
            f["uv"] = [6, 14, 11, 14.5]
    els = [
        box([5, 0, 5.5], [11, 6, 10.5]),               # 下半截（防滑方格）
        waist,                                         # 腰：y 6..6.5，只有 0.5 像素高（用户：往上 1 像素、0.5 厚）
        box([5, 6.5, 5.5], [11, 12.5, 10.5]),          # 上半截：饱满的方块一直到顶（用户：不要现实那样慢慢收）
        box([6, 12.5, 6.5], [10, 13, 9.5]),            # 一小级台阶就到瓶口
        box([7, 13, 7], [9, 13.75, 9]),                # 瓶颈
        box([6.9, 13.75, 6.9], [9.1, 15, 9.1], "cap"), # 瓶盖
        # 提环放下来的样子（照片）：瓶颈套一圈，两根细杆平躺在瓶顶上伸到边上，一根横杆连起来，像个平放的 U
        box([6.75, 13.1, 6.75], [9.25, 13.5, 9.25], "ring"),
        # 提环不贴死（用户）：整个 U 平着悬在瓶顶上方，离台阶 0.5、离上半截顶面 1 像素
        box([9.25, 13.5, 6.75], [10.9, 13.85, 7.1], "ring"),
        box([9.25, 13.5, 8.9], [10.9, 13.85, 9.25], "ring"),
        box([10.55, 13.5, 7.1], [10.9, 13.85, 8.9], "ring"),   # 横杆
    ]
    return {"parent": "minecraft:block/block",
            "textures": {"d": f"{MODID}:block/orange_drink", "particle": f"{MODID}:block/orange_drink"},
            "elements": els}


def main():
    tex = Image.new("RGBA", (16, 16), O)
    texture_b(tex)
    for root in ASSET_ROOTS:
        (root / "textures/block").mkdir(parents=True, exist_ok=True)
        tex.save(root / "textures/block/orange_drink.png")
        texture_empty(tex).save(root / "textures/block/orange_drink_empty.png")
        empty = bottle_b()
        empty["textures"] = {"d": f"{MODID}:block/orange_drink_empty", "particle": f"{MODID}:block/orange_drink_empty"}
        (root / "models/block/orange_drink_empty.json").write_text(json.dumps(empty, indent=2) + "\n", encoding="utf-8")
        (root / "models/block/orange_drink_a.json").unlink(missing_ok=True)   # 第一版（按错的尺寸），已淘汰
        (root / "models/block/orange_drink_b.json").unlink(missing_ok=True)   # 定稿后改名为 orange_drink
        (root / "models/block/orange_drink.json").write_text(json.dumps(bottle_b(), indent=2) + "\n", encoding="utf-8")
    print("ok")


if __name__ == "__main__":
    main()


DRINK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0.5, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 13, 0], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 1, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
}


def game_files():
    """摆在桌上的瓶子（方块状态 drink：种类 × 满 / 空 × 方向）和每种饮料的满瓶 / 空瓶物品模型。
    r101 加了维二（模型和贴图在 tools/gen_plant_protein_drink.py，物品显示参数共用这里的 DRINK_DISPLAY）。"""
    kinds = ("orange_drink", "plant_protein_drink")
    for root in ASSET_ROOTS:
        variants = {}
        for kind in kinds:
            for axis in ("x", "z"):
                for empty in ("false", "true"):
                    v = {"model": f"{MODID}:block/{kind}" + ("_empty" if empty == "true" else "")}
                    if axis == "z":
                        v["y"] = 90
                    variants[f"axis={axis},empty={empty},kind={kind}"] = v
        (root / "blockstates/drink.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8")
        for kind in kinds:
            full, empty = {"orange_drink": ("orange_drink", "empty_orange_drink_bottle"),
                           "plant_protein_drink": ("plant_protein_drink", "empty_plant_protein_drink_bottle")}[kind]   # 物品 ID（r102）
            for item, model in ((full, kind), (empty, f"{kind}_empty")):
                (root / f"models/item/{item}.json").write_text(json.dumps(
                    {"parent": f"{MODID}:block/{model}", "display": DRINK_DISPLAY}, indent=2) + "\n", encoding="utf-8")


game_files()
