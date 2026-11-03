# 作者：mryt233
"""从只读底本重新生成 1.20.1 data；assets 不进行格式转换。"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT.parent / "base" / "0.2.1-alpha" / "common/src/main/resources"
OUTPUT = ROOT / "common/src/main/resources"
NAMES = {"recipe": "recipes", "loot_table": "loot_tables", "advancement": "advancements",
         "predicate": "predicates", "item_modifier": "item_modifiers",
         "function": "functions", "structure": "structures"}
TAGS = {"item": "items", "block": "blocks", "entity_type": "entity_types",
        "fluid": "fluids", "game_event": "game_events"}


def convert(value):
    if isinstance(value, list):
        # 1.20.1 的锅内容在方块掉落路径打包，不使用组件拷贝函数。
        return [convert(x) for x in value if not (isinstance(x, dict) and
                x.get("function") == "minecraft:copy_components")]
    if isinstance(value, dict):
        return {k: convert(v) for k, v in value.items()}
    return value


for source in (BASE / "data").rglob("*.json"):
    parts = list(source.relative_to(BASE).parts)
    if parts[2] == "tags":
        parts[3] = TAGS.get(parts[3], parts[3])
    else:
        parts[2] = NAMES.get(parts[2], parts[2])
    target = OUTPUT.joinpath(*parts)
    data = convert(json.loads(source.read_text(encoding="utf-8")))
    if parts[2] == "recipes" and isinstance(data.get("result"), dict):
        result = data["result"]
        item = result.get("id", result.get("item"))
        if data.get("type") in {"minecraft:smelting", "minecraft:blasting", "minecraft:smoking",
                                "minecraft:campfire_cooking", "minecraft:stonecutting"}:
            data["result"] = item
            if data["type"] == "minecraft:stonecutting":
                data["count"] = result.get("count", 1)
        elif "id" in result:
            result["item"] = result.pop("id")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# 从初次复制留下的单数路径移除本脚本对应文件，不递归删除目录。
for source in (BASE / "data").rglob("*.json"):
    old = OUTPUT / source.relative_to(BASE)
    parts = source.relative_to(BASE).parts
    if parts[2] in NAMES or (parts[2] == "tags" and parts[3] in TAGS):
        if old.is_file():
            old.unlink()
(OUTPUT / "pack.mcmeta").write_text(json.dumps({"pack": {"pack_format": 15,
    "description": "Orient's Hotpot Chronicles"}}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
