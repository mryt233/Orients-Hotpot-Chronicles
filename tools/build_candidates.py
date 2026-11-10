"""双端打包：统一生成内嵌框架 Runtime 的单 JAR（文件名带 MC 版本 1.21.1），按版本类型自动归档；不自动发布。作者：mryt233

用法：
    python tools/build_candidates.py                 # 用已编译产物打包（默认内嵌单 JAR）
    python tools/build_candidates.py --compile       # 先跑 Gradle 编译再打包
    python tools/build_candidates.py --release       # 兼容原参数，同样生成内嵌单 JAR
    python tools/build_candidates.py --out build/optimization2/trial  # 完全隔离的本地试验输出
    python tools/build_candidates.py list            # 列出构建缓存产物
中间拆分产物保留在 build/exp-pkg/candidate/（仅构建内部使用）。
输出位置与命名（统一带 1.21.1，单文件直接放进 mods 即可）：
  - 开发版（1.0.0-rN）：
      本体单 JAR -> build/libs/lastest/dev/1.21.1/<loader>/mod/rN-rM/orients_hotpot_chronicles-1.21.1-<loader>-1.0.0-rN.jar
      独立前置留档 -> build/libs/lastest/dev/1.21.1/<loader>/framework/rN-rM/orient_framework-<loader>-<runtime>.jar
  - 预览版 / 准备公开版（0.x.y-alpha / 0.x.y-beta 等）：同版本号集中放同一文件夹，不按加载器拆分目录
      本体单 JAR -> build/libs/lastest/per/1.21.1/<version>/orients_hotpot_chronicles-1.21.1-<loader>-<version>.jar
      mryt233 改（2026-11-09）：预览版只写上述目录。
Runtime 版本读取 ApiVersion.java；开发版、预览版、正式版均为内嵌前置的单 JAR，无需另装独立前置。
"""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

# mryt233 改（2026-11-10）：版本与 JDK 读取唯一配置源。
from project_env import jdk21, mod_version, runtime_version
from compact_json import compact_json

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/exp-pkg/candidate"
RELEASE_DIR = ROOT / "build/libs/releases"
DEV_DIR = ROOT / "build/libs/lastest/dev/1.21.1"
PER_DIR = ROOT / "build/libs/lastest/per/1.21.1"
MC_VERSION = "1.21.1"
# mryt233 改（2026-11-10）：试验输出完全隔离；JSON 只在写包时压缩。
TRIAL_DIR = None
COMPACT_JSON = True
JAVA_HOME = str(jdk21())

FRAMEWORK_OUTPUTS = [
    ROOT / "framework/pure/build/classes/java/main",
    ROOT / "framework/loader/build/classes/java/main",
    ROOT / "framework/game/build/classes/java/main",
    ROOT / "framework/client/build/classes/java/main",
]
NEOFORGE_OUTPUT = ROOT / "neoforge/build/classes/java/main"
NEOFORGE_RUNTIME_SRC = ROOT / "neoforge/src/runtime/java"
NEOFORGE_JAR = ROOT / "neoforge/build/moddev/artifacts/neoforge-21.1.265.jar"
JAVAC = jdk21() / "bin/javac.exe"
JAVA_HOME_PATH = jdk21()
# mryt233 改（2026-11-04）：只取 1.21.1 用的 FML 4.x。26.x 移植把 FML 11.x（Java 25 编译）也下进了 Gradle 缓存，
# 全塞进编译路径会让 javac 读到 11.x 的类而报「类文件版本 69.0，应为 65.0」。
FML_JARS = [str(p) for p in (Path.home() / ".gradle/caches/modules-2/files-2.1/net.neoforged.fancymodloader").rglob("*.jar")
            if "sources" not in p.name and "javadoc" not in p.name and p.parent.parent.name.startswith("4.")]
EVENTBUS_JARS = [str(p) for p in (Path.home() / ".gradle/caches/modules-2/files-2.1/net.neoforged/bus").rglob("*.jar")
                 if "sources" not in p.name]


# mryt233 改（2026-11-10）：清除五项无引用常量，保留实际筛选包名。
FRAMEWORK_PACKAGE = "cn/blockforge/orient/framework/"

# 版本号唯一来源：framework/loader 的 ApiVersion.java（mryt233 改，2026-11-01，前置检查 W4）。
# 前置用递增数字版本，不用 hotfix（用户 2026-11-01 定）；Fabric 与 NeoForge 对 hotfix 字符串排序相反，数字两端一致。
# 内容包要求 [本次 Runtime, 下一个次版本)：0.1.x 内向后兼容，破坏性变更进 0.2.0；
# 内容包用到了本次新增的接口，所以下限就是本次 Runtime。
_API_SOURCE = (ROOT / "framework/loader/src/main/java/cn/blockforge/orient/framework/loader/ApiVersion.java").read_text(encoding="utf-8")
RUNTIME_VERSION = runtime_version()
API_VERSION = re.search(r'CURRENT\s*=\s*"([^"]+)"', _API_SOURCE).group(1)
_MAJOR, _MINOR = RUNTIME_VERSION.split(".")[:2]
_NEXT_MINOR = f"{_MAJOR}.{int(_MINOR) + 1}.0"
RUNTIME_RANGE = f"[{RUNTIME_VERSION},{_NEXT_MINOR})"
FABRIC_RUNTIME_RANGE = f">={RUNTIME_VERSION} <{_NEXT_MINOR}"



def display_path(path: Path) -> str:
    """--out 可以指向工程外的试验目录。"""
    try:
        return path.relative_to(ROOT).as_posix()
    except ValueError:
        return str(path)


def read_classes(root: Path, include: str | None = None, exclude=()) -> dict[str, bytes]:
    if not root.is_dir():
        raise SystemExit(f"缺少编译产物：{root}（先运行 Gradle 编译）")
    entries: dict[str, bytes] = {}
    for file in root.rglob("*.class"):
        rel = file.relative_to(root).as_posix()
        if include is not None and not rel.startswith(include):
            continue
        if any(rel.startswith(prefix) for prefix in exclude):
            continue
        entries[rel] = file.read_bytes()
    return entries


def read_resources(root: Path) -> dict[str, bytes]:
    entries: dict[str, bytes] = {}
    if not root.is_dir():
        return entries
    for file in root.rglob("*"):
        if file.is_file():
            entries[file.relative_to(root).as_posix()] = file.read_bytes()
    return entries


def write_jar(path: Path, entries: dict[str, bytes]) -> None:
    # mryt233 改（2026-11-10）：全部验证成功后才打开目标，坏 JSON 不损坏已有包。
    prepared = {}
    for name, data in entries.items():
        if COMPACT_JSON and name.endswith((".json", ".mcmeta")):
            try:
                data = compact_json(data)
            except (ValueError, UnicodeError) as exc:
                raise ValueError(f"打包 JSON 解析失败：{name}：{exc}") from exc
        prepared[name] = data
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as jar:
        for name in sorted(prepared):
            jar.writestr(name, prepared[name])


def runtime_fabric_meta() -> bytes:
    meta = {
        "schemaVersion": 1,
        "id": "orient_framework",
        "version": RUNTIME_VERSION,
        "name": "Orient Framework Runtime (candidate)",
        "description": "Shared framework runtime for Orient content mods. Local candidate build.",
        "environment": "*",
        "entrypoints": {"main": ["cn.blockforge.orient.framework.platform.fabric.FabricFrameworkRuntime"]},
        "depends": {"fabricloader": ">=0.15.11", "minecraft": "1.21.1", "java": ">=21", "fabric-api": "*"},
    }
    return json.dumps(meta, indent=2, ensure_ascii=False).encode()


def runtime_neoforge_meta() -> bytes:
    return f'''modLoader="javafml"
loaderVersion="[4,)"
license="MIT"

[[mods]]
modId="orient_framework"
version="{RUNTIME_VERSION}"
displayName="Orient Framework Runtime (candidate)"
description="""
Shared framework runtime for Orient content mods. Local candidate build.
"""

[[dependencies.orient_framework]]
modId="neoforge"
type="required"
versionRange="[21.1.0,)"
ordering="NONE"
side="BOTH"

[[dependencies.orient_framework]]
modId="minecraft"
type="required"
versionRange="[1.21.1,1.22)"
ordering="NONE"
side="BOTH"
'''.encode()


def hotpot_fabric_meta(version: str) -> bytes:
    source = ROOT / "fabric/src/main/resources/fabric.mod.json"
    meta = json.loads(source.read_text(encoding="utf-8"))
    meta["version"] = version
    meta.setdefault("depends", {})["orient_framework"] = FABRIC_RUNTIME_RANGE
    return json.dumps(meta, indent=2, ensure_ascii=False).encode()


def hotpot_neoforge_meta(version: str, neoforge_version: str) -> bytes:
    source = ROOT / "neoforge/src/main/resources/META-INF/neoforge.mods.toml"
    text = source.read_text(encoding="utf-8")
    text = text.replace("${version}", version).replace("${neoforge_version}", neoforge_version)
    block = f'''
# 框架 Runtime 前置（B 方案候选）；只接受已实测兼容的版本范围
[[dependencies.orients_hotpot_chronicles]]
modId="orient_framework"
type="required"
versionRange="{RUNTIME_RANGE}"
ordering="AFTER"
side="BOTH"
'''
    text = text.rstrip() + "\n" + block
    return text.encode()


def neoforge_version() -> str:
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    match = re.search(r"^neoforge_version\s*=\s*(\S+)", text, re.MULTILINE)
    return match.group(1) if match else "21.1.265"


def compile_neoforge_runtime() -> dict[str, bytes]:
    """单独编译 NeoForge Runtime 入口（不在开发环境 mod 扫描范围内）。"""
    out = OUT / "work/neoforge-runtime-classes"
    if out.exists():
        import shutil as _shutil
        _shutil.rmtree(out)
    out.mkdir(parents=True)
    classpath = ";".join([str(NEOFORGE_OUTPUT)]
                         + [str(path) for path in FRAMEWORK_OUTPUTS]
                         + [str(NEOFORGE_JAR)] + FML_JARS + EVENTBUS_JARS)
    sources = [str(path) for path in NEOFORGE_RUNTIME_SRC.rglob("*.java")]
    proc = subprocess.run([str(JAVAC), "-encoding", "UTF-8", "--release", "21",
                           "-cp", classpath, "-d", str(out), *sources],
                          capture_output=True, text=True, encoding="utf-8", errors="replace",
                          env={**os.environ, "JAVA_HOME": str(JAVA_HOME_PATH)})
    if proc.returncode != 0:
        raise SystemExit("NeoForge Runtime 入口编译失败：" + chr(10) + (proc.stderr or proc.stdout))
    return read_classes(out)


def gradle_production_jars() -> None:
    """先出两端生产 jar：Fabric 走 remapJar（intermediary + mixin refmap），NeoForge 用官方名 jar。"""
    env = {**os.environ, "JAVA_HOME": JAVA_HOME}
    command = r".\gradlew.bat :fabric:remapJar :neoforge:jar --console=plain -q"
    proc = subprocess.run(command, shell=True, cwd=ROOT, env=env)
    if proc.returncode != 0:
        raise SystemExit("Gradle 生产 jar 构建失败")


def production_jar(folder: str, version: str) -> Path:
    candidates = [path for path in (ROOT / folder / "build/libs").glob(f"*{version}*.jar")
                  if not path.name.endswith("-sources.jar")]
    if len(candidates) != 1:
        raise SystemExit(f"{folder}/build/libs 里找到 {len(candidates)} 个 {version} jar，无法确定生产 jar")
    return candidates[0]


def read_jar(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as archive:
        return {name: archive.read(name) for name in archive.namelist() if not name.endswith("/")}


def build() -> dict[str, Path]:
    version = mod_version()
    results: dict[str, Path] = {}
    gradle_production_jars()

    # ---- Fabric：从 remapJar 产物拆两份（生产名 + refmap 由 Loom 处理）----
    fabric_entries = read_jar(production_jar("fabric", version))
    runtime_entries = {name: data for name, data in fabric_entries.items()
                       # mryt233 改（2026-11-10）：前置资源随内嵌 Runtime 分发。
                       if name.startswith((FRAMEWORK_PACKAGE, "assets/orient_framework/"))}
    runtime_entries["fabric.mod.json"] = runtime_fabric_meta()
    path = OUT / f"orient_framework-fabric-{RUNTIME_VERSION}.jar"
    write_jar(path, runtime_entries)
    results["runtime-fabric"] = path

    content_entries = {name: data for name, data in fabric_entries.items()
                       if not name.startswith(("cn/blockforge/orient/", "assets/orient_framework/")) and name != "fabric.mod.json"}
    meta = json.loads(fabric_entries["fabric.mod.json"].decode("utf-8"))
    meta.setdefault("depends", {})["orient_framework"] = FABRIC_RUNTIME_RANGE
    content_entries["fabric.mod.json"] = json.dumps(meta, indent=2, ensure_ascii=False).encode()
    path = OUT / f"orients_hotpot_chronicles-fabric-{version}.jar"
    write_jar(path, content_entries)
    results["hotpot-fabric"] = path

    # ---- NeoForge：官方名 jar 拆两份，Runtime 入口单独编译 ----
    neo_entries = read_jar(production_jar("neoforge", version))
    runtime_entries = {name: data for name, data in neo_entries.items()
                       # mryt233 改（2026-11-10）：前置资源随内嵌 Runtime 分发。
                       if name.startswith((FRAMEWORK_PACKAGE, "assets/orient_framework/"))}
    runtime_entries.update(compile_neoforge_runtime())
    runtime_entries["META-INF/neoforge.mods.toml"] = runtime_neoforge_meta()
    path = OUT / f"orient_framework-neoforge-{RUNTIME_VERSION}.jar"
    write_jar(path, runtime_entries)
    results["runtime-neoforge"] = path

    content_entries = {name: data for name, data in neo_entries.items()
                       if not name.startswith(("cn/blockforge/orient/", "assets/orient_framework/"))
                       and name != "META-INF/neoforge.mods.toml"}
    toml = neo_entries["META-INF/neoforge.mods.toml"].decode("utf-8").rstrip()
    content_entries["META-INF/neoforge.mods.toml"] = (toml + f"""

# 框架 Runtime 前置（B 方案候选）；只接受已实测兼容的版本范围
[[dependencies.orients_hotpot_chronicles]]
modId="orient_framework"
type="required"
versionRange="{RUNTIME_RANGE}"
ordering="AFTER"
side="BOTH"
""").encode()
    path = OUT / f"orients_hotpot_chronicles-neoforge-{version}.jar"
    write_jar(path, content_entries)
    results["hotpot-neoforge"] = path

    print(f"API {API_VERSION} · Runtime {RUNTIME_VERSION} · 内容 {version}（候选，不发布）")
    for key, path in results.items():
        print(f"  {key:16} {display_path(path)}  {path.stat().st_size // 1024} KB")
    return results


def compile_sources() -> None:
    env = {**os.environ, "JAVA_HOME": JAVA_HOME}
    command = (r".\gradlew.bat :framework:pure:compileJava :framework:loader:compileJava "
               ":framework:game:compileJava :framework:client:compileJava :common:compileJava "
               ":fabric:compileJava :neoforge:compileJava --console=plain -q")
    proc = subprocess.run(command, shell=True, cwd=ROOT, env=env)
    if proc.returncode != 0:
        raise SystemExit("Gradle 编译失败")



def build_release() -> dict[str, Path]:
    """mryt233 改（2026-11-09）：统一内嵌 Runtime 单 JAR + 统一带 MC 版本号（1.21.1）命名；开发版只写 dev/，预览版只写 per/。"""
    build()
    version = mod_version()
    dev_match = re.match(r"^\d+\.\d+\.\d+-r(\d+)", version)
    if dev_match:
        first = int(dev_match.group(1)) // 50 * 50
        bucket = f"r{first}-r{first + 49}"
    results: dict[str, Path] = {}
    for loader in ("fabric", "neoforge"):
        content = OUT / f"orients_hotpot_chronicles-{loader}-{version}.jar"
        runtime = OUT / f"orient_framework-{loader}-{RUNTIME_VERSION}.jar"
        if not content.is_file() or not runtime.is_file():
            raise SystemExit(f"缺少候选：{content.name} / {runtime.name}")
        entries = read_jar(content)
        nested = f"META-INF/jars/{runtime.name}"
        entries[nested] = runtime.read_bytes()
        if loader == "fabric":
            meta = json.loads(entries["fabric.mod.json"].decode("utf-8"))
            meta["jars"] = [{"file": nested}]
            entries["fabric.mod.json"] = json.dumps(meta, indent=2, ensure_ascii=False).encode()
        else:
            # mryt233 改：两端统一内嵌目录；NeoForge 仍由 JarJar 元数据里的 path 定位依赖。
            metadata = {"jars": [{
                "identifier": {"group": "cn.blockforge.orient", "artifact": "orient_framework"},
                "version": {"range": RUNTIME_RANGE, "artifactVersion": RUNTIME_VERSION},
                "path": nested,
            }]}
            entries["META-INF/jarjar/metadata.json"] = json.dumps(metadata, indent=2).encode()
        jar_name = f"orients_hotpot_chronicles-{MC_VERSION}-{loader}-{version}.jar"
        if TRIAL_DIR is not None:
            archived = TRIAL_DIR / jar_name
            write_jar(archived, entries)
            results[f"trial-{loader}"] = archived
        elif dev_match:
            mod_dir = DEV_DIR / loader / "mod" / bucket
            framework_dir = DEV_DIR / loader / "framework" / bucket
            mod_dir.mkdir(parents=True, exist_ok=True)
            framework_dir.mkdir(parents=True, exist_ok=True)
            archived = mod_dir / jar_name
            write_jar(archived, entries)
            results[f"dev-{loader}"] = archived
            archived_runtime = framework_dir / runtime.name
            shutil.copy2(runtime, archived_runtime)
            results[f"framework-{loader}"] = archived_runtime
        else:
            per_out = PER_DIR / version / jar_name
            write_jar(per_out, entries)
            results[f"per-{loader}"] = per_out
    print(f"完整单 jar（内嵌 Runtime {RUNTIME_VERSION}；统一命名含 {MC_VERSION}）：")
    for key, path in results.items():
        print(f"  {key:16} {display_path(path)}  {path.stat().st_size // 1024} KB")
    return results


def main() -> int:
    global OUT, TRIAL_DIR, COMPACT_JSON
    args = sys.argv[1:]
    if "--out" in args:
        index = args.index("--out")
        if index + 1 >= len(args):
            raise SystemExit("--out 后缺少目录")
        TRIAL_DIR = Path(args[index + 1]).resolve()
        OUT = TRIAL_DIR / "candidate"
    if "--no-json-compact" in args:
        if TRIAL_DIR is None:
            raise SystemExit("--no-json-compact 仅供 --out 隔离试验使用")
        COMPACT_JSON = False
    if "list" in args:
        for path in sorted(OUT.glob("*.jar")):
            print(f"{path.name}  {path.stat().st_size // 1024} KB")
        return 0
    if "--compile" in args:
        compile_sources()
    # mryt233 改：--release 保留兼容，默认调用也执行一次拆分与内嵌打包。
    build_release()
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
