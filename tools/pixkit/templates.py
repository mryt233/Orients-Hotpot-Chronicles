"""同形模型共用父几何，保留全部既有模型 ID。作者：mryt233"""
import copy
import re
from pathlib import Path
from .io import write_legacy_json
from .materials import ids
from .paths import MODID


def template_name(path: Path) -> str | None:
    name = path.stem
    if re.fullmatch(r"(?:" + "|".join(ids("woods") + ["iron"]) + r")_sauce_station(?:_empty)?", name):
        return "sauce_station_empty" if name.endswith("_empty") else "sauce_station_full"
    for material in ids("woods") + ids("stone_tables"):
        if not name.startswith(material + "_"):
            continue
        shape = name[len(material) + 1:]
        if re.fullmatch(r"(?:tall_)?hotpot_table(?:_open)?(?:_[nesw]+)?", shape):
            kind = "stone_" if material in ids("stone_tables") else ""
            return kind + shape + ("_item" if path.parent.name == "item" else "")
        if re.fullmatch(r"(?:bench|stool)(?:_left|_right|_middle)?", shape):
            return shape + ("_item" if path.parent.name == "item" else "")
    for material in ids("plates"):
        if name.startswith(material + "_"):
            shape = name[len(material) + 1:]
            if shape in ("small_dish", "two_slot_plate", "three_slot_long_plate", "four_slot_square_plate", "five_slot_long_plate", "eight_slot_platter"):
                return "plate_" + shape
    return None


def write_shared_model(path, obj, **kwargs):
    path = Path(path)
    shape = template_name(path) if obj.get("elements") and path.parent.name in ("block", "item") else None
    if shape:
        # mryt233 改（2026-11-10）：只拆木质桌面的槽位；高桌原有红砖段保留独立材质。
        if "hotpot_table" in shape and not shape.startswith("stone_"):
            obj = copy.deepcopy(obj)
            old = obj["textures"].pop("planks")
            obj["textures"].update(top=old, base=old)
            for index, element in enumerate(obj["elements"]):
                for face in element.get("faces", {}).values():
                    if face["texture"] == "#planks":
                        face["texture"] = "#top" if index == 0 else "#base"
        root = path.parent.parent
        parent = root / "block/template" / (shape + ".json")
        geometry = {"credit": "作者：mryt233；mryt233 改（2026-11-10）", **{key: value for key, value in obj.items() if key != "textures"}}
        if parent.exists():
            import json
            previous = json.loads(parent.read_text(encoding="utf-8"))
            if {k: v for k, v in previous.items() if k != "credit"} != {k: v for k, v in geometry.items() if k != "credit"}:
                raise ValueError(f"同名父模型几何不一致：{shape}")
        write_legacy_json(parent, geometry)
        obj = {"parent": f"{MODID}:block/template/{shape}", "textures": obj["textures"]}
    write_legacy_json(path, obj, **kwargs)
