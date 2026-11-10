"""摆在桌上的芝麻酱 / 蚝油瓶（r146a，用户 2026-11-08：两种调料要能摆上桌、摆着能用）：方块状态 + 模型。

    python tools/gen_sauce_bottle.py
造型照它们的物品图标（tools 里 r137 画的 item/sesame_paste.png / item/oyster_sauce.png）：
  芝麻酱 = 带白标签的矮玻璃罐（瓶身 4×4、高 4，上面一圈瓶肩和深色瓶盖，总高 6 像素）
  蚝油   = 细高的深色方瓶（瓶身 4×4、高 6，瓶口一圈金黄瓶盖，总高 8 像素）
方块大小和旁边的蘸料碗（7×7、3 像素高）配得上，也装得进盘子格借来的 S1X1（5×5）占地。
两瓶都是「只带模型」的方块（真正摆在哪由 PlateHolderBlock 的盘子格记着，见 hotpot/SauceBottleBlock），
所以这里只写 blockstates/sauce_bottle.json 和 models/block/sauce_bottle_<调料>.json，物品模型用原来的平面图标。
高度（像素）要和 SauceBottleKind#bottleHeight 一致。
作者：mryt233
"""
import json


# mryt233 改（2026-11-10）：共用路径、保留原资源格式。
from pixkit.paths import MODID, RES
from pixkit.io import write_legacy_json as write

from PIL import Image

ASSET_ROOTS = [RES / "assets" / MODID]

FACES = ["north", "east", "south", "west", "up", "down"]

# 每种瓶子的形状（都和物品图标一色：芝麻酱是玻璃罐 + 白标签 + 棕色酱，蚝油是深棕瓶 + 金黄瓶盖）
BOTTLES = {
    "sesame_paste": {
        "height": 6.0,
        # (from, to, 贴图名)
        "parts": [
            ([6.0, 0.0, 6.0], [10.0, 3.2, 10.0], "paste"),      # 罐身下半：透过玻璃看到的酱（棕黄）
            ([5.9, 3.2, 5.9], [10.1, 4.6, 10.1], "label"),      # 白标签（比罐身略微凸出一圈，像套上去的纸标）
            ([6.2, 4.6, 6.2], [9.8, 5.2, 9.8], "glass_hi"),     # 玻璃肩（收进去一点）
            ([6.5, 5.2, 6.5], [9.5, 6.0, 9.5], "cap"),          # 深色瓶盖
        ],
    },
    "oyster_sauce": {
        "height": 8.0,
        "parts": [
            ([6.0, 0.0, 6.0], [10.0, 2.4, 10.0], "body"),       # 深棕瓶身下半 4×4
            ([6.05, 2.4, 6.05], [9.95, 4.4, 9.95], "gold"),     # 金黄标签带（贴住瓶身，别和它打架）
            ([6.0, 4.4, 6.0], [10.0, 6.0, 10.0], "body"),       # 瓶身上半
            ([6.4, 6.0, 6.4], [9.6, 6.6, 9.6], "neck"),         # 瓶颈
            ([6.3, 6.6, 6.3], [9.7, 8.0, 9.7], "gold_hi"),      # 瓶盖
        ],
    },
}

# 贴图区域（16×16 里的一小块），六面共用同一块：颜色平涂 + 一条高光，和别的方块贴图一个风格
UVS = {
    "paste": [0, 0, 4, 4],
    "label": [0, 4, 4, 6],
    "glass_hi": [4, 0, 7, 2],
    "cap": [4, 2, 6, 4],
    "body": [8, 0, 12, 4],
    "neck": [8, 4, 11, 6],
    "gold": [0, 8, 4, 10],
    "gold_hi": [4, 8, 7, 10],
}

# 颜色照物品图标取（tools/gen_garnish.py / r137 画的 item/sesame_paste.png、item/oyster_sauce.png）
COLORS = {
    # 芝麻酱：罐里是棕黄的酱，白纸标签、深灰瓶盖，瓶肩露一点玻璃的冷白
    "paste": (158, 108, 62), "label": (245, 240, 225), "glass_hi": (196, 214, 226), "cap": (75, 70, 78),
    # 蚝油：深棕瓶身、金黄标签带、亮一点的金黄瓶盖
    "body": (98, 46, 20), "neck": (120, 62, 30), "gold": (230, 204, 126), "gold_hi": (246, 236, 198),
}


def texture():
    """一张 16×16 的贴图：按 UVS 把每种色块画上去，块里加一条斜向高光，剩下填成玻璃那种半透明的灰蓝。"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for name, (u0, v0, u1, v1) in UVS.items():
        base = COLORS[name]
        hi = tuple(min(255, c + 26) for c in base)
        for y in range(v0, v1):
            for x in range(u0, u1):
                px[x, y] = base + (255,)
        # 一条从左上到右下的高光，让平涂的色块看起来有体积
        for y in range(v0, v1):
            for x in range(u0, u1):
                if (x - u0) + (y - v0) == 0 or (x - u0) == (u1 - u0) - 1:
                    px[x, y] = hi + (255,)
    return im


def box(f, t, key):
    uv = UVS[key]
    return {"from": f, "to": t,
            "faces": {d: {"texture": "#tex", "uv": list(uv)} for d in FACES}}


def model(kind):
    info = BOTTLES[kind]
    els = [box(f, t, key) for f, t, key in info["parts"]]
    return {"parent": "minecraft:block/block",
            "textures": {"tex": f"{MODID}:block/sauce_bottle_{kind}",
                         "particle": f"{MODID}:block/sauce_bottle_{kind}"},
            "elements": els}


def game_files():
    """方块状态（kind × axis 两种朝向）和两种瓶子的方块模型。"""
    for root in ASSET_ROOTS:
        (root / "textures/block").mkdir(parents=True, exist_ok=True)
        for kind in BOTTLES:
            texture().save(root / f"textures/block/sauce_bottle_{kind}.png")
            (root / "models/block").mkdir(parents=True, exist_ok=True)
            write(root / f"models/block/sauce_bottle_{kind}.json", model(kind), ensure_ascii=True)
        variants = {}
        for kind in BOTTLES:
            for axis in ("x", "z"):
                v = {"model": f"{MODID}:block/sauce_bottle_{kind}"}
                if axis == "z":
                    v["y"] = 90
                variants[f"axis={axis},kind={kind}"] = v
        (root / "blockstates").mkdir(parents=True, exist_ok=True)
        write(root / "blockstates/sauce_bottle.json", {"variants": variants}, ensure_ascii=True)
    print("ok")


if __name__ == "__main__":
    game_files()
