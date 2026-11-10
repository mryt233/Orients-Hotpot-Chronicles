"""条凳预览的几何与覆盖层路径；参数只读游戏源码。作者：mryt233"""
from __future__ import annotations

import copy
import math
import re
from pathlib import Path

from pixkit.paths import MOD, MODID, RES, SOURCE_RES

JAVA_PACKAGE = "cn/blockforge/generated/orientshotpotchronicles"
VARIANTS = {(False, False): "", (True, False): "_left",
            (False, True): "_right", (True, True): "_middle"}


def preview_root(resources: Path = RES) -> Path:
    resources = Path(resources).resolve()
    return MOD / "preview_models" if resources == SOURCE_RES else resources.with_name(resources.name + "-preview_models")


def java_parameters(root: Path = MOD) -> dict[str, float]:
    result = {}
    for source, names in ((f"main/java/{JAVA_PACKAGE}/hotpot/BenchBlock.java", ("LEG_TOP", "LEG_SPREAD")),
                          (f"client/java/{JAVA_PACKAGE}/client/BenchLegsRenderer.java", ("LEG_W", "LEG_T"))):
        text = (root / "common/src" / source).read_text(encoding="utf-8")
        text = re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)
        for name in names:
            found = re.findall(r"\bstatic\s+final\s+float\s+" + name + r"\s*=\s*([0-9]+(?:\.[0-9]+)?)[fF]?\s*;", text)
            if len(found) != 1 or not math.isfinite(float(found[0])) or float(found[0]) <= 0:
                raise ValueError(f"无法唯一读取正数 Java 常量：{name}（{source}）")
            result[name] = float(found[0])
    return result


def leg_elements(left: bool, right: bool, parameters=None) -> list[dict]:
    p = java_parameters() if parameters is None else parameters
    top, spread, width, thickness = (p[k] for k in ("LEG_TOP", "LEG_SPREAD", "LEG_W", "LEG_T"))
    length = math.hypot(spread, top)
    angle = math.degrees(math.atan2(spread, top))
    single = not left and not right
    ends = ([1.5 if single else 4.5] if not left else []) + ([12.5 if single else 9.5] if not right else [])
    elements = []
    for x in ends:
        for z, sign in ((6.5, 1), (9.5, -1)):
            # mryt233 改（2026-11-10）：逐面沿用 box/quad 的左上角采样，不使用模型默认 UV。
            sizes = {"north": (width, length), "south": (width, length),
                     "west": (thickness, length), "east": (thickness, length),
                     "up": (width, thickness), "down": (width, thickness)}
            elements.append({"name": "斜腿", "from": [x, top - length, z - thickness / 2],
                             "to": [x + width, top, z + thickness / 2],
                             "rotation": {"origin": [x, top, z], "axis": "x", "angle": sign * angle},
                             "faces": {face: {"texture": "#planks", "uv": [0, 0, min(w, 16), min(h, 16)]}
                                       for face, (w, h) in sizes.items()}})
    return elements


def complete_model(base: dict, left: bool, right: bool, parameters=None) -> dict:
    model = {"credit": "作者：mryt233；mryt233 改（2026-11-10）；仅供预览，不可装进资源包",
             **copy.deepcopy({key: base[key] for key in
                              ("textures", "elements", "display", "ambientocclusion", "gui_light") if key in base})}
    model["elements"] += leg_elements(left, right, parameters)
    return model


def explanation(name: str) -> str:
    return ("斜腿由 BenchLegsRenderer 在游戏里绘制，模型文件里没有；完整造型见 "
            f"preview_models/assets/{MODID}/models/block/{name}.json。作者：mryt233")
