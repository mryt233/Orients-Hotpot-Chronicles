"""P3 测试矩阵：一条命令跑 EXP-PKG 场景与 B 方案候选（Fabric/NeoForge）。作者：mryt233

用法：
    python tools/run_matrix.py --dry-run          # 只列出场景
    python tools/run_matrix.py fabric             # 只跑 Fabric
    python tools/run_matrix.py all                # 双端全部
结果：build/exp-pkg/results.json；日志：build/exp-pkg/logs/
注意：需要本地开发服端口 25570/25580（Fabric）、25571/25581（NeoForge）空闲。
候选场景额外断言 [ORIENT-FW] 摘要：init=1、每个模块 register=1、每个通道 registered=1。
"""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/exp-pkg"
LOGS = OUT / "logs"
JAVA_HOME = "C:/Users/10536/.jdks/jdk-21.0.12.1+1"

SERVERS = {
    "fabric": (ROOT / "experiments/server-fabric", ":experiments:server-fabric:runServer",
               ROOT / "experiments/server-fabric/run/server.properties"),
    "neoforge": (ROOT / "experiments/server-neoforge", ":experiments:server-neoforge:runServer",
                 ROOT / "experiments/server-neoforge/run/server.properties"),
}
# 场景：(名称, jar 路径模板列表, 期望, 说明, 是否断言 [ORIENT-FW])
SCENARIOS = [
    ("alpha-alone-A", ["alpha-{l}-A.jar"], "success", "A 单系列", False),
    ("beta-alone-A", ["beta-{l}-A.jar"], "success", "A 单系列", False),
    ("together-A", ["alpha-{l}-A.jar", "beta-{l}-A.jar"], "success", "A 两系列同装", False),
    ("different-versions-A", ["alpha-{l}-A.jar", "beta-v2-{l}-A.jar"], "reject", "A 互斥版本范围", False),
    ("B-single", ["framework-{l}-0.1.0.jar", "alpha-{l}-B.jar"], "success", "B 单系列 + 框架前置", False),
    ("B-pair", ["framework-{l}-0.1.0.jar", "alpha-{l}-B.jar", "beta-{l}-B.jar"], "success", "B 两系列同装", False),
    ("B-matched", ["framework-{l}-0.2.0.jar", "beta-v2-{l}-B.jar"], "success", "匹配版本基线", False),
    ("B-compatible", ["framework-{l}-0.2.0.jar", "alpha-compat-{l}-B.jar", "beta-v2-{l}-B.jar"],
     "success", "兼容版本组合（有交集范围）", False),
    ("missing-framework", ["beta-{l}-B.jar"], "reject", "缺框架前置", False),
    ("api-major-conflict", ["framework-{l}-0.1.0.jar", "beta-conflict-{l}-B.jar"], "reject",
     "API 主版本冲突", False),
    ("candidate-B", ["candidate/orient_framework-{l}-0.1.1.jar",
                     "candidate/orients_hotpot_chronicles-{l}-{v}.jar"], "success",
     "B 候选：框架 Runtime + 火锅内容", True),
    ("candidate-missing-runtime", ["candidate/orients_hotpot_chronicles-{l}-{v}.jar"], "reject",
     "B 候选缺框架 Runtime", False),
]
REJECT_MARKERS = ("Incompatible mods", "Mod loading has failed", "Missing or unsupported",
                  "ModResolutionException", "requires orient_framework", "缺少", "incompatible",
                  "failed to resolve")


def mod_version() -> str:
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    match = re.search(r"^mod_version\s*=\s*(\S+)", text, re.MULTILINE)
    if not match:
        raise SystemExit("gradle.properties 里找不到 mod_version")
    return match.group(1)


def rcon(loader: str, *commands: str):
    props = SERVERS[loader][2]
    return subprocess.run([sys.executable, str(ROOT / "tools/rcon.py"), "--properties", str(props),
                           *commands], capture_output=True, text=True, encoding="utf-8", errors="replace")


def kill_tree(proc: subprocess.Popen) -> None:
    """只杀本次启动的进程树（不能按进程名成片杀）。"""
    subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                   capture_output=True, text=True, encoding="utf-8", errors="replace")


def launch(loader: str):
    task = SERVERS[loader][1]
    env = {**os.environ, "JAVA_HOME": JAVA_HOME}
    log_path = LOGS / "_last_launch.log"
    handle = log_path.open("w", encoding="utf-8", errors="replace")
    # 用相对命令 + cwd，避开 “路径含空格 + cmd /c 引号” 的坑
    proc = subprocess.Popen(rf".\gradlew.bat -p . {task} --console=plain", shell=True,
                            stdout=handle, stderr=subprocess.STDOUT, env=env, cwd=ROOT)
    return proc, handle


def parse_runtime_stats(text: str) -> dict:
    """解析**最后一个** [ORIENT-FW] 摘要块；返回 init/modules/net 三类断言依据。
    不能用全部摘要行：较早摘要里的通道会补齐末尾缺失的通道（复核）。"""
    all_lines = text.splitlines()
    start = None
    for index, line in enumerate(all_lines):
        if "[ORIENT-FW] summary trigger=" in line:
            start = index
    lines = []
    if start is not None:
        for line in all_lines[start + 1:]:
            if "[ORIENT-FW] " not in line:
                break
            lines.append(line.split("[ORIENT-FW] ", 1)[1].strip())
    # 摘要头行必须存在；只有完全没有摘要时才退回入口日志行（：有摘要但缺头行必须失败）
    init = [line for line in lines if line.startswith("process=")]
    if start is None:
        init = [line.split("[ORIENT-FW] ", 1)[1].strip()
                for line in all_lines if "[ORIENT-FW] init " in line]
    modules = {}
    for line in lines:
        if not line.startswith("module "):
            continue
        fields = dict(re.findall(r"(\w+)=([^\s]+)", line))
        modules[fields.get("id", "?")] = fields
    net = {}
    for line in lines:
        if not line.startswith("net "):
            continue
        fields = dict(re.findall(r"(\w+)=([^\s]+)", line))
        net[(fields.get("channel", "?"), fields.get("direction", "?"))] = fields
    duplicates = [line for line in text.splitlines() if "duplicate channel registration ignored" in line]
    return {"init": init, "modules": modules, "net": net, "duplicates": duplicates}


def stats_ok(stats: dict) -> tuple[bool, list[str]]:
    problems = []
    if len(stats["init"]) != 1:
        problems.append(f"init 行数 {len(stats['init'])} != 1")
    else:
        # 逐字段精确比较，避免 count=10 因子串匹配误过（指出）
        fields = dict(re.findall(r"(\w+)=([^\s]+)", stats["init"][0]))
        count = fields.get("init", fields.get("count"))
        if count != "1" or fields.get("installs") != "1":
            problems.append("init 计数不是 1：" + stats["init"][0])
    if not stats["modules"]:
        problems.append("没有 module 摘要")
    for module, fields in stats["modules"].items():
        if fields.get("register") != "1":
            problems.append(f"模块 {module} register={fields.get('register')}（应为 1）")
        if fields.get("runtime") != "1":
            problems.append(f"模块 {module} runtime={fields.get('runtime')}（应为 1）")
    if stats["duplicates"]:
        problems.append(f"重复通道登记 {len(stats['duplicates'])} 次")
    for (channel, direction), fields in stats["net"].items():
        registered = fields.get("registered")
        if registered != "1":
            problems.append(f"通道 {channel}/{direction} registered={registered}（应为 1）")
    for channel, direction in (("orients_hotpot_chronicles:config", "S2C"),
                               ("orients_hotpot_chronicles:chopsticks_aim", "C2S"),
                               ("orients_hotpot_chronicles:open_sauce_restock", "C2S")):
        if (channel, direction) not in stats["net"]:
            problems.append(f"摘要缺少通道 {channel}/{direction}")
    return (not problems, problems)


def run_scenario(loader: str, name: str, jars: list[str], expect: str, note: str,
                 assert_stats: bool) -> dict:
    folder, task, props = SERVERS[loader]
    version = mod_version()
    mods = folder / "run/mods"
    if mods.exists():
        shutil.rmtree(mods)
    mods.mkdir(parents=True)
    for pattern in jars:
        file = OUT / pattern.format(l=loader, v=version)
        if not file.is_file():
            return {"loader": loader, "scenario": name, "expect": expect, "note": note,
                    "status": "fail", "ready": False, "reason": f"缺少 {file.name}", "log": ""}
        shutil.copy(file, mods)
    log = LOGS / f"{loader}-{name}.log"
    LOGS.mkdir(parents=True, exist_ok=True)
    proc, handle = launch(loader)
    ready = False
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            break
        if rcon(loader, "list").returncode == 0:
            ready = True
            break
        time.sleep(2)
    if ready:
        rcon(loader, "stop")
    try:
        proc.wait(timeout=120)
    except subprocess.TimeoutExpired:
        kill_tree(proc)
        proc.wait(timeout=30)
    handle.close()
    shutil.copy(LOGS / "_last_launch.log", log)
    text = log.read_text(encoding="utf-8", errors="replace")
    stats = parse_runtime_stats(text)
    rejects = any(marker in text for marker in REJECT_MARKERS)
    problems: list[str] = []
    if expect == "success":
        shells = [line.split("[EXP-PKG] ", 1)[1] for line in text.splitlines() if "[EXP-PKG]" in line]
        init = [line for line in shells if "framework-init" in line]
        if assert_stats:
            ok, problems = stats_ok(stats)
            ok = ok and ready
        else:
            ok = ready and bool(shells) and len(init) == 1
            if not ok:
                problems.append(f"ready={ready} shells={len(shells)} exp-init={len(init)}")
    else:
        ok = (not ready) and rejects
        if not ok:
            problems.append(f"ready={ready} rejects={rejects}")
    return {
        "loader": loader, "scenario": name, "expect": expect, "note": note,
        "status": "pass" if ok else "fail", "ready": ready,
        "problems": problems,
        "init": stats["init"], "modules": stats["modules"],
        "net": {f"{k[0]}/{k[1]}": v for k, v in stats["net"].items()},
        "log": str(log.relative_to(ROOT)),
    }


def main() -> int:
    args = sys.argv[1:]
    dry = "--dry-run" in args
    which = next((a for a in args if not a.startswith("--")), "all")
    loaders = ["fabric", "neoforge"] if which == "all" else [which]
    if dry:
        for loader in loaders:
            for name, jars, expect, note, assert_stats in SCENARIOS:
                marker = " +stats" if assert_stats else ""
                print(f"{loader:9} {name:24} {expect:7} {note}{marker}")
        return 0
    OUT.mkdir(parents=True, exist_ok=True)
    results = []
    for loader in loaders:
        for name, jars, expect, note, assert_stats in SCENARIOS:
            print(f"[{loader}] {name} ...", flush=True)
            result = run_scenario(loader, name, jars, expect, note, assert_stats)
            results.append(result)
            print(f"    -> {result['status']}", flush=True)
    (OUT / "results.json").write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")
    failed = [r for r in results if r["status"] != "pass"]
    print(f"通过 {len(results) - len(failed)}/{len(results)}；结果 build/exp-pkg/results.json")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
