"""独立生产环境验证：候选 JAR 装进非工程启动器的服务端（Fabric / NeoForge）。作者：mryt233

用法：
    python tools/prod_server_check.py fabric
    python tools/prod_server_check.py fabric --from-log
目录：experiments/prod-<loader>-server（自带 eula / server.properties / mods / 启动器）
日志：build/exp-pkg/logs/prod-<loader>-server.log
断言：启动成功 + [ORIENT-FW] init=1/installs=1 + 模块 register=1 + 通道 registered=1。
"""
from __future__ import annotations

import json
import re
import subprocess
import sys
import time
from pathlib import Path

import rcon_lib

ROOT = Path(__file__).resolve().parent.parent
LOGS = ROOT / "build/exp-pkg/logs"
JAVA = "C:/Users/10536/.jdks/jdk-21.0.12.1+1/bin/java.exe"

CONFIG = {
    "fabric": {
        "dir": ROOT / "experiments/prod-fabric-server",
        "command": [JAVA, "-Xmx2G", "-jar", "fabric-server-launch.jar", "nogui"],
        "props": ROOT / "experiments/prod-fabric-server/server.properties",
        "startup": 600,
    },
    "neoforge": {
        "dir": ROOT / "experiments/prod-neoforge-server",
        "command": [JAVA, "-Xmx2G", "@user_jvm_args.txt", "@libraries/net/neoforged/neoforge/21.1.265/win_args.txt"],
        "props": ROOT / "experiments/prod-neoforge-server/server.properties",
        "startup": 600,
    },
}


def rcon(loader: str, *commands: str) -> bool:
    try:
        rcon_lib.execute(CONFIG[loader]["props"], *commands)
        return True
    except (OSError, ValueError, rcon_lib._rcon.RconError):
        return False


def kill_tree(proc: subprocess.Popen) -> None:
    subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                   capture_output=True, text=True, encoding="utf-8", errors="replace")


def parse_stats(text: str) -> dict:
    # 只取最后一个摘要块（server-started 与 server-stopped 各有一份）
    all_lines = text.splitlines()
    start = None
    for index, line in enumerate(all_lines):
        if "[ORIENT-FW] summary trigger=" in line:
            start = index
    block = []
    if start is not None:
        for line in all_lines[start + 1:]:
            if "[ORIENT-FW] " not in line:
                break
            block.append(line.split("[ORIENT-FW] ", 1)[1].strip())
    lines = block
    init = [line for line in lines if line.startswith("process=")]
    modules, net = {}, {}
    for line in lines:
        fields = dict(re.findall(r"(\w+)=([^\s]+)", line))
        if line.startswith("module "):
            modules[fields.get("id", "?")] = fields
        elif line.startswith("net "):
            net[(fields.get("channel", "?"), fields.get("direction", "?"))] = fields
    duplicates = [line for line in text.splitlines() if "duplicate channel registration ignored" in line]
    errors = [line for line in text.splitlines()
              if re.search(r"\[(?:[^\]]*/)?(?:ERROR|FATAL)\]", line)]
    return {"init": init, "modules": modules, "net": net, "duplicates": duplicates, "errors": errors}


def evaluate(stats: dict, recipes: int) -> list[str]:
    problems: list[str] = []
    if len(stats["init"]) != 1:
        problems.append(f"init 摘要行数 {len(stats['init'])}（应为 1）")
    else:
        fields = dict(re.findall(r"(\w+)=([^\s]+)", stats["init"][0]))
        if fields.get("init") != "1" or fields.get("installs") != "1":
            problems.append("init/installs 不是 1：" + stats["init"][0])
    module = stats["modules"].get("orients_hotpot_chronicles", {})
    if module.get("register") != "1" or module.get("runtime") != "1":
        problems.append(f"模块计数异常：{module}")
    for (channel, direction), fields in stats["net"].items():
        if fields.get("registered") != "1":
            problems.append(f"通道 {channel}/{direction} registered={fields.get('registered')}")
    for channel, direction in (("orients_hotpot_chronicles:config", "S2C"),
                               ("orients_hotpot_chronicles:chopsticks_aim", "C2S"),
                               ("orients_hotpot_chronicles:open_sauce_restock", "C2S")):
        if (channel, direction) not in stats["net"]:
            problems.append(f"摘要缺少通道 {channel}/{direction}")
    if stats["duplicates"]:
        problems.append(f"重复通道登记 {len(stats['duplicates'])} 次")
    if stats["errors"]:
        problems.append("日志出现错误行：" + stats["errors"][0].strip()[:160])
    if recipes < 1000:
        problems.append(f"配方只有 {recipes} 条（应 ≥1000）")
    return problems


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args or args[0] not in CONFIG:
        print(__doc__)
        return 2
    loader = args[0]
    cfg = CONFIG[loader]
    LOGS.mkdir(parents=True, exist_ok=True)
    log_path = LOGS / f"prod-{loader}-server.log"

    if "--from-log" in sys.argv:
        text = log_path.read_text(encoding="utf-8", errors="replace")
        recipes = int(re.search(r"Loaded (\d+) recipes", text).group(1)) if re.search(r"Loaded (\d+) recipes", text) else 0
        stats = parse_stats(text)
        problems = evaluate(stats, recipes)
        report = {"loader": loader, "status": "pass" if not problems else "fail",
                  "problems": problems, "recipes": recipes, "log": str(log_path.relative_to(ROOT))}
        print(json.dumps(report, indent=2, ensure_ascii=False))
        return 0 if not problems else 1

    if rcon(loader, "list"):
        print("服务端已在运行，请先停掉")
        return 2
    handle = log_path.open("w", encoding="utf-8", errors="replace")
    proc = subprocess.Popen([str(part) for part in cfg["command"]], cwd=cfg["dir"],
                            stdout=handle, stderr=subprocess.STDOUT)
    ready = False
    deadline = time.monotonic() + cfg["startup"]
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            break
        if rcon(loader, "list"):
            ready = True
            break
        time.sleep(3)
    if ready:
        rcon(loader, "stop")
    try:
        proc.wait(timeout=300)
    except subprocess.TimeoutExpired:
        kill_tree(proc)
        proc.wait(timeout=30)
    handle.close()

    text = log_path.read_text(encoding="utf-8", errors="replace")
    match = re.search(r"Loaded (\d+) recipes", text)
    recipes = int(match.group(1)) if match else 0
    stats = parse_stats(text)
    problems = evaluate(stats, recipes)
    if not ready:
        problems.insert(0, "服务端未就绪（见日志）")
    report = {"loader": loader, "status": "pass" if not problems else "fail",
              "problems": problems, "recipes": recipes,
              "stats": {"init": stats["init"],
                        "modules": stats["modules"],
                        "net": {f"{k[0]}/{k[1]}": v for k, v in stats["net"].items()}},
              "log": str(log_path.relative_to(ROOT))}
    (LOGS / f"prod-{loader}-server.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
