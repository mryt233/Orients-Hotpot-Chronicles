"""石料 / 金属桌面的火锅桌（2026-10-26 用户：桌子加其他材质的桌面——平滑石头、磨制安山岩……铁块；石头桌面，下面还是砖块）。

    python tools/gen_stone_tables.py
每种材质一张矮桌 <材质>_hotpot_table、一张高桌 <材质>_tall_hotpot_table：
  - 矮桌：形状同木头矮桌（tools/gen_tables.py 的 model），桌面板换成这种材质，下面的桌身换成原版红砖块。
  - 高桌：形状同木头高桌（tools/gen_tall.py 的 table_model / table_open_model），本来下面就是红砖，上面木头那层换成这种材质。
  桌面朝上那一面用材质的顶面贴图（石英、平滑石头的顶面和侧面不一样）。
方块状态、物品模型、配方、战利品表、镐子挖掘标签一起写。配方同木头桌：矮桌 材料围一圈 + 中间一块红砖 → 4；
高桌 一排材料 + 一排红砖块 → 3（铁的用铁锭，不然要 8 个铁块太贵）。
作者：mryt233
"""
import json
from pathlib import Path

import gen_tables as low
import gen_tall as tall

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "common/src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID]
DATA = RES / "data"

# 材质 → (配方里用的物品, 顶面贴图, 侧面贴图)；和 HotPotContent.STONE_TABLES 同序
MATERIALS = {
    "smooth_stone": ("minecraft:smooth_stone", "smooth_stone", "smooth_stone_slab_side"),
    "polished_andesite": ("minecraft:polished_andesite", "polished_andesite", "polished_andesite"),
    "polished_diorite": ("minecraft:polished_diorite", "polished_diorite", "polished_diorite"),
    "polished_granite": ("minecraft:polished_granite", "polished_granite", "polished_granite"),
    "polished_deepslate": ("minecraft:polished_deepslate", "polished_deepslate", "polished_deepslate"),
    "polished_blackstone": ("minecraft:polished_blackstone", "polished_blackstone", "polished_blackstone"),
    "quartz": ("minecraft:quartz_block", "quartz_block_top", "quartz_block_side"),
    "cut_copper": ("minecraft:cut_copper", "cut_copper", "cut_copper"),
    "iron": ("minecraft:iron_ingot", "iron_block", "iron_block"),
}
BRICKS = "minecraft:block/bricks"
DIRS = ["north", "east", "south", "west"]


def retexture_top(element):
    """桌面板：朝上一面用 #top，其余（侧面、底面）用 #planks（= 这种材质的侧面）。"""
    for d, f in element["faces"].items():
        f["texture"] = "#top" if d == "up" else "#planks"


def low_model(mask, top, side):
    m = low.model(mask, f"minecraft:block/{side}")
    retexture_top(m["elements"][0])
    for f in m["elements"][1]["faces"].values():      # 桌身 → 红砖
        f["texture"] = "#bricks"
    m["textures"].update({"top": f"minecraft:block/{top}", "bricks": BRICKS, "particle": f"minecraft:block/{side}"})
    return m


def tall_model(mask, top, side, open_=False):
    m = (tall.table_open_model if open_ else tall.table_model)(mask, f"minecraft:block/{side}")
    retexture_top(m["elements"][0])
    m["textures"].update({"top": f"minecraft:block/{top}", "bricks": BRICKS, "particle": f"minecraft:block/{side}"})
    return m


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def key_of(mask):
    return ",".join(f"{d}={'true' if mask & b else 'false'}" for d, b in zip(DIRS, [1, 2, 4, 8]))


def main():
    tall_display = dict(tall.BLOCK_DISPLAY, gui={"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]})
    for root in ASSET_ROOTS:
        blocks = root / "models/block"
        for mat, (_, top, side) in MATERIALS.items():
            # 矮桌
            name = f"{mat}_hotpot_table"
            variants = {}
            for mask in range(16):
                write(blocks / f"{name}{low.suffix(mask)}.json", low_model(mask, top, side))
                variants[key_of(mask)] = {"model": f"{MODID}:block/{name}{low.suffix(mask)}"}
            write(root / f"blockstates/{name}.json", {"variants": variants})
            # 物品图标：东南西三面接桌的形状，物品里没有邻居挡着，六个面都画（同 gen_tables.py）
            item = low_model(0b1110, top, side)
            for el in item["elements"]:
                el["faces"] = low.faces(set(), cull_down=False)
            retexture_top(item["elements"][0])
            for f in item["elements"][1]["faces"].values():
                f["texture"] = "#bricks"
            write(root / f"models/item/{name}.json", item)
            # 高桌
            name = f"{mat}_tall_hotpot_table"
            variants = {}
            for mask in range(16):
                write(blocks / f"{name}{tall.suffix(mask)}.json", tall_model(mask, top, side))
                write(blocks / f"{name}_open{tall.suffix(mask)}.json", tall_model(mask, top, side, open_=True))
                variants[f"{key_of(mask)},open=false"] = {"model": f"{MODID}:block/{name}{tall.suffix(mask)}"}
                variants[f"{key_of(mask)},open=true"] = {"model": f"{MODID}:block/{name}_open{tall.suffix(mask)}"}
            write(root / f"blockstates/{name}.json", {"variants": variants})
            write(root / f"models/item/{name}.json", {"parent": f"{MODID}:block/{name}_esw", "display": tall_display})

    loot = lambda n: {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                      "conditions": [{"condition": "minecraft:survives_explosion"}],
                      "entries": [{"type": "minecraft:item", "name": f"{MODID}:{n}"}]}]}
    ids = []
    for mat, (item_id, _, _) in MATERIALS.items():
        lo, hi = f"{mat}_hotpot_table", f"{mat}_tall_hotpot_table"
        ids += [lo, hi]
        write(DATA / MODID / f"recipe/{lo}.json", {
            "type": "minecraft:crafting_shaped", "group": f"{MODID}:hotpot_table", "pattern": ["PPP", "PBP", "PPP"],
            "key": {"P": {"item": item_id}, "B": {"item": "minecraft:brick"}}, "result": {"id": f"{MODID}:{lo}", "count": 4}})
        write(DATA / MODID / f"recipe/{hi}.json", {
            "type": "minecraft:crafting_shaped", "group": f"{MODID}:tall_hotpot_table", "pattern": ["PPP", "BBB"],
            "key": {"P": {"item": item_id}, "B": {"item": "minecraft:bricks"}}, "result": {"id": f"{MODID}:{hi}", "count": 3}})
        write(DATA / MODID / f"loot_table/blocks/{lo}.json", loot(lo))
        write(DATA / MODID / f"loot_table/blocks/{hi}.json", loot(hi))
    f = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    tag = json.loads(f.read_text(encoding="utf-8"))
    for i in ids:
        if f"{MODID}:{i}" not in tag["values"]:
            tag["values"].append(f"{MODID}:{i}")
    write(f, tag)
    print(f"已生成 {len(MATERIALS)} 种石料 / 金属桌面的矮桌和高桌")


if __name__ == "__main__":
    main()
