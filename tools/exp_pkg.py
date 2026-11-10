"""EXP-PKG：构建打包实验用的框架与空壳 jar（不发布）。作者：mryt233

用法：
    python tools/exp_pkg.py build          # 生成 build/exp-pkg/*.jar
    python tools/exp_pkg.py list           # 列出可用 jar
实验 jar 只用于本地加载/依赖验证，不是发布产物。
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

# mryt233 改（2026-11-10）：版本与 JDK 读取唯一配置源。
from project_env import jdk21

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/exp-pkg"
JAVA = jdk21() / "bin/javac.exe"
JAVA_HOME = jdk21()
FRAMEWORK_CLASSES = [
    ROOT / "framework/pure/build/classes/java/main",
    ROOT / "framework/loader/build/classes/java/main",
    ROOT / "framework/game/build/classes/java/main",
]
FABRIC_LOADER = Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc/fabric-loader/0.19.3/354dfaa02d0552e11867f85dff7cdbfaf813ba3e/fabric-loader-0.19.3.jar"
NEOFORGE = ROOT / "neoforge/build/moddev/artifacts/neoforge-21.1.265.jar"
MC_CLIENT = Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-client.jar"
RUNTIME_SRC = ROOT / "experiments/runtime-src"
FML_JARS = [str(p) for p in (Path.home() / ".gradle/caches/modules-2/files-2.1/net.neoforged.fancymodloader").rglob("*.jar")
            if "sources" not in p.name and "javadoc" not in p.name]


def zip_jar(path: Path, entries: dict[str, bytes]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        for name, data in entries.items():
            z.writestr(name, data)


def class_entries() -> dict[str, bytes]:
    entries: dict[str, bytes] = {}
    for root in FRAMEWORK_CLASSES:
        if not root.is_dir():
            raise SystemExit(f"缺少编译产物：{root}（先运行 Gradle 编译）")
        for file in root.rglob("*.class"):
            entries[file.relative_to(root).as_posix()] = file.read_bytes()
    return entries


def fabric_meta(version: str, entrypoint: bool = False) -> bytes:
    meta = {
        "schemaVersion": 1,
        "id": "orient_framework",
        "version": version,
        "name": "Orient Framework (experiment)",
        "environment": "*",
        "depends": {"fabricloader": ">=0.15", "minecraft": "1.21.1", "java": ">=21"},
    }
    if entrypoint:
        meta["entrypoints"] = {"main": ["exp.runtime.ExpFabricInit"]}
    return json.dumps(meta, indent=2).encode()


def neoforge_meta(version: str) -> bytes:
    return f'''modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
[[mods]]
modId="orient_framework"
version="{version}"
displayName="Orient Framework (experiment)"
'''.encode()


def build_framework() -> dict[str, Path]:
    classes = class_entries()
    tmp = OUT / "tmp-framework-0.1.jar"
    tmpdata = dict(classes)
    tmpdata["fabric.mod.json"] = fabric_meta("0.1.0")
    zip_jar(tmp, tmpdata)
    runtime = compile_runtime(OUT / "work", tmp)
    tmp.unlink()
    result = {}
    for version in ("0.1.0", "0.2.0"):
        fabric = OUT / f"framework-fabric-{version}.jar"
        data = dict(classes)
        for file in runtime.rglob("*.class"):
            rel = file.relative_to(runtime).as_posix()
            if rel == "exp/runtime/ApiV2.class" and version != "0.2.0":
                continue
            data[rel] = file.read_bytes()
        data["fabric.mod.json"] = fabric_meta(version, entrypoint=True)
        zip_jar(fabric, data)
        neo = OUT / f"framework-neoforge-{version}.jar"
        data = dict(classes)
        for file in runtime.rglob("*.class"):
            rel = file.relative_to(runtime).as_posix()
            if rel == "exp/runtime/ApiV2.class" and version != "0.2.0":
                continue
            data[rel] = file.read_bytes()
        data["META-INF/neoforge.mods.toml"] = neoforge_meta(version)
        zip_jar(neo, data)
        result[f"framework-fabric-{version}"] = fabric
        result[f"framework-neoforge-{version}"] = neo
    return result


def compile_runtime(work: Path, framework_0_1: Path) -> Path:
    out = work / "runtime-classes"
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    cp = [str(framework_0_1), str(FABRIC_LOADER), str(NEOFORGE), str(MC_CLIENT)] + FML_JARS
    sources = [str(p) for p in RUNTIME_SRC.rglob("*.java")]
    proc = subprocess.run([str(JAVA), "-encoding", "UTF-8", "--release", "21", "-cp", ";".join(cp),
                           "-d", str(out), *sources],
                          capture_output=True, text=True, encoding="utf-8", errors="replace",
                          env={"JAVA_HOME": str(JAVA_HOME)})
    if proc.returncode != 0:
        raise SystemExit("runtime javac 失败:\n" + proc.stdout + "\n" + proc.stderr)
    return out


FABRIC_SHELL = '''package {pkg};

import cn.blockforge.orient.framework.loader.ApiVersion;
import cn.blockforge.orient.framework.loader.Framework;
import net.fabricmc.api.ModInitializer;

public final class {cls} implements ModInitializer {{
    private static final String NAME = "{name}";

    @Override
    public void onInitialize() {{
        System.out.println("[EXP-PKG] shell=" + NAME + " loader=fabric"
            + " api=" + ApiVersion.CURRENT
            + " installed=" + Framework.isInstalled()
            + " framework=" + Framework.class.getProtectionDomain().getCodeSource().getLocation()
            + " loot=" + Integer.toHexString(System.identityHashCode(Framework.lootEvents()))
            + {extra});
    }}
}}
'''

NEOSHELL = '''package {pkg};

import cn.blockforge.orient.framework.loader.ApiVersion;
import cn.blockforge.orient.framework.loader.Framework;
import net.neoforged.fml.common.Mod;

@Mod("{modid}")
public final class {cls} {{
    private static final String NAME = "{name}";

    public {cls}() {{
        System.out.println("[EXP-PKG] shell=" + NAME + " loader=neoforge"
            + " api=" + ApiVersion.CURRENT
            + " installed=" + Framework.isInstalled()
            + " framework=" + Framework.class.getProtectionDomain().getCodeSource().getLocation()
            + " loot=" + Integer.toHexString(System.identityHashCode(Framework.lootEvents()))
            + {extra});
    }}
}}
'''


def extra_for(name: str) -> str:
    if name == "beta-v2":
        return '" tag=" + exp.runtime.ApiV2.tag()'
    return '""'


def build_shells(framework: dict[str, Path]) -> dict[str, Path]:
    work = OUT / "work"
    src = work / "src/exp/pkg"
    classes_dir = work / "classes"
    for folder in (src, classes_dir):
        if folder.exists():
            shutil.rmtree(folder)
        folder.mkdir(parents=True)
    shells = [("alpha", "Alpha"), ("alpha-compat", "AlphaCompat"), ("beta", "Beta"),
              ("beta-v2", "BetaV2"), ("beta-conflict", "BetaConflict")]
    def pkg_of(name: str) -> str:
        return "exp." + name.replace("-", "")

    for name, cls in shells:
        pkg = pkg_of(name)
        folder = src / pkg.replace(".", "/")
        folder.mkdir(parents=True, exist_ok=True)
        (folder / f"{cls}Fabric.java").write_text(
            FABRIC_SHELL.format(pkg=pkg, cls=f"{cls}Fabric", name=name, extra=extra_for(name)),
            encoding="utf-8")
        (folder / f"{cls}Neo.java").write_text(
            NEOSHELL.format(pkg=pkg, modid=f"exp_{name.replace('-', '_')}", cls=f"{cls}Neo", name=name,
                            extra=extra_for(name)),
            encoding="utf-8")
    common = [str(Path(framework['framework-fabric-0.1.0'])), str(Path(framework['framework-neoforge-0.1.0'])),
              str(FABRIC_LOADER), str(NEOFORGE), str(MC_CLIENT)]
    for name, cls in shells:
        for loader, cp, out in [
            ("fabric", [str(framework['framework-fabric-' + ("0.2.0" if name == "beta-v2" else "0.1.0")]), str(FABRIC_LOADER), str(MC_CLIENT)],
             classes_dir / "fabric"),
            ("neoforge", [str(framework['framework-neoforge-' + ("0.2.0" if name == "beta-v2" else "0.1.0")]), str(NEOFORGE), str(MC_CLIENT)] + FML_JARS,
             classes_dir / "neoforge"),
        ]:
            out.mkdir(parents=True, exist_ok=True)
            pkg = pkg_of(name)
            java_file = src / pkg.replace(".", "/") / f"{cls}{'Fabric' if loader == 'fabric' else 'Neo'}.java"
            cmd = [str(JAVA), "-encoding", "UTF-8", "--release", "21", "-cp", ";".join(cp),
                   "-d", str(out), str(java_file)]
            proc = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace",
                                  env={"JAVA_HOME": str(JAVA_HOME)})
            if proc.returncode != 0:
                raise SystemExit(f"javac 失败 {java_file}:\n{proc.stdout}\n{proc.stderr}")
    result = {}
    range_ok = "[0.1.0,0.2.0)"
    range_bad = "[9.0.0,10.0.0)"
    for name, cls in shells:
        depends = (range_bad if name == "beta-conflict"
                   else "[0.2.0,0.3.0)" if name == "beta-v2"
                   else "[0.1.0,0.3.0)" if name == "alpha-compat"
                   else range_ok)
        # Fabric A（内嵌）/ B（前置）
        for variant, embedded in (("A", True), ("B", False)):
            entries = {}
            base = classes_dir / "fabric"
            for file in base.rglob("*.class"):
                if file.name == f"{cls}Fabric.class" or file.name.startswith(f"{cls}Fabric$"):
                    entries[file.relative_to(base).as_posix()] = file.read_bytes()
            meta = {
                "schemaVersion": 1, "id": f"exp_{name.replace('-', '_')}", "version": "0.1.0",
                "name": f"EXP {name}", "environment": "*",
                "entrypoints": {"main": [f"{pkg_of(name)}.{cls}Fabric"]},
                "depends": {"fabricloader": ">=0.15", "minecraft": "1.21.1", "java": ">=21"},
            }
            embed_version = "0.2.0" if name == "beta-v2" else "0.1.0"
            if embedded:
                entries["META-INF/jars/orient_framework.jar"] = Path(framework[f'framework-fabric-{embed_version}']).read_bytes()
                meta["jars"] = [{"file": "META-INF/jars/orient_framework.jar"}]
            if name == "beta-conflict":
                meta["depends"]["orient_framework"] = ">=9.0.0 <10.0.0"
            elif name == "beta-v2":
                meta["depends"]["orient_framework"] = ">=0.2.0 <0.3.0"
            elif name == "alpha-compat":
                meta["depends"]["orient_framework"] = ">=0.1.0 <0.3.0"
            else:
                meta["depends"]["orient_framework"] = ">=0.1.0 <0.2.0"
            entries["fabric.mod.json"] = json.dumps(meta, indent=2).encode()
            jar = OUT / f"{name}-fabric-{variant}.jar"
            zip_jar(jar, entries)
            result[f"{name}-fabric-{variant}"] = jar
        # NeoForge A（内嵌）/ B（前置）
        for variant, embedded in (("A", True), ("B", False)):
            entries = {}
            base = classes_dir / "neoforge"
            for file in base.rglob("*.class"):
                if file.name == f"{cls}Neo.class" or file.name.startswith(f"{cls}Neo$"):
                    entries[file.relative_to(base).as_posix()] = file.read_bytes()
            dep = f'''[[dependencies.exp_{name.replace('-', '_')}]]
modId="orient_framework"
type="required"
versionRange="{depends}"
ordering="NONE"
side="BOTH"
'''
            meta = f'''modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
[[mods]]
modId="exp_{name.replace('-', '_')}"
version="0.1.0"
displayName="EXP {name}"
{dep}'''
            entries["META-INF/neoforge.mods.toml"] = meta.encode()
            embed_version = "0.2.0" if name == "beta-v2" else "0.1.0"
            if embedded:
                entries["META-INF/jarjar/orient_framework.jar"] = Path(framework[f'framework-neoforge-{embed_version}']).read_bytes()
                jm = {"jars": [{
                    "identifier": {"group": f"exp.{name.replace('-', '')}", "artifact": "orient_framework"},
                    "version": {"range": "[0.2.0,0.3.0)" if embed_version == '0.2.0' else "[0.1.0,0.2.0)", "artifactVersion": embed_version},
                    "path": "META-INF/jarjar/orient_framework.jar",
                }]}
                entries["META-INF/jarjar/metadata.json"] = json.dumps(jm, indent=2).encode()
            jar = OUT / f"{name}-neoforge-{variant}.jar"
            zip_jar(jar, entries)
            result[f"{name}-neoforge-{variant}"] = jar
    return result


def main() -> int:
    if len(sys.argv) < 2 or sys.argv[1] not in ("build", "list"):
        print(__doc__)
        return 1
    if sys.argv[1] == "list":
        for jar in sorted(OUT.glob("*.jar")):
            print(jar.relative_to(ROOT))
        return 0
    framework = build_framework()
    shells = build_shells(framework)
    print(f"框架 jar：{len(framework)} 个；空壳 jar：{len(shells)} 个；输出：{OUT.relative_to(ROOT)}")
    for jar in sorted(OUT.glob("*.jar")):
        print(" ", jar.name)
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
