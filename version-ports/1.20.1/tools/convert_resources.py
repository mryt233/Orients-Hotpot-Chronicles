# 作者：mryt233
"""从 1.21.1 主工程资源生成 1.20.1 data：目录复数化、配方 result 格式、进度 ItemPredicate 格式。

1.20.1 与 1.21.1 的 data 差异（mryt233 改，0.2.2-alpha 起以主工程为底本）：
  * 目录复数：recipe→recipes、loot_table→loot_tables、advancement→advancements、
    tags/item→tags/items、tags/block→tags/blocks
  * 配方 result：1.21.1 是 {"id": "...", "count": n}，1.20.1 的 smelting 系列与 stonecutting
    是字符串 + 顶层 count，crafting 系列是 {"item": "...", "count": n}
  * 进度 inventory_changed 的 ItemPredicate：1.21.1 允许 "items" 直接写字符串（单数兼容），
    1.20.1 必须是字符串数组，否则整条进度解析失败
  * 方块掉落里的 copy_components 函数（拷贝锅内容）1.20.1 没有，改为空（1.20.1 由
    HotPotBreaking 在方块实体里直接写掉落物）
  * c/tags/item/foods.json 在 1.20.1 要两份：c:tags/items/foods.json（Fabric）
    与 forge:tags/items/foods.json（Forge）
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# 只读底本：1.21.1 主工程资源目录
BASE = ROOT.parent.parent / "mod" / "common" / "src" / "main" / "resources"
OUTPUT = ROOT / "common" / "src" / "main" / "resources"

NAMES = {"recipe": "recipes", "loot_table": "loot_tables", "advancement": "advancements",
         "predicate": "predicates", "item_modifier": "item_modifiers",
         "function": "functions", "structure": "structures"}
TAGS = {"item": "items", "block": "blocks", "entity_type": "entity_types",
        "fluid": "fluids", "game_event": "game_events"}

COOKING_RECIPES = {"minecraft:smelting", "minecraft:blasting", "minecraft:smoking",
                   "minecraft:campfire_cooking", "minecraft:stonecutting"}

# 1.20.1 不存在的配方 / 战利品表（#108 豆腐改 ID，alpha 期间不做旧 ID 兼容）
DROPPED = {
    ("recipes", "tofu.json"),
    ("recipes", "tofu_cube_from_stonecutting.json"),
    ("recipes", "cooked_tofu_cube.json"),
    ("recipes", "cooked_tofu_cube_from_smelting.json"),
    ("recipes", "cooked_tofu_cube_from_smoking.json"),
    ("recipes", "cooked_tofu_cube_from_campfire_cooking.json"),
    ("recipes", "hot_tofu_cube_from_smelting.json"),
    ("recipes", "hot_tofu_cube_from_smoking.json"),
    ("recipes", "hot_tofu_cube_from_campfire_cooking.json"),
}


def convert_recipe(data):
    """配方 result 转 1.20.1 写法。"""
    result = data.get("result")
    if not isinstance(result, dict):
        return data
    item = result.get("id", result.get("item"))
    count = result.get("count", 1)
    if data.get("type") in COOKING_RECIPES:
        data["result"] = item
        if data["type"] == "minecraft:stonecutting":
            data["count"] = count
    else:
        new_result = {"item": item}
        if count != 1:
            new_result["count"] = count
        data["result"] = new_result
    return data


def convert_loot(data):
    """方块掉落的函数表：去掉 1.20.1 没有的 copy_components（锅内容由方块实体自己塞）。"""
    for pool in data.get("pools", []):
        for entry in pool.get("entries", []):
            functions = entry.get("functions")
            if not functions:
                continue
            kept = [f for f in functions if f.get("function") != "minecraft:copy_components"]
            if kept:
                entry["functions"] = kept
            else:
                entry.pop("functions", None)
    return data


def convert_item_predicate(predicate):
    """1.21.1 的 ItemPredicate 里 "items" 可以是字符串；1.20.1 必须是数组。"""
    if not isinstance(predicate, dict):
        return predicate
    items = predicate.get("items")
    if isinstance(items, str):
        predicate["items"] = [items]
    return predicate


def convert_advancement(data):
    """进度的 icon 用 "item"，inventory_changed 条件里的 items 数组元素转 1.20.1。"""
    display = data.get("display")
    if isinstance(display, dict):
        icon = display.get("icon")
        if isinstance(icon, dict) and "id" in icon:
            icon["item"] = icon.pop("id")
    for criterion in data.get("criteria", {}).values():
        conditions = criterion.get("conditions")
        if not isinstance(conditions, dict):
            continue
        items = conditions.get("items")
        if isinstance(items, list):
            for predicate in items:
                convert_item_predicate(predicate)
    return data


def main():
    source_data = BASE / "data"
    if not source_data.is_dir():
        raise SystemExit(f"主工程资源目录不存在：{source_data}")

    written = 0
    for source in source_data.rglob("*.json"):
        parts = list(source.relative_to(source_data).parts)
        namespace = parts[0]
        category = parts[1]

        if category == "tags":
            parts[2] = TAGS.get(parts[2], parts[2])
        else:
            parts[1] = NAMES.get(category, category)

        rel = "/".join(parts[2:]) if parts[1] == "tags" else "/".join(parts[1:])
        key = (parts[1], rel)
        if key in DROPPED:
            continue

        data = json.loads(source.read_text(encoding="utf-8"))
        if parts[1] == "recipes":
            data = convert_recipe(data)
        elif parts[1] == "loot_tables":
            data = convert_loot(data)
        elif parts[1] == "advancements":
            data = convert_advancement(data)

        target = OUTPUT / "data" / namespace / Path(*parts[1:])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        written += 1

    # 1.20.1 的通用食物标签：Fabric 用 c:，Forge 用 forge:
    foods = json.loads((source_data / "c" / "tags" / "item" / "foods.json").read_text(encoding="utf-8"))
    for target_rel in (("c", "tags", "items", "foods.json"), ("forge", "tags", "items", "foods.json")):
        target = OUTPUT / "data" / Path(*target_rel)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(foods, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        written += 1

    # 清掉本脚本负责的旧目录里已不存在的文件（不递归删目录，只删文件）
    wanted = {p.relative_to(OUTPUT / "data").as_posix()
              for p in (OUTPUT / "data").rglob("*.json")}
    removed = 0
    for namespace_dir in (OUTPUT / "data").iterdir():
        if not namespace_dir.is_dir():
            continue
        for target in namespace_dir.rglob("*.json"):
            rel = target.relative_to(OUTPUT / "data").as_posix()
            if rel not in wanted:
                continue
            # 只清本脚本产出的分类：recipes / loot_tables / advancements / tags
            # （c / forge 的通用食物标签是本脚本额外生成的一份，源目录里没有对应文件，跳过）
            parts_of_rel = rel.split("/")
            if len(parts_of_rel) < 2:
                continue
            if parts_of_rel[0] in {"c", "forge"}:
                continue
            parent = parts_of_rel[1]
            if parent not in {"recipes", "loot_tables", "advancements", "tags"}:
                continue
            source_parts = list(Path(rel).parts)
            if source_parts[1] == "recipes":
                source_parts[1] = "recipe"
            elif source_parts[1] == "loot_tables":
                source_parts[1] = "loot_table"
            elif source_parts[1] == "advancements":
                source_parts[1] = "advancement"
            elif source_parts[1] == "tags":
                rev = {v: k for k, v in TAGS.items()}
                source_parts[2] = rev.get(source_parts[2], source_parts[2])
            if not (source_data.joinpath(*source_parts)).is_file():
                target.unlink()
                removed += 1

    pack = OUTPUT / "pack.mcmeta"
    pack.write_text(json.dumps({"pack": {"pack_format": 15,
        "description": "Orient's Hotpot Chronicles"}}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")
    print(f"wrote {written} files, removed {removed} stale files")


if __name__ == "__main__":
    main()
