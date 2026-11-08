"""火锅食材（2026-10-23 用户：一样样加，先做原版有的；这批：土豆片 + 豆腐）。画贴图和物品模型：

  土豆片 potato_slice / 熟土豆片 cooked_potato_slice   原版土豆在砧板上切 3 片（切石机也行），熟的每片回 2
  黄豆 soybean                                          打草掉（像小葱 / 香菜），以后再考虑做成作物
  豆腐（冷） cold_tofu                                  3 黄豆 + 水桶在工作台做一整块（桶留下）
  豆腐（热） hot_tofu                                   整块冷豆腐在锅里煮 10 秒（锅专用产物，见 HotPotContent）
  豆腐块（冷） cold_tofu_cube / 豆腐块（热） hot_tofu_cube   一整块豆腐在砧板上切 3 块；煮熟过头也会碎成热的

  mryt233 改（2026-11-04）：豆腐分冷热（用户定）。冷豆腐 / 冷豆腐块沿用原来的贴图配色，
  热豆腐是新做的一张偏暖、略黄的「煮过」配色（照熟豆腐块和豆腐块的色差），热豆腐块沿用原熟豆腐块的图。

    python tools/gen_ingredients.py
作者：mryt233（2026-11-01 mryt233 改：改用 tools/pixkit 公共库；JSON 统一写 LF，贴图逐字节不变）
"""
import json
import random

from PIL import Image, ImageDraw

from pixkit import canvas, io, model as pm
from pixkit.canvas import iso_cube as cube, outline_inner as outline
from pixkit.color import shade_floor as shade
from pixkit.paths import MODID, RES, ROOTS


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


# ---------------------------------------------------------------- 豆腐（等角小方块，canvas.iso_cube）
# 整块豆腐顶上的一圈轮廓（2026-10-26 用户：顶角那块看着塌陷；尺寸、细节都不变，只修这里；调整）：
# 多边形填出来的顶棱台阶是 3 / 2 / 1 格，越往上越窄，看着往里凹；改成两边都是 2 格一台阶、顶上平平 3 格。
# 每行：(行, 描边的列, 顶面的列)
TOFU_TOP = [(2, (6, 7, 8), ()), (3, (4, 5, 9, 10), (6, 7, 8)), (4, (2, 3, 11, 12), tuple(range(4, 11))),
            (5, (0, 1, 13, 14), tuple(range(2, 13)))]


# mryt233 改（2026-11-04）：豆腐改名分冷热。冷豆腐 / 冷豆腐块 = 原来的配色；
# 热豆腐是新做的一张「煮过」配色 —— 比冷豆腐整体偏暖、略黄，色差照原来熟豆腐块和豆腐块的比例取。
TOFU = {
    "cold_tofu": ((252, 250, 240), (230, 226, 210), (208, 202, 184)),
    "hot_tofu": ((248, 233, 196), (226, 205, 160), (200, 175, 126)),            # 煮过：偏暖、略黄
    "cold_tofu_cube": ((252, 250, 240), (230, 226, 210), (208, 202, 184)),
    "hot_tofu_cube": ((244, 222, 168), (222, 192, 132), (196, 162, 104)),      # 汤里煮过：吸了汤，微黄
}


def tofu_texture(name):
    """一整块：扁扁的大方块；豆腐块：三小块，每块单独描边后从后往前叠，才分得开。"""
    top, left, right = TOFU[name]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    whole = name in ("cold_tofu", "hot_tofu")
    cubes = [(7.5, 2, 14, 6)] if whole else [(10.5, 1.0, 8, 4), (4.5, 4.5, 8, 4), (9.5, 7.5, 8, 4)]
    rnd = random.Random(len(name))
    for cx, ty, w, h in cubes:
        layer = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        cube(ImageDraw.Draw(layer), cx, ty, w, h, top + (255,), left + (255,), right + (255,))
        canvas.speckle(layer, rnd, 0.05, 0.92, fn=shade)                               # 细细的气孔
        outline(layer, shade(right, 0.62))
        if whole:
            px = layer.load()
            for y in range(0, 2):
                for x in range(16):
                    px[x, y] = (0, 0, 0, 0)
            for y, edge, face in TOFU_TOP:
                for x in range(16):
                    px[x, y] = (0, 0, 0, 0) if y == 2 else px[x, y]
                for x in edge:
                    px[x, y] = shade(right, 0.62) + (255,)
                for x in face:
                    px[x, y] = top + (255,)
        im.alpha_composite(layer)
    return im


write = io.write_json


def item_model(root, name):
    write(root / f"models/item/{name}.json", pm.item_generated(name))


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
    # mryt233 改（2026-11-04）：豆腐改名分冷热（用户定）。
    # 切石机：冷豆腐 → 冷豆腐块（3），热豆腐 → 热豆腐块（3，和冷豆腐一样的切法）。
    # 烧炼 / 烟熏 / 营火只做「冷豆腐块 → 热豆腐块」；整块热豆腐是锅煮出来的（HotPotContent.potResult），没有熔炉配方。
    for raw, out in [("minecraft:potato", "potato_slice"), (f"{MODID}:cold_tofu", "cold_tofu_cube"),
                     (f"{MODID}:hot_tofu", "hot_tofu_cube")]:
        write(rec / f"{out}_from_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": {"item": raw},
                                                     "result": {"id": f"{MODID}:{out}", "count": 3}})
    for kind, time, suffix in [("smelting", 100, ""), ("smoking", 50, "_from_smoking"), ("campfire_cooking", 300, "_from_campfire_cooking")]:
        write(rec / f"hot_tofu_cube{suffix}.json", {"type": f"minecraft:{kind}", "category": "food", "ingredient": {"item": f"{MODID}:cold_tofu_cube"},
                                                    "result": {"id": f"{MODID}:hot_tofu_cube"}, "experience": 0.35, "cookingtime": time})
    # 3 黄豆 + 水桶 → 1 块冷豆腐（水桶合成后自动还回空桶）
    write(rec / "cold_tofu.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                   "ingredients": [{"item": f"{MODID}:soybean"}] * 3 + [{"item": "minecraft:water_bucket"}],
                                   "result": {"id": f"{MODID}:cold_tofu", "count": 1}})
    for tag, add in [("cuttable", ["minecraft:potato", f"{MODID}:cold_tofu", f"{MODID}:hot_tofu"]),
                     ("cut_piece_by_piece", ["minecraft:potato"])]:
        f = DATA / MODID / f"tags/item/{tag}.json"
        t = json.loads(f.read_text(encoding="utf-8")) if f.exists() else {"replace": False, "values": []}
        for v in add:
            if v not in t["values"]:
                t["values"].append(v)
        write(f, t)


if __name__ == "__main__":
    data_files()
