"""EXP-PKG 场景运行器（本地开发服 + run/mods，真实 jar 解析）。作者：mryt233

python tools/exp_pkg_run.py [fabric|neoforge|all]
输出：build/exp-pkg/results.json + logs/<loader>-<scenario>.log
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

# mryt233 改（2026-11-10）：版本与 JDK 读取唯一配置源。
from project_env import jdk21

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/exp-pkg"
LOGS = OUT / "logs"
JAVA_HOME = str(jdk21())

SERVERS = {
    "fabric": ROOT / "experiments/server-fabric",
    "neoforge": ROOT / "experiments/server-neoforge",
}
PROPS = {name: folder / "run/server.properties" for name, folder in SERVERS.items()}
JARS = {
    "fabric": {
        "alpha-A": "alpha-fabric-A.jar",
        "beta-A": "beta-fabric-A.jar",
        "alpha-B": "alpha-fabric-B.jar",
        "beta-B": "beta-fabric-B.jar",
        "beta-v2-A": "beta-v2-fabric-A.jar",
        "beta-v2-B": "beta-v2-fabric-B.jar",
        "beta-conflict-B": "beta-conflict-fabric-B.jar",
        "framework-0.1": "framework-fabric-0.1.0.jar",
        "framework-0.2": "framework-fabric-0.2.0.jar",
    },
    "neoforge": {
        "alpha-A": "alpha-neoforge-A.jar",
        "beta-A": "beta-neoforge-A.jar",
        "alpha-B": "alpha-neoforge-B.jar",
        "beta-B": "beta-neoforge-B.jar",
        "beta-v2-A": "beta-v2-neoforge-A.jar",
        "beta-v2-B": "beta-v2-neoforge-B.jar",
        "beta-conflict-B": "beta-conflict-neoforge-B.jar",
        "framework-0.1": "framework-neoforge-0.1.0.jar",
        "framework-0.2": "framework-neoforge-0.2.0.jar",
    },
}
SCENARIOS = [
    ("alpha-alone-A", ["alpha-A"], "success"),
    ("beta-alone-A", ["beta-A"], "success"),
    ("together-A", ["alpha-A", "beta-A"], "success"),
    ("different-versions-A", ["alpha-A", "beta-v2-A"], "conflict"),
    ("standalone-B", ["framework-0.1", "alpha-B", "beta-B"], "success"),
    ("missing-framework", ["beta-B"], "reject"),
    ("api-major-conflict", ["framework-0.1", "beta-conflict-B"], "reject"),
]


def rcon(loader: str, *commands: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, str(ROOT / "tools/rcon.py"),
                           "--properties", str(PROPS[loader]), *commands],
                          capture_output=True, text=True, encoding="utf-8", errors="replace")


def wait_ready(loader: str, proc: subprocess.Popen, deadline: float) -> bool:
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            return False
        if rcon(loader, "list").returncode == 0:
            return True
        time.sleep(2)
    return False


def run(loader: str, name: str, jars: list[str], expect: str) -> dict:
    server = SERVERS[loader]
    mods = server / "run/mods"
    if mods.exists():
        shutil.rmtree(mods)
    mods.mkdir(parents=True)
    for key in jars:
        shutil.copy(OUT / JARS[loader][key], mods)
    log = LOGS / f"{loader}-{name}.log"
    task = ":experiments:server-fabric:runServer" if loader == "fabric" else ":experiments:server-neoforge:runServer"
    env = dict(**__import__("os").environ, JAVA_HOME=JAVA_HOME)
    with log.open("w", encoding="utf-8", errors="replace") as handle:
        proc = subprocess.Popen([str(ROOT / "gradlew"), "-p", str(ROOT), task, "--console=plain"],
                                stdout=handle, stderr=subprocess.STDOUT, env=env, cwd=ROOT)
        ready = wait_ready(loader, proc, time.monotonic() + 150)
        lines = log.read_text(encoding="utf-8", errors="replace")
        shells = [line.split("[EXP-PKG] ", 1)[1] for line in lines.splitlines() if "[EXP-PKG]" in line]
        if ready:
            rcon(loader, "stop")
        try:
            proc.wait(timeout=90)
        except subprocess.TimeoutExpired:
            proc.kill()
    lines = log.read_text(encoding="utf-8", errors="replace")
    rejected = any(marker in lines for marker in (
        "Incompatible mods", "requires", "Missing", "not compatible", "failed to resolve",
        "ModResolutionException", "Mod loading has failed", "缺少", "incompatible"))
    if expect == "success":
        status = "pass" if ready and shells else "fail"
    else:
        status = "pass" if (not ready and rejected) else "fail"
    loot = sorted({line.split(" loot=")[1] for line in shells if " loot=" in line})
    return {
        "loader": loader, "scenario": name, "expect": expect, "status": status,
        "ready": ready, "shells": shells, "loot_identities": loot, "log": str(log.relative_to(ROOT)),
    }


def main() -> int:
    which = sys.argv[1] if len(sys.argv) > 1 else "all"
    loaders = ["fabric", "neoforge"] if which == "all" else [which]
    LOGS.mkdir(parents=True, exist_ok=True)
    results = []
    for loader in loaders:
        for name, jars, expect in SCENARIOS:
            print(f"[{loader}] {name} ...", flush=True)
            results.append(run(loader, name, jars, expect))
            print(f"    -> {results[-1]['status']}", flush=True)
    (OUT / "results.json").write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")
    failed = [r for r in results if r["status"] != "pass"]
    print(f"通过 {len(results) - len(failed)}/{len(results)}；结果 build/exp-pkg/results.json")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
