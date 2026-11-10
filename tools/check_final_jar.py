"""验收显式指定的最终单 JAR、内嵌前置和资源引用链。作者：mryt233"""
from __future__ import annotations
import io
import json
import re
import tomllib
import zipfile
from pathlib import Path
from model_bake import ModelResolver, qualify
from pixkit.materials import ids
from pixkit.paths import MOD, MODID, RES
from project_env import mod_version, runtime_version


def read_jar(value):
    with zipfile.ZipFile(value) as jar:
        if len(jar.namelist()) != len(set(jar.namelist())):
            raise ValueError("ZIP 含重复路径")
        return {name: jar.read(name) for name in jar.namelist() if not name.endswith("/")}


def registered_ids():
    import gen_content_manifest as content
    source = content.CONTENT_JAVA.read_text(encoding="utf-8")
    items = set(content.registered_items(source))
    blocks = set(re.findall(r'\.blocks\(\)\.register\(\s*HotPot\.id\("([a-z0-9_]+)"\)', source))
    materials = ids("woods") + ids("stone_tables")
    tables = {m + "_" + kind for m in materials for kind in ("hotpot_table", "tall_hotpot_table")}
    benches = {m + "_" + kind for m in ids("woods") for kind in ("stool", "bench")}
    stations = {m + "_sauce_station" for m in ids("woods") + ["iron"]}
    plates = {m + "_" + kind for m in ids("plates") for kind in (
        "small_dish", "two_slot_plate", "three_slot_long_plate", "four_slot_square_plate", "five_slot_long_plate", "eight_slot_platter")}
    # 枚举的锅 ID 与各直接注册 ID，名单来源均为源码，不以模型文件反推注册表。
    for name in ("HotPotMaterial", "DrinkKind"):
        code = (content.CONTENT_JAVA.parent / (name + ".java")).read_text(encoding="utf-8")
        blocks.update(re.findall(r'^\s*[A-Z_]+\(\s*"([a-z0-9_]+)"', code, re.M))
    blocks |= tables | benches | stations | plates
    items |= tables | benches | stations | plates | {x for x in blocks if x not in ("plates", "bowl", "drink", "cup", "drink_cauldron", "tall_brick_stove_top", "sauce_station_top", "chopstick_rest")}
    return {"item": items, "block": blocks}


def refs_in(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in ("model", "parent") and isinstance(child, str):
                yield qualify(child)
            else:
                yield from refs_in(child)
    elif isinstance(value, list):
        for child in value:
            yield from refs_in(child)


def resource_problems(entries, registry=None):
    problems = []
    decoded = {name: json.loads(data) for name, data in entries.items() if name.endswith((".json", ".mcmeta"))}
    resolver = ModelResolver(RES)
    def load(ref):
        ns, name = qualify(ref).split(":", 1)
        if name.startswith("builtin/"):
            return {}
        if ns == "minecraft":
            return ModelResolver.load(resolver, ref)
        key = f"assets/{ns}/models/{name}.json"
        if key not in decoded:
            raise ValueError("缺模型：" + ref)
        return decoded[key]
    resolver.load = load
    root_models = set()
    for name, obj in decoded.items():
        if name.startswith(f"assets/{MODID}/") and ("/models/" in name or "/blockstates/" in name):
            for ref in refs_in(obj):
                try:
                    load(ref)
                except (OSError, ValueError) as exc:
                    problems.append(f"{name}：{exc}")
                if "/blockstates/" in name:
                    root_models.add(ref)
            if "/models/" in name and "/template/" not in name:
                root_models.add(MODID + ":" + name.split("/models/", 1)[1][:-5])
    for ref in sorted(root_models):
        try:
            model = resolver.bake(ref)
            textures = {f["texture"] for e in model["elements"] for f in e.get("faces", {}).values()}
            textures.update(model.get("layers", {}).values())
            if model["particle"]:
                textures.add(model["particle"])
            for tex in textures:
                ns, name = tex.split(":", 1)
                if ns == MODID and f"assets/{ns}/textures/{name}.png" not in entries:
                    problems.append(f"{ref}：缺贴图 {tex}")
                elif ns == "minecraft" and not (resolver.vanilla / "textures" / (name + ".png")).is_file():
                    problems.append(f"{ref}：原版缓存中缺贴图 {tex}")
        except (OSError, ValueError) as exc:
            problems.append(f"{ref}：{exc}")
    registry = registry if registry is not None else registered_ids()
    for name, obj in decoded.items():
        match = re.match(r"data/[^/]+/tags/(item|block)/(.+)\.json$", name)
        if match:
            for value in obj.get("values", []):
                if isinstance(value, dict) and not value.get("required", True):
                    continue
                ref = value["id"] if isinstance(value, dict) else value
                if ref.startswith("#" + MODID + ":"):
                    target = f"data/{MODID}/tags/{match[1]}/{ref.split(':', 1)[1]}.json"
                    if target not in entries:
                        problems.append(f"{name}：缺必选标签 {ref}")
                elif ref.startswith(MODID + ":") and ref.split(":", 1)[1] not in registry[match[1]]:
                    problems.append(f"{name}：必选 {match[1]} 未注册 {ref}")
        if name.startswith(f"data/{MODID}/recipe/"):
            result = obj.get("result")
            rid = result.get("id") if isinstance(result, dict) else result
            if isinstance(rid, str) and rid.startswith(MODID + ":") and rid.split(":", 1)[1] not in registry["item"]:
                problems.append(f"{name}：配方产物未注册 {rid}")
    return problems


def check_final_jar(path: Path):
    problems, notes = [], []
    expected = runtime_version()
    try:
        entries = read_jar(path)
        fabric = "fabric.mod.json" in entries
        if fabric == ("META-INF/neoforge.mods.toml" in entries):
            raise ValueError("必须且只能含一个加载器元数据")
        if fabric:
            meta = json.loads(entries["fabric.mod.json"])
            nested = [m["file"] for m in meta.get("jars", [])]
            if meta.get("id") != MODID or meta.get("version") != mod_version():
                problems.append("本体 ID / 版本与当前项目不一致")
            if not str(meta.get("depends", {}).get("orient_framework", "")).startswith(">=" + expected + " "):
                problems.append("本体前置版本下限与 ApiVersion.RUNTIME 不一致")
        else:
            meta = tomllib.loads(entries["META-INF/neoforge.mods.toml"].decode())
            if not any(m.get("modId") == MODID and m.get("version") == mod_version() for m in meta.get("mods", [])):
                problems.append("本体 ID / 版本与当前项目不一致")
            jarjar = json.loads(entries["META-INF/jarjar/metadata.json"])["jars"]
            nested = [m["path"] for m in jarjar]
            for item in jarjar:
                if item["identifier"] != {"group": "cn.blockforge.orient", "artifact": "orient_framework"} or item["version"]["artifactVersion"] != expected:
                    problems.append("JarJar 前置坐标 / 版本不一致")
            deps = meta.get("dependencies", {}).get(MODID, [])
            if not any(d.get("modId") == "orient_framework" and d.get("versionRange", "").startswith("[" + expected + ",") for d in deps):
                problems.append("本体前置版本下限与 ApiVersion.RUNTIME 不一致")
        actual_nested = [name for name in entries if name.endswith(".jar")]
        if len(nested) != 1 or set(nested) != set(actual_nested):
            raise ValueError("必须只内嵌一份前置，且元数据与实际嵌套路径一致")
        runtime = read_jar(io.BytesIO(entries[nested[0]]))
        if fabric:
            runtime_meta = json.loads(runtime["fabric.mod.json"])
            good = runtime_meta.get("id") == "orient_framework" and runtime_meta.get("version") == expected
        else:
            runtime_meta = tomllib.loads(runtime["META-INF/neoforge.mods.toml"].decode())
            good = any(m.get("modId") == "orient_framework" and m.get("version") == expected for m in runtime_meta.get("mods", []))
        if not good:
            problems.append("内嵌前置 ID / 版本与 ApiVersion.RUNTIME 不一致")
        for label, files in (("本体", entries), ("前置", runtime)):
            for name in files:
                if "/devtest/" in name or name.startswith(("mezz/jei/", "me/shedaniel/", "snownee/jade/", "com/terraformersmc/")):
                    problems.append(label + "混入开发测试或签名桩：" + name)
                # mryt233 改（2026-11-10）：独立预览 JSON 也属于禁止入包的预览素材。
                if "preview_models/" in name.lower() or name.lower().endswith((".java", ".md", ".psd", ".bbmodel")) or name.lower().endswith(".png") and ("preview" in name.lower() or "/textures/" not in name and not name.endswith("/icon.png")):
                    problems.append(label + "混入源码、文档或预览素材：" + name)
        problems.extend(resource_problems(entries))
        notes.append(f"{path.name}：{len(entries)} 个外包条目，{len(runtime)} 个前置条目，Runtime {expected}")
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as exc:
        problems.append(str(exc))
    return not problems, notes + problems
