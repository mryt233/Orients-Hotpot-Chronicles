"""生成盘子：5 种材质（石英、竹编、红陶、铜、铁）× 6 种大小的方块状态、模型、物品模型、配方，以及「盘子格」的方块状态。

用法（在 mod/ 目录）：
    python tools/gen_plates.py
形状（与 PlateSize / PlateBlock 一致）：长边沿 x（axis=z 时模型转 90°），中心在方块中央；
盘底 y 0..1，四周盘沿高 0.75、厚 0.5。盘子不单独占方块：摆在「盘子格」里，由渲染器按位置平移、贴着桌面画。
合成：2 份材料横排 → 2 个小碟（1x1，石英用下界石英）；大盘用同材质小碟拼，几格用几个。
"""
import json
from pathlib import Path


MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]
DATA = RES / "data" / MODID

# id: (盘面贴图, 盘沿贴图, 合成材料)
MATERIALS = {
    "quartz": ("minecraft:block/quartz_block_top", "minecraft:block/quartz_block_side", "minecraft:quartz"),
    "bamboo": ("minecraft:block/bamboo_mosaic", "minecraft:block/bamboo_mosaic", "minecraft:bamboo"),
    "terracotta": ("minecraft:block/terracotta", "minecraft:block/terracotta", "minecraft:brick"),
    "copper": ("minecraft:block/copper_block", "minecraft:block/copper_block", "minecraft:copper_ingot"),
    "iron": ("minecraft:block/iron_block", "minecraft:block/iron_block", "minecraft:iron_ingot"),
}
# id: (列, 行, 长 px, 宽 px, 合成形状)
SIZES = {
    "1x1": (1, 1, 5, 5, None),
    "1x2": (2, 1, 11, 6, ["DD"]),
    "1x3": (3, 1, 14, 6, ["DDD"]),
    "2x2": (2, 2, 11, 11, ["DD", "DD"]),
    "1x5": (5, 1, 15, 6, ["D D", "DDD"]),
    "2x4": (4, 2, 15, 8, ["DDD", "D D", "DDD"]),
}
FLOOR, RIM_H, RIM_T = 1.0, 0.75, 0.5
PLATE_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [0.75, 0.75, 0.75]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.75, 0.75, 0.75]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 2], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 2], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [30, 45, 0], "translation": [0, 4, 0], "scale": [0.55, 0.55, 0.55]},
    "firstperson_lefthand": {"rotation": [30, 225, 0], "translation": [0, 4, 0], "scale": [0.55, 0.55, 0.55]},
}
TABLE_DROP = 5.0


def box(f, t, tex):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in ["north", "east", "south", "west", "up", "down"]}}


def model(size, floor_tex, rim_tex, drop):
    _, _, w, d, _ = SIZES[size]
    x0, x1, z0, z1 = 8 - w / 2, 8 + w / 2, 8 - d / 2, 8 + d / 2
    y0, y1, y2 = -drop, FLOOR - drop, FLOOR + RIM_H - drop
    els = [
        box([x0, y0, z0], [x1, y1, z1], "#plate"),
        box([x0, y1, z0], [x1, y2, z0 + RIM_T], "#rim"),
        box([x0, y1, z1 - RIM_T], [x1, y2, z1], "#rim"),
        box([x0, y1, z0 + RIM_T], [x0 + RIM_T, y2, z1 - RIM_T], "#rim"),
        box([x1 - RIM_T, y1, z0 + RIM_T], [x1, y2, z1 - RIM_T], "#rim"),
    ]
    return {"parent": "minecraft:block/block",
            "textures": {"plate": floor_tex, "rim": rim_tex, "particle": floor_tex},
            "elements": els}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def main():
    for root in ASSET_ROOTS:
        # 旧版白瓷（r81）的贴图、中间材料和桌上下移模型都不要了
        for old in list((root / "models/block").glob("*_plate_*_table.json")) + \
                [root / f"textures/block/porcelain_plate{t}.png" for t in ("", "_rim")] + \
                [root / f"textures/item/{i}.png" for i in ("porcelain_clay", "porcelain_shard")] + \
                [root / f"models/item/{i}.json" for i in ("porcelain_clay", "porcelain_shard")] + \
                list((root / "models/block").glob("porcelain_plate_*.json")) + \
                list((root / "models/item").glob("porcelain_plate_*.json")) + \
                list((root / "blockstates").glob("porcelain_plate_*.json")):
            if old.exists():
                old.unlink()
        write(root / "models/item/plate_with_food.json", {"parent": "minecraft:builtin/entity", "gui_light": "side"})
        # 盘子格：本身看不见（渲染器画盘子），模型只给挖的时候的碎屑用
        write(root / "blockstates/plates.json", {"multipart": [{"apply": {"model": f"{MODID}:block/plates"}}]})
        write(root / "models/block/plates.json", {"textures": {"particle": "minecraft:block/quartz_block_side"}})
        for mat, (floor_tex, rim_tex, _) in MATERIALS.items():
            for size in SIZES:
                name = f"{mat}_plate_{size}"
                write(root / f"models/block/{name}.json", model(size, floor_tex, rim_tex, 0))
                variants = {}
                for axis, y in (("x", 0), ("z", 90)):
                    v = {"model": f"{MODID}:block/{name}"}
                    if y:
                        v["y"] = y
                    variants[f"axis={axis}"] = v
                write(root / f"blockstates/{name}.json", {"variants": variants})
                # 手持往上翘一点能看到盘面（原来平着拿几乎只看到一条边，r91）；装了菜时切到 builtin 模型，
                # 由 HeldFoodItemRenderer 先画盘子再把菜摆上去（Claude）
                write(root / f"models/item/{name}.json", {
                    "parent": f"{MODID}:block/{name}", "display": PLATE_DISPLAY,
                    "overrides": [{"predicate": {"custom_model_data": 1}, "model": f"{MODID}:item/plate_with_food"}]})

    for old in [DATA / "recipe/porcelain_clay.json", DATA / "recipe/porcelain_shard.json"] + \
            list((DATA / "recipe").glob("porcelain_plate_*.json")) + list((DATA / "loot_table/blocks").glob("*_plate_*.json")):
        if old.exists():
            old.unlink()
    for mat, (_, _, material) in MATERIALS.items():
        dish = f"{MODID}:{mat}_plate_1x1"
        for size, (_, _, _, _, pattern) in SIZES.items():
            name = f"{mat}_plate_{size}"
            if pattern is None:
                recipe = {"type": "minecraft:crafting_shaped", "group": f"{MODID}:plate_1x1", "pattern": ["MM"],
                          "key": {"M": {"item": material}}, "result": {"id": dish, "count": 2}}
            else:
                recipe = {"type": "minecraft:crafting_shaped", "group": f"{MODID}:plate_{size}", "pattern": pattern,
                          "key": {"D": {"item": dish}}, "result": {"id": f"{MODID}:{name}", "count": 1}}
            write(DATA / f"recipe/{name}.json", recipe)
    print(f"已生成 {len(MATERIALS)} 种材质 × {len(SIZES)} 种大小的盘子")


if __name__ == "__main__":
    main()
