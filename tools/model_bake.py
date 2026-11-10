"""按模型 parent 链展开几何、贴图和显示参数。作者：mryt233"""
from __future__ import annotations

import copy
import json
from pathlib import Path

from pixkit.paths import MODID, VANILLA


def qualify(ref: str) -> str:
    return ref if ":" in ref else "minecraft:" + ref


class ModelResolver:
    def __init__(self, resources: Path, vanilla: Path = VANILLA):
        self.resources, self.vanilla = Path(resources), Path(vanilla)
        self.cache = {}

    def load(self, ref: str) -> dict:
        ns, name = qualify(ref).split(":", 1)
        if name.startswith("builtin/"):
            return {}
        root = self.vanilla if ns == "minecraft" else self.resources / "assets" / ns
        return json.loads((root / "models" / (name + ".json")).read_text(encoding="utf-8"))

    def merged(self, ref: str, stack=()) -> dict:
        ref = qualify(ref)
        if ref in stack:
            raise ValueError("模型 parent 循环：" + " -> ".join((*stack, ref)))
        if ref in self.cache:
            return copy.deepcopy(self.cache[ref])
        own = self.load(ref)
        parent = own.get("parent")
        result = self.merged(parent, (*stack, ref)) if parent else {
            "elements": [], "textures": {}, "display": {}, "ambientocclusion": True,
            "gui_light": "side", "builtin": ref if ":builtin/" in ref else None,
        }
        result["textures"].update(own.get("textures", {}))
        result["display"].update(own.get("display", {}))
        # 原版空 elements 与省略 elements 都继承父几何。
        if own.get("elements"):
            result["elements"] = own["elements"]
        for key in ("ambientocclusion", "gui_light"):
            if key in own:
                result[key] = own[key]
        self.cache[ref] = copy.deepcopy(result)
        return result

    @staticmethod
    def texture(ref: str, textures: dict) -> str:
        seen = set()
        while ref.startswith("#"):
            key = ref[1:]
            if key in seen:
                raise ValueError("贴图变量循环：" + key)
            seen.add(key)
            if key not in textures:
                raise ValueError("未定义贴图变量：" + key)
            ref = textures[key]
        return qualify(ref)

    def bake(self, ref: str) -> dict:
        model = self.merged(ref)
        textures = model.pop("textures")
        for element in model["elements"]:
            for face in element.get("faces", {}).values():
                face["texture"] = self.texture(face["texture"], textures)
        model["particle"] = self.texture(textures["particle"], textures) if "particle" in textures else None
        if model.get("builtin") == "minecraft:builtin/generated":
            model["layers"] = {key: self.texture(value, textures) for key, value in textures.items() if key.startswith("layer")}
        return model


def compare(before: Path, after: Path) -> dict:
    old, new = ModelResolver(before), ModelResolver(after)
    folder = Path(before) / "assets" / MODID / "models"
    differences = []
    total = 0
    for file in sorted(folder.rglob("*.json")):
        ref = MODID + ":" + file.relative_to(folder).with_suffix("").as_posix()
        total += 1
        try:
            if old.bake(ref) != new.bake(ref):
                differences.append({"model": ref, "error": "烘焙结果不同"})
        except (OSError, ValueError) as exc:
            differences.append({"model": ref, "error": str(exc)})
    return {"models": total, "differences": differences}
