"""多加载器工程自检。

十项（mryt233 改（2026-11-10））：未引用模型/语言键警告、生成物哈希/合并关系、条凳预览与 Java 参数、显式 --jar 最终单包验收；原有六项源码检查与产物检查。
原有：分层 / 架构 / 内容（模型父链、贴图、语言、配方、指南）/ 清单 / 数值 / 贴图（尺寸、动画、半透明、居中、原版风格；mryt233 2026-11-01 加）/ 产物。
产物缺失或早于源码时标为「未验证」，不自动打包，也不声称十项全绿（--source-only 为九项；mryt233 改，2026-11-10）。
--strict：有失败或未验证返回 1。--source-only：显式只验源码，不做产物验收。

用法（mod 目录下）：
    python tools/check_all.py
    python tools/check_all.py --strict    # 有失败返回 1
作者：mryt233
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
COMMON = ROOT / "common/src"
RES = ROOT / "common/src/main/resources"
ASSETS = RES / "assets/orients_hotpot_chronicles"
MANIFEST = ROOT / "content/items.json"
NS = "orients_hotpot_chronicles"

LOADER_RE = re.compile(r"\b(net\.fabricmc\.|net\.neoforged\.|net\.minecraftforge\.)")


def check_layering() -> tuple[bool, list[str]]:
    """common 里不能 import 任何加载器。"""
    problems = []
    files = 0
    for path in COMMON.rglob("*.java"):
        files += 1
        for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
            code = line.split("//", 1)[0]
            if code.lstrip().startswith("*"):
                continue
            if LOADER_RE.search(code):
                problems.append(f"{path.name}:{lineno}: {line.strip()[:80]}")
    return (not problems), [f"扫了 {files} 个核心类"] + problems


def check_content() -> tuple[bool, list[str]]:
    """每样物品要有模型 + 10 种语言键。"""
    items = json.loads(MANIFEST.read_text(encoding="utf-8"))["items"]
    langs = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in (ASSETS / "lang").glob("*.json")}
    problems = []
    for item in items:
        item_id = item["id"]
        if not (ASSETS / "models/item" / f"{item_id}.json").is_file():
            problems.append(f"{item_id}: 缺 models/item/{item_id}.json")
        keys = (f"item.{NS}.{item_id}", f"block.{NS}.{item_id}")
        missing = [code for code, data in langs.items() if not any(k in data for k in keys)]
        if missing:
            problems.append(f"{item_id}: 语言缺 {', '.join(missing)}")
    return (not problems), [f"清单 {len(items)} 条，语言 {len(langs)} 种"] + problems


def inspect_jar(jar: Path, must: str, must_not: str) -> list[str]:
    """只检查给定文件；不执行 Gradle，不依赖系统 unzip。"""
    problems = []
    try:
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
            cores = sum("/hotpot/" in name and name.endswith(".class") for name in names)
            if cores < 50:
                problems.append(f"核心类只有 {cores} 个（应 >=50）")
            if must not in names:
                problems.append(f"缺 {must}")
            if must_not in names:
                problems.append(f"混进了对方的 {must_not}")
            if any("/devtest/" in name and name.endswith(".class") for name in names):
                problems.append("混进了开发测试类")
    except (OSError, zipfile.BadZipFile) as exc:
        problems.append(f"无法读取 jar：{exc}")
    return problems


def check_jars() -> tuple[bool | None, list[str]]:
    """缺失/陈旧是未验证，检查成功也只证明静态结构，不证明可运行。"""
    notes, problems = [], []
    unverified = False
    for name, must, must_not in (
        ("fabric", "fabric.mod.json", "META-INF/neoforge.mods.toml"),
        ("neoforge", "META-INF/neoforge.mods.toml", "fabric.mod.json"),
    ):
        jars = [p for p in (ROOT / name / "build/libs").glob("*.jar")
                if not any(part in p.stem for part in ("sources", "dev", "javadoc"))]
        if not jars:
            notes.append(f"{name}: 无 jar，未验证（不会自动打包）")
            unverified = True
            continue
        jar = max(jars, key=lambda p: p.stat().st_mtime_ns)
        inputs = [ROOT / p for p in ("build.gradle", "settings.gradle", "gradle.properties",
                                     "common/build.gradle", f"{name}/build.gradle")]
        for base in (ROOT / "common/src", ROOT / name / "src"):
            inputs.extend(p for p in base.rglob("*") if p.is_file())
        latest_input = max((p.stat().st_mtime_ns for p in inputs if p.is_file()), default=0)
        if jar.stat().st_mtime_ns < latest_input:
            notes.append(f"{name}: {jar.name} 早于当前源码/资源，未验证")
            unverified = True
            continue
        found = inspect_jar(jar, must, must_not)
        problems.extend(f"{name}: {message}" for message in found)
        notes.append(f"{name}: {jar.name} 静态结构检查（不代替游戏运行测试）")
    return (False if problems else None if unverified else True), notes + problems


def run_script(script: str, *args: str) -> tuple[bool, list[str]]:
    """跑同目录下的另一个脚本，返回（是否通过，输出行）。"""
    proc = subprocess.run([sys.executable, str(Path(__file__).resolve().parent / script), *args],
                          capture_output=True, text=True, encoding="utf-8", errors="replace",
                          env={**os.environ, "PYTHONIOENCODING": "utf-8"})
    out = (proc.stdout + proc.stderr).strip().splitlines()
    return proc.returncode == 0, out[:8]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strict", action="store_true")
    parser.add_argument("--source-only", action="store_true")
    parser.add_argument("--jar", action="append", nargs="+", type=Path, default=[], help="指定一个或多个最终单 JAR")
    args = parser.parse_args()
    jars = [path for group in args.jar for path in group]
    if args.source_only and jars:
        parser.error("--source-only 与 --jar 不能一起使用")
    strict = args.strict or bool(jars)
    source_only = args.source_only
    from check_unused import check_unused
    from check_generated import check_manifest
    from check_final_jar import check_final_jar
    from check_bench_preview import check_bench_preview
    failed, unverified = [], []
    checks = [
        ("分层", check_layering),
        ("架构", lambda: run_script("check_architecture.py", "--strict")),
        ("内容", lambda: run_script("check_content.py", "--strict")),
        ("清单", lambda: run_script("gen_content_manifest.py", "--check")),
        ("数值", lambda: run_script("gen_food_numbers.py", "--strict")),
        ("贴图", lambda: run_script("check_textures.py", "--strict")),
        ("未引用模型/语言键", check_unused),
        ("生成物哈希/合并关系", check_manifest),
        ("条凳预览/Java 参数", check_bench_preview),
    ]
    if jars:
        for jar in jars:
            checks.append(("最终单 JAR：" + jar.name, lambda jar=jar: check_final_jar(jar)))
    elif not source_only:
        checks.append(("中间产物（最终包请指定 --jar）", check_jars))
    for name, fn in checks:
        try:
            ok, lines = fn()
        except (OSError, ValueError) as exc:
            ok, lines = False, [str(exc)]
        status = "通过" if ok else "未验证" if ok is None else "失败"
        print(f"[{status}] {name}")
        for line in lines[:12]:
            print("   ", line)
        if ok is None:
            unverified.append(name)
        elif not ok:
            failed.append(name)
    print()
    if failed or unverified:
        if failed:
            print(f"失败：{'、'.join(failed)}")
        if unverified:
            print(f"未验证：{'、'.join(unverified)}")
        return 1 if strict else 0
    print("源码检查通过；未做产物验收。" if source_only else "全部静态检查通过；不代替游戏运行验收。")
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())