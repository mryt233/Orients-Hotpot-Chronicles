"""未引用模型和语言键仅告警；动态名字和查看器食物模型显式豁免。作者：mryt233"""
import json
import re
from pixkit.paths import MOD, MODID, RES, ASSETS
from check_final_jar import refs_in, registered_ids


def check_unused():
    allow = json.loads((MOD / "tools/content_usage_allowlist.json").read_text(encoding="utf-8"))
    text = []
    for directory, suffixes in ((MOD / "common/src", (".java",)), (MOD / "fabric/src", (".java",)),
                                (MOD / "neoforge/src", (".java",)), (MOD / "viewer", (".js", ".json")),
                                (RES / "data", (".json",)), (ASSETS / "guide", (".json",))):
        for file in directory.rglob("*"):
            if file.suffix in suffixes:
                text.append(file.read_text(encoding="utf-8"))
    for module in ("pure", "loader", "game", "client"):
        text.extend(file.read_text(encoding="utf-8") for file in (MOD / "framework" / module / "src").rglob("*.java"))
    code = "\n".join(text)
    code = re.sub(r'"\s*\+\s*[\w.]+\.(?:MOD_ID|MODID)\s*\+\s*"', MODID, code)
    code = re.sub(r'"\s*\+\s*"', '', code)
    dynamic = {prefix for prefix in re.findall(r'"([^"\n]+[._])"\s*\+', code)
               if MODID in prefix and len(prefix.split(MODID, 1)[1]) > 2}
    used = set()
    for file in list((ASSETS / "blockstates").rglob("*.json")) + list((ASSETS / "models").rglob("*.json")):
        used.update(refs_in(json.loads(file.read_text(encoding="utf-8"))))
    warnings = []
    for file in sorted((ASSETS / "models").rglob("*.json")):
        rel = file.relative_to(ASSETS / "models").with_suffix("").as_posix()
        ref = MODID + ":" + rel
        if ref not in used and rel not in allow["models"] and not any(rel.startswith(p) for p in allow["model_prefixes"]) and not re.search(r'["/:]' + re.escape(file.stem) + r'["/.]', code):
            warnings.append("未引用模型：" + rel)
    registry = registered_ids()
    language = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    for key in language:
        if key.startswith(("item." + MODID + ".", "block." + MODID + ".")) and key.split(".", 2)[2] in registry["item"] | registry["block"]:
            continue
        short = key.replace("text." + MODID + ".", "")
        if key in code or '"' + short + '"' in code or any(short.startswith(p) for p in allow["language_prefixes"]) or any(key.startswith(p) for p in dynamic):
            continue
        warnings.append("未引用语言键：" + key)
    return True, [f"警告 {len(warnings)} 条（不报错）；27 个食物模型由查看器的模型页消费，已豁免"] + warnings
