"""火锅食材（2026-10-23 用户：一样样加，先做原版有的；这批：土豆片 + 豆腐）。画贴图和物品模型：

  土豆片 potato_slice / 熟土豆片 cooked_potato_slice   原版土豆在砧板上切 3 片（切石机也行），熟的每片回 2
  黄豆 soybean                                          打草掉（像小葱 / 香菜），以后再考虑做成作物
  豆腐 tofu                                             3 黄豆 + 水桶在工作台做一整块（桶留下）
  豆腐块 tofu_cube / 熟豆腐块 cooked_tofu_cube           一整块豆腐在砧板上切 3 块

    python tools/gen_ingredients.py
作者：
"""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, k):
    return clamp(tuple(v * k for v in c))


def outline(im, color):
    """不透明区域最外一圈描深色（同原版物品）。"""
    px = im.load()
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = color + (255,)


# ---------------------------------------------------------------- 土豆片
# 三片斜着叠放的圆片（斜看是椭圆）：浅黄的切面、一圈土豆皮，几个小斑点。熟的变成透亮的金黄。
POTATO = {
    "potato_slice": ((238, 220, 150), (176, 132, 72), (214, 190, 120)),
    "cooked_potato_slice": ((240, 196, 96), (170, 118, 50), (226, 170, 74)),
}
SLICES = [(5.5, 10.5), (8.0, 8.0), (10.5, 5.5)]   # 三片的中心（从左下往右上叠）
SLICE_RX, SLICE_RY = 4.6, 3.2


def potato_texture(face, skin, spot, seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for cx, cy in SLICES:                         # 后画的压在上面
        for y in range(16):
            for x in range(16):
                r = ((x + 0.5 - cx) / SLICE_RX) ** 2 + ((y + 0.5 - cy) / SLICE_RY) ** 2
                if r <= 1:
                    if r > 0.8:
                        c = shade(skin, 0.6)              # 每片自己一圈深边，叠着也分得开
                    elif r > 0.55:
                        c = skin
                    else:
                        c = shade(face, 1.06 if r < 0.2 else 1.0)
                        if rnd.random() < 0.06:
                            c = spot
                    px[x, y] = c + (255,)
    outline(im, shade(skin, 0.6))
    return im


# ---------------------------------------------------------------- 黄豆
BEANS = [(2, 8), (7, 5), (9, 10), (3, 12), (11, 3)]
BEAN = [".XX.", "XXXX", "XXXX", ".XX."]


def soybean_texture():
    """几颗圆鼓鼓的黄豆（4×4 一颗），每颗一个高光、一个豆脐、自己一圈描边。"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for bx, by in BEANS:
        bean = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = bean.load()
        for dy, row in enumerate(BEAN):
            for dx, ch in enumerate(row):
                if ch == "X" and bx + dx < 16 and by + dy < 16:
                    px[bx + dx, by + dy] = (226, 196, 110, 255)
        px[bx + 1, by + 1] = (248, 228, 162, 255)
        px[bx + 2, by + 2] = (160, 120, 56, 255)
        outline(bean, (130, 98, 44))
        im.alpha_composite(bean)
    return im


# ---------------------------------------------------------------- 豆腐（等角小方块）
def cube(draw, cx, ty, w, h, top, left, right):
    q = w / 4
    draw.polygon([(cx, ty), (cx + w / 2, ty + q), (cx, ty + 2 * q), (cx - w / 2, ty + q)], fill=top)
    draw.polygon([(cx - w / 2, ty + q), (cx, ty + 2 * q), (cx, ty + 2 * q + h), (cx - w / 2, ty + q + h)], fill=left)
    draw.polygon([(cx + w / 2, ty + q), (cx, ty + 2 * q), (cx, ty + 2 * q + h), (cx + w / 2, ty + q + h)], fill=right)


TOFU = {
    "tofu": ((252, 250, 240), (230, 226, 210), (208, 202, 184)),
    "tofu_cube": ((252, 250, 240), (230, 226, 210), (208, 202, 184)),
    "cooked_tofu_cube": ((244, 222, 168), (222, 192, 132), (196, 162, 104)),   # 汤里煮过：吸了汤，微黄
}


def tofu_texture(name):
    """一整块：扁扁的大方块；豆腐块：三小块，每块单独描边后从后往前叠，才分得开。"""
    top, left, right = TOFU[name]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cubes = [(7.5, 2, 14, 6)] if name == "tofu" else [(10.5, 1.0, 8, 4), (4.5, 4.5, 8, 4), (9.5, 7.5, 8, 4)]
    rnd = random.Random(len(name))
    for cx, ty, w, h in cubes:
        layer = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        cube(ImageDraw.Draw(layer), cx, ty, w, h, top + (255,), left + (255,), right + (255,))
        px = layer.load()
        for y in range(16):                                                            # 细细的气孔
            for x in range(16):
                if px[x, y][3] and rnd.random() < 0.05:
                    px[x, y] = shade(px[x, y][:3], 0.92) + (255,)
        outline(layer, shade(right, 0.62))
        im.alpha_composite(layer)
    return im


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def item_model(root, name):
    write(root / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{name}"}})


def main():
    for root in ROOTS:
        tex = root / "textures/item"
        tex.mkdir(parents=True, exist_ok=True)
        for i, (name, cols) in enumerate(POTATO.items()):
            potato_texture(*cols, seed=3).save(tex / f"{name}.png")
            item_model(root, name)
        soybean_texture().save(tex / "soybean.png")
        item_model(root, "soybean")
        for name in TOFU:
            tofu_texture(name).save(tex / f"{name}.png")
            item_model(root, name)
    print("ok")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- 进游戏的数据
DATA = RES / "data"


def data_files():
    rec = DATA / MODID / "recipe"
    for raw, out in [("minecraft:potato", "potato_slice"), (f"{MODID}:tofu", "tofu_cube")]:
        write(rec / f"{out}_from_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": {"item": raw},
                                                     "result": {"id": f"{MODID}:{out}", "count": 3}})
        for kind, time, suffix in [("smelting", 100, ""), ("smoking", 50, "_from_smoking"), ("campfire_cooking", 300, "_from_campfire_cooking")]:
            write(rec / f"cooked_{out}{suffix}.json", {"type": f"minecraft:{kind}", "category": "food", "ingredient": {"item": f"{MODID}:{out}"},
                                                        "result": {"id": f"{MODID}:cooked_{out}"}, "experience": 0.35, "cookingtime": time})
    # 3 黄豆 + 水桶 → 1 块豆腐（水桶合成后自动还回空桶）
    write(rec / "tofu.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                              "ingredients": [{"item": f"{MODID}:soybean"}] * 3 + [{"item": "minecraft:water_bucket"}],
                              "result": {"id": f"{MODID}:tofu", "count": 1}})
    for tag, add in [("cuttable", ["minecraft:potato", f"{MODID}:tofu"]), ("cut_piece_by_piece", ["minecraft:potato"])]:
        f = DATA / MODID / f"tags/item/{tag}.json"
        t = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"replace": False, "values": []}
        for v in add:
            if v not in t["values"]:
                t["values"].append(v)
        write(f, t)


if __name__ == "__main__":
    data_files()
