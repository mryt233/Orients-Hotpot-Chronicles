"""生成每种原版木板的火锅桌：方块状态、16 种连接模型、物品模型、配方、战利品表、斧头挖掘标签。

用法（在 mod/ 目录）：
    python tools/gen_tables.py
形状（与 HotPotTableBlock 的常量一致）：
  - 桌面板：y 7..11（与灶台台面 11/16 齐平），永远铺满整格，几块拼起来是一整张桌面。
  - 桌身：y 0..7，像倒放的楼梯——朝外（没有接桌子/灶台）的一侧收进去半格，留出放腿的空间；
    接着桌子/灶台的一侧铺到方块边界；两边都没接时收成中间 8 像素宽的一截。
连接位掩码：北 1 / 东 2 / 南 4 / 西 8，模型后缀按 n e s w 顺序拼字母（全没接 = 无后缀）。
"""
import json
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]
DATA = RES / "data"

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]

TOP_Y0, TOP_Y1 = 7, 11
BASE_Y1 = TOP_Y0
# 桌身朝外一侧收进去多少（像素）；用户 r82 觉得半格（8）腿部空间太大，改 4
RECESS = 4
BITS = [("n", 1), ("e", 2), ("s", 4), ("w", 8)]


def suffix(mask):
    s = "".join(c for c, b in BITS if mask & b)
    return "_" + s if s else ""


def base_box(mask):
    """桌身在 x/z 上的范围：接邻的一侧到边，朝外一侧收进去。"""
    def span(lo_bit, hi_bit):
        lo_conn, hi_conn = bool(mask & lo_bit), bool(mask & hi_bit)
        lo = 0 if lo_conn else RECESS
        hi = 16 if hi_conn else 16 - RECESS
        return lo, hi
    x0, x1 = span(8, 2)   # 西 / 东
    z0, z1 = span(1, 4)   # 北 / 南
    return x0, x1, z0, z1


def faces(skip, cull_down=False):
    # 只有贴着方块底面（y=0）的那一面才能按下方方块剔除；桌面板底面悬空在 y=7，不能剔除
    return {f: {"texture": "#planks", **({"cullface": f} if f == "down" and cull_down else {})}
            for f in ["north", "east", "south", "west", "up", "down"] if f not in skip}


def model(mask, planks):
    connected = {d for d, b in zip(["north", "east", "south", "west"], [1, 2, 4, 8]) if mask & b}
    x0, x1, z0, z1 = base_box(mask)
    base_skip = set(connected) | {"up"}
    return {
        "parent": "minecraft:block/block",
        "textures": {"planks": planks, "particle": planks},
        "elements": [
            {"from": [0, TOP_Y0, 0], "to": [16, TOP_Y1, 16], "faces": faces(connected)},
            {"from": [x0, 0, z0], "to": [x1, BASE_Y1, z1], "faces": faces(base_skip, cull_down=True)},
        ],
    }


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def main():
    for root in ASSET_ROOTS:
        # 旧的单一火锅桌（自绘贴图）全部删掉
        for f in list((root / "models/block").glob("hotpot_table*.json")) + \
                 list((root / "textures/block").glob("hotpot_table_*.png")) + \
                 [root / "blockstates/hotpot_table.json", root / "models/item/hotpot_table.json"]:
            if f.exists():
                f.unlink()
        for wood in WOODS:
            name = f"{wood}_hotpot_table"
            planks = f"minecraft:block/{wood}_planks"
            variants = {}
            for mask in range(16):
                write(root / f"models/block/{name}{suffix(mask)}.json", model(mask, planks))
                key = ",".join(f"{d}={'true' if mask & b else 'false'}"
                               for d, b in zip(["north", "east", "south", "west"], [1, 2, 4, 8]))
                variants[key] = {"model": f"{MODID}:block/{name}{suffix(mask)}"}
            write(root / f"blockstates/{name}.json", {"variants": variants})
            # 物品图标：东南西三面接桌的形状 = 一块倒放的楼梯；物品里没有邻居挡着，所有面都要画
            item = model(0b1110, planks)
            for el in item["elements"]:
                el["faces"] = faces(set(), cull_down=False)
            write(root / f"models/item/{name}.json", item)

    for f in [DATA / MODID / "recipe/hotpot_table.json", DATA / MODID / "loot_table/blocks/hotpot_table.json"]:
        if f.exists():
            f.unlink()
    for wood in WOODS:
        name = f"{wood}_hotpot_table"
        write(DATA / MODID / f"recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "group": f"{MODID}:hotpot_table",
            "pattern": ["PPP", "PBP", "PPP"],
            "key": {"P": {"item": f"minecraft:{wood}_planks"}, "B": {"item": "minecraft:brick"}},
            "result": {"id": f"{MODID}:{name}", "count": 4},
        })
        write(DATA / MODID / f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "conditions": [{"condition": "minecraft:survives_explosion"}],
                       "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}]}],
        })
    # 和 gen_benches.py 共用这个标签文件：只补自己的条目，不覆盖条凳的
    axe = DATA / "minecraft/tags/block/mineable/axe.json"
    tag = json.loads(axe.read_text(encoding="utf-8")) if axe.exists() else {"replace": False, "values": []}
    for w in WOODS:
        if f"{MODID}:{w}_hotpot_table" not in tag["values"]:
            tag["values"].append(f"{MODID}:{w}_hotpot_table")
    write(axe, tag)
    print(f"已生成 {len(WOODS)} 种火锅桌（每种 16 个连接模型）")


if __name__ == "__main__":
    main()
