"""核对全部条凳预览、游戏常量、静态几何和预览物品显示。作者：mryt233"""
import json
from pathlib import Path

from bench_preview import VARIANTS, complete_model, explanation, java_parameters
from model_bake import ModelResolver
from pixkit.materials import ids
from pixkit.paths import MOD, MODID


def check_bench_preview(root: Path = MOD):
    resources = root / "common/src/main/resources"
    overlay = root / "preview_models"
    resolver = ModelResolver(resources)
    parameters = java_parameters(root)
    problems = []
    count = 0
    for wood in ids("woods"):
        for (left, right), tail in VARIANTS.items():
            name = f"{wood}_bench{tail}"
            rel = f"assets/{MODID}/models/block/{name}.json"
            try:
                source = json.loads((resources / rel).read_text(encoding="utf-8"))
                if source.get("_说明") != explanation(name):
                    problems.append(f"{name}：缺完整预览说明")
                actual = json.loads((overlay / rel).read_text(encoding="utf-8"))
                expected = complete_model(resolver.merged(f"{MODID}:block/{name}"), left, right, parameters)
                if actual != expected:
                    problems.append(f"{name}：预览几何、UV 或显示与 Java 参数 / 原模型不一致")
                count += 1
            except (OSError, ValueError) as exc:
                problems.append(f"{name}：{exc}")
        base = resolver.merged(f"{MODID}:block/{wood}_bench")
        base["display"] = resolver.merged(f"{MODID}:item/{wood}_bench")["display"]
        try:
            item = overlay / f"assets/{MODID}/models/item/{wood}_bench.json"
            if json.loads(item.read_text(encoding="utf-8")) != complete_model(base, False, False, parameters):
                problems.append(f"{wood}_bench：物品预览不一致")
            count += 1
        except (OSError, ValueError) as exc:
            problems.append(str(exc))
    return not problems, [f"{count} 个预览；Java 参数 {parameters}；游戏模型未增加斜腿"] + problems


if __name__ == "__main__":
    ok, lines = check_bench_preview()
    print("\n".join(lines))
    raise SystemExit(not ok)
