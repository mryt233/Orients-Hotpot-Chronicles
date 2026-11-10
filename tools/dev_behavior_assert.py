"""P3 行为断言：开发服 + 开发客户端，验证单次发包、事件各处理一次、按进程/模块/通道/方向统计。作者：mryt233

用法：
    python tools/dev_behavior_assert.py fabric|neoforge
流程：起开发服 → 布置一口有食材的锅 → 起开发客户端自动进服 → 探针 aim → 取 /hpt stats →
      断线（客户端摘要）→ 正常退出客户端 → RCON stop 服务端 → 解析双方日志断言。
日志：build/exp-pkg/logs/behavior-<loader>-{server,client}.log
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

# mryt233 改（2026-11-10）：版本与 JDK 读取唯一配置源。
from project_env import jdk21

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/exp-pkg/logs"
JAVA_HOME = str(jdk21())

CONFIG = {
    "fabric": {
        "server_task": ":fabric:runServer",
        "client_task": ":fabric:runClient",
        "props": ROOT / "fabric/run-server/server.properties",
        "client_run": ROOT / "fabric/run-client",
        "port": 25565,
    },
    "neoforge": {
        "server_task": ":neoforge:runServer",
        "client_task": ":neoforge:runClient",
        "props": ROOT / "neoforge/run-server/server.properties",
        "client_run": ROOT / "neoforge/run-client",
        "port": 25566,
    },
}


def rcon(loader: str, *commands: str):
    return subprocess.run([sys.executable, str(ROOT / "tools/rcon.py"), "--properties",
                           str(CONFIG[loader]["props"]), *commands],
                          capture_output=True, text=True, encoding="utf-8", errors="replace")


def probe(loader: str, probe_id: str, action: str, *extra: str) -> bool:
    result = subprocess.run([sys.executable, str(ROOT / "tools/client_probe.py"),
                             "--run-dir", str(CONFIG[loader]["client_run"].relative_to(ROOT)),
                             probe_id, action, *extra],
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode != 0:
        print("  探针失败：", result.stderr.strip())
        return False
    return True


def kill_tree(proc: subprocess.Popen) -> None:
    subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                   capture_output=True, text=True, encoding="utf-8", errors="replace")


def start(loader: str, task: str, log_path: Path, extra_args: str = "") -> subprocess.Popen:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    handle = log_path.open("w", encoding="utf-8", errors="replace")
    env = {**os.environ, "JAVA_HOME": JAVA_HOME}
    proc = subprocess.Popen(rf".\gradlew.bat -p . {task} --console=plain {extra_args}".strip(),
                            shell=True, stdout=handle, stderr=subprocess.STDOUT, env=env, cwd=ROOT)
    proc._log_handle = handle  # type: ignore[attr-defined]
    return proc


def wait(deadline_seconds: float, check) -> bool:
    end = time.monotonic() + deadline_seconds
    while time.monotonic() < end:
        if check():
            return True
        time.sleep(2)
    return False


def online_players(loader: str) -> int:
    result = rcon(loader, "list")
    match = re.search(r"There are (\d+) of a max", result.stdout or "")
    return int(match.group(1)) if match else 0


def parse_stats_text(text: str) -> dict:
    lines = [line.split("[ORIENT-FW] ", 1)[1].strip()
             for line in text.splitlines() if "[ORIENT-FW] " in line]
    # 摘要头行是 process=... init=1 installs=1；入口日志行（init process=...）不参与断言
    init = [line for line in lines if line.startswith("process=")]
    modules = {}
    for line in lines:
        if line.startswith("module "):
            fields = dict(re.findall(r"(\w+)=([^\s]+)", line))
            modules[fields.get("id", "?")] = fields
    net = {}
    for line in lines:
        if line.startswith("net "):
            fields = dict(re.findall(r"(\w+)=([^\s]+)", line))
            net[(fields.get("channel", "?"), fields.get("direction", "?"))] = fields
    duplicates = [line for line in text.splitlines() if "duplicate channel registration ignored" in line]
    return {"init": init, "modules": modules, "net": net, "duplicates": duplicates}


def find_summary(loader: str, trigger: str, text: str) -> dict:
    """取最后一次该 trigger 的摘要块（同一事件可能合法触发多次，只核对最后一次的计数）。"""
    lines = text.splitlines()
    start = None
    for index, line in enumerate(lines):
        if "[ORIENT-FW] summary trigger=" in line and f"trigger={trigger}" in line:
            start = index
    if start is None:
        return parse_stats_text("")
    block = []
    for line in lines[start + 1:]:
        if "[ORIENT-FW] summary trigger=" in line or "[ORIENT-FW] " not in line:
            break
        block.append(line.split("[ORIENT-FW] ", 1)[1].strip())
    joined = "\n".join("[ORIENT-FW] " + line for line in block)
    return parse_stats_text(joined)


def net_of(stats: dict, channel: str, direction: str) -> dict:
    return stats["net"].get((channel, direction), {})


def main() -> int:
    if len(sys.argv) < 2 or sys.argv[1] not in CONFIG:
        print(__doc__)
        return 2
    loader = sys.argv[1]
    cfg = CONFIG[loader]
    if "--from-logs" in sys.argv:
        return analyze(loader, OUT / f"behavior-{loader}-server.log",
                       OUT / f"behavior-{loader}-client.log")
    if rcon(loader, "list").returncode == 0:
        print("开发服已经在运行，请先停掉再跑断言。")
        return 2
    OUT.mkdir(parents=True, exist_ok=True)
    server_log = OUT / f"behavior-{loader}-server.log"
    client_log = OUT / f"behavior-{loader}-client.log"
    stamp = str(int(time.time()))
    marker = f"beh{stamp}"

    print("[1/7] 起开发服 ...")
    server = start(loader, cfg["server_task"], server_log)
    if not wait(300, lambda: rcon(loader, "list").returncode == 0):
        kill_tree(server)
        print("服务端未就绪，见", server_log)
        return 1
    try:
        print("[2/7] 布置带食材的锅 ...")
        rcon(loader, "forceload add 0 0 16 16")
        rcon(loader, "setblock 2 -59 0 orients_hotpot_chronicles:brick_stove[pot=iron]")
        rcon(loader, "data merge block 2 -59 0 {Base:0, FuelType:1, BurnTicks:2400, BurnTotal:2400, "
                     "Items:[{Slot:30b,id:\"orients_hotpot_chronicles:raw_ham_sausage\",count:1}]}")

        print("[3/7] 起开发客户端并自动进服 ...")
        # 清掉上次运行残留的探针请求（否则新客户端一启动就执行旧的 stop，实测踩过）
        stale = cfg["client_run"] / "hotpot-probe-request.json"
        if stale.is_file():
            stale.unlink()
        for stale_result in cfg["client_run"].glob(f"hotpot-probe-{marker}*.json"):
            stale_result.unlink()
        client = start(loader, cfg["client_task"], client_log,
                       f"-PhotpotTestServer=127.0.0.1:{cfg['port']} -PhotpotClientProbe=true")
        joined = wait(360, lambda: online_players(loader) > 0)
        if not joined:
            print("客户端未进服，见", client_log)
            probe(loader, marker + "x", "stop")
            kill_tree(client)
            return 1
        time.sleep(5)
        # 站到锅边：瞄准包在 8 格内才受理（ChopsticksAimPayload 距离校验）
        rcon(loader, "gamemode creative @a")
        rcon(loader, "tp @a 0 -59 0")
        time.sleep(3)

        print("[4/7] 探针 snapshot + aim ...")
        probe(loader, marker + "a", "snapshot")
        # bigCell=4 覆盖 30-32/39-41/48-50 这 9 个小格，食材在 Slot 30
        probe(loader, marker + "b", "aim", "2", "-59", "0", "4")
        time.sleep(3)
        result = rcon(loader, "hpt stats")
        print(result.stdout.strip())
        stats_cmd = result.stdout

        print("[5/7] 断开客户端（触发客户端摘要）...")
        probe(loader, marker + "c", "disconnect")
        time.sleep(5)
        print("[6/7] 正常退出客户端 ...")
        probe(loader, marker + "d", "stop")
        try:
            client.wait(timeout=120)
        except subprocess.TimeoutExpired:
            kill_tree(client)
            client.wait(timeout=30)
        client._log_handle.close()  # type: ignore[attr-defined]
    finally:
        print("[7/7] 停止服务端 ...")
        rcon(loader, "stop")
        try:
            server.wait(timeout=180)
        except subprocess.TimeoutExpired:
            kill_tree(server)
            server.wait(timeout=30)
        server._log_handle.close()  # type: ignore[attr-defined]

    return analyze(loader, server_log, client_log, stats_cmd)


ERROR_LINE_RE = re.compile(r"\[(?:[^\]]*/)?(?:ERROR|FATAL)\]|Exception in thread")

# 火锅必须出现的三条通道（缺一条即失败，避免摘要末尾被截断时误过；复核）
EXPECTED_CHANNELS = (
    ("orients_hotpot_chronicles:config", "S2C"),
    ("orients_hotpot_chronicles:chopsticks_aim", "C2S"),
    ("orients_hotpot_chronicles:open_sauce_restock", "C2S"),
)


def stats_problems(process: str, stats: dict) -> list[str]:
    """把摘要计数与本轮期望逐字段精确比较（不能用子串，否则 init=10 会误过）。"""
    problems: list[str] = []
    init = stats["init"]
    if len(init) != 1:
        problems.append(f"{process} init 摘要行数 {len(init)}（应为 1）：{init}")
    else:
        fields = dict(re.findall(r"(\w+)=([^\s]+)", init[0]))
        if fields.get("init") != "1" or fields.get("installs") != "1":
            problems.append(f"{process} init/installs 不是 1：{init[0]}")
    module = stats["modules"].get("orients_hotpot_chronicles", {})
    if module.get("register") != "1" or module.get("runtime") != "1":
        problems.append(f"{process} 模块计数异常：{module}")
    if process == "client" and module.get("client") != "1":
        problems.append(f"client 模块 client 阶段不是 1：{module}")
    if stats["duplicates"]:
        problems.append(f"{process} 重复通道登记 {len(stats['duplicates'])} 次")
    for (channel, direction), fields in stats["net"].items():
        if fields.get("registered") != "1":
            problems.append(f"{process} 通道 {channel}/{direction} registered={fields.get('registered')}")
    for channel, direction in EXPECTED_CHANNELS:
        if (channel, direction) not in stats["net"]:
            problems.append(f"{process} 摘要缺少通道 {channel}/{direction}")
    return problems


def log_problems(process: str, text: str) -> list[str]:
    """扫完整日志里的错误行（重复登记由 stats 的 duplicates 统一报告）。"""
    problems: list[str] = []
    for line in text.splitlines():
        if "duplicate channel registration ignored" in line:
            continue
        if ERROR_LINE_RE.search(line):
            problems.append(f"{process} 日志出现错误行：{line.strip()[:160]}")
            break
    return problems


def analyze(loader: str, server_log: Path, client_log: Path, stats_cmd: str = "") -> int:
    """从日志复算行为断言（--from-logs 或正式运行结束时调用）。"""

    server_text = server_log.read_text(encoding="utf-8", errors="replace")
    client_text = client_log.read_text(encoding="utf-8", errors="replace")
    server_stats = find_summary(loader, "server-stopped", server_text)
    client_stats = find_summary(loader, "client-disconnect", client_text)
    # 重复通道告警打在摘要块之外，必须扫完整日志（指出的解析口径）
    server_stats["duplicates"] = [line for line in server_text.splitlines()
                                  if "duplicate channel registration ignored" in line]
    client_stats["duplicates"] = [line for line in client_text.splitlines()
                                  if "duplicate channel registration ignored" in line]
    problems: list[str] = []
    problems.extend(stats_problems("server", server_stats))
    problems.extend(stats_problems("client", client_stats))
    problems.extend(log_problems("server", server_text))
    problems.extend(log_problems("client", client_text))
    checks = [
        ("server config S2C sent", net_of(server_stats, "orients_hotpot_chronicles:config", "S2C").get("sent"), "1"),
        ("server aim C2S handled", net_of(server_stats, "orients_hotpot_chronicles:chopsticks_aim", "C2S").get("handled"), "1"),
        ("client config S2C handled", net_of(client_stats, "orients_hotpot_chronicles:config", "S2C").get("handled"), "1"),
        ("client aim C2S sent", net_of(client_stats, "orients_hotpot_chronicles:chopsticks_aim", "C2S").get("sent"), "1"),
    ]
    for label, actual, expected in checks:
        if actual != expected:
            problems.append(f"{label} = {actual}（应为 {expected}）")

    def serial(stats: dict) -> dict:
        return {"init": stats["init"], "modules": stats["modules"],
                "net": {f"{channel}/{direction}": fields
                        for (channel, direction), fields in stats["net"].items()},
                "duplicates": stats["duplicates"]}

    report = {
        "loader": loader, "status": "pass" if not problems else "fail",
        "problems": problems,
        "server": serial(server_stats), "client": serial(client_stats),
        "hpt_stats": stats_cmd.strip().splitlines()[-1:] if stats_cmd else [],
        "logs": [str(server_log.relative_to(ROOT)), str(client_log.relative_to(ROOT))],
    }
    (OUT / f"behavior-{loader}.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
