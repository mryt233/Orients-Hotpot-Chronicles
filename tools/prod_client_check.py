"""独立生产客户端 + 服务端端到端验证（候选 JAR，生产命名）。作者：mryt233

用法：
    python tools/prod_client_check.py
前置：
    experiments/prod-fabric-client 已由 Fabric 安装器安装（versions/libraries）；
    experiments/harness-client 已 build 出测试助爪 jar。
流程：起生产服务端 → 布置带食材的锅 → 用生产客户端（候选 Runtime+内容+助爪）连入 →
      助爪发一次瞄准包后退出 → 停服 → 解析两端日志与统计。
输出：build/exp-pkg/logs/prod-client-{server,client}.log / prod-client.json
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

import rcon_lib

ROOT = Path(__file__).resolve().parent.parent
LOGS = ROOT / "build/exp-pkg/logs"
JAVA = "C:/Users/10536/.jdks/jdk-21.0.12.1+1/bin/java.exe"
SERVER_DIR = ROOT / "experiments/prod-fabric-server"
CLIENT_DIR = ROOT / "experiments/prod-fabric-client"
LOOM = Path.home() / ".gradle/caches/fabric-loom"
GRADLE_MODULES = Path.home() / ".gradle/caches/modules-2/files-2.1"
HARNESS_JAR = ROOT / "experiments/harness-client/build/libs/hotpot-harness-client-1.0.0.jar"
CANDIDATES = ROOT / "build/exp-pkg/candidate"


def rcon(*commands: str) -> bool:
    try:
        rcon_lib.execute(SERVER_DIR / "server.properties", *commands)
        return True
    except (OSError, ValueError, rcon_lib._rcon.RconError):
        return False


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def allowed(library: dict) -> bool:
    rules = library.get("rules")
    if not rules:
        return True
    allow = False
    for rule in rules:
        os_name = (rule.get("os") or {}).get("name")
        if rule.get("action") == "allow" and (os_name is None or os_name == "windows"):
            allow = True
        elif rule.get("action") == "disallow" and os_name == "windows":
            allow = False
    return allow


def find_in_gradle(filename: str) -> Path | None:
    for path in GRADLE_MODULES.rglob(filename):
        if path.is_file() and "sources" not in path.name and "javadoc" not in path.name:
            return path
    return None


def resolve_library(artifact: dict) -> Path | None:
    dest = CLIENT_DIR / "libraries" / artifact["path"]
    if dest.is_file():
        return dest
    found = find_in_gradle(Path(artifact["path"]).name)
    if found is not None:
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(found, dest)
        return dest
    try:
        dest.parent.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve(artifact["url"], dest)
        return dest
    except Exception as exc:  # 网络失败时报告，不静默
        print(f"下载失败 {artifact['path']}: {exc}", file=sys.stderr)
        return None


def prepare_client() -> tuple[list[str], Path]:
    version = json.loads((CLIENT_DIR / "versions/1.21.1.json").read_text(encoding="utf-8"))
    natives = CLIENT_DIR / "natives"
    natives.mkdir(parents=True, exist_ok=True)
    classpath: list[str] = []
    for library in version["libraries"]:
        if not allowed(library):
            continue
        downloads = library.get("downloads") or {}
        artifact = downloads.get("artifact")
        if artifact is None:
            continue
        if any(tag in artifact["path"] for tag in ("natives-linux", "natives-macos", "natives-windows-arm64",
                                                   "natives-windows-x86")):
            continue
        jar = resolve_library(artifact)
        if jar is None:
            continue
        classpath.append(str(jar))
        if "natives-windows" in artifact["path"]:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith(".dll"):
                        with archive.open(name) as source, (natives / Path(name).name).open("wb") as target:
                            shutil.copyfileobj(source, target)

    # Fabric loader 依赖（安装器已下载到 client/libraries）
    for jar in sorted((CLIENT_DIR / "libraries").rglob("*.jar")):
        classpath.append(str(jar))

    # 原版客户端 jar
    client_jar = CLIENT_DIR / "versions/1.21.1/1.21.1.jar"
    if not client_jar.is_file():
        client_jar.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(LOOM / "1.21.1/minecraft-client.jar", client_jar)
    classpath.append(str(client_jar))

    # 资源：index 17 + objects junction
    assets = CLIENT_DIR / "assets"
    (assets / "indexes").mkdir(parents=True, exist_ok=True)
    shutil.copy(LOOM / "assets/indexes/1.21.1-17.json", assets / "indexes/17.json")
    objects = assets / "objects"
    if not objects.exists():
        target = LOOM / "assets/objects"
        subprocess.run(["cmd", "/c", "mklink", "/J", str(objects), str(target)],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
        if not objects.exists():
            raise SystemExit("无法创建 assets/objects 目录联接，请手动处理")
    return classpath, natives


def prepare_mods() -> list[Path]:
    mods = CLIENT_DIR / "mods"
    if mods.exists():
        shutil.rmtree(mods)
    mods.mkdir(parents=True)
    jars = [
        CANDIDATES / "orient_framework-fabric-0.1.1.jar",
        CANDIDATES / "orients_hotpot_chronicles-fabric-1.0.0-r130.jar",
        HARNESS_JAR,
        Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.116.14+1.21.1/1bc9caf399a9eadddaec3215e31594ca40fc9485/fabric-api-0.116.14+1.21.1.jar",
    ]
    for jar in jars:
        shutil.copy(jar, mods / jar.name)
    return jars


def start_server(log_path: Path) -> subprocess.Popen:
    handle = log_path.open("w", encoding="utf-8", errors="replace")
    proc = subprocess.Popen([JAVA, "-Xmx2G", "-jar", "fabric-server-launch.jar", "nogui"],
                            cwd=SERVER_DIR, stdout=handle, stderr=subprocess.STDOUT)
    proc._log_handle = handle  # type: ignore[attr-defined]
    return proc


def wait_ready(proc: subprocess.Popen, seconds: float) -> bool:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            return False
        if rcon("list"):
            return True
        time.sleep(3)
    return False


def kill_tree(proc: subprocess.Popen) -> None:
    subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                   capture_output=True, text=True, encoding="utf-8", errors="replace")


def last_summary(text: str, trigger: str) -> dict:
    lines = text.splitlines()
    start = None
    for index, line in enumerate(lines):
        if "[ORIENT-FW] summary trigger=" in line and f"trigger={trigger}" in line:
            start = index
    block = []
    if start is not None:
        for line in lines[start + 1:]:
            if "[ORIENT-FW] " not in line:
                break
            block.append(line.split("[ORIENT-FW] ", 1)[1].strip())
    init = [line for line in block if line.startswith("process=")]
    modules, net = {}, {}
    for line in block:
        fields = dict(re.findall(r"(\w+)=([^\s]+)", line))
        if line.startswith("module "):
            modules[fields.get("id", "?")] = fields
        elif line.startswith("net "):
            net[(fields.get("channel", "?"), fields.get("direction", "?"))] = fields
    return {"init": init, "modules": modules, "net": net}


def main() -> int:
    LOGS.mkdir(parents=True, exist_ok=True)
    server_log = LOGS / "prod-client-server.log"
    if "--from-logs" in sys.argv:
        return analyze(server_log, LOGS / "prod-client-client.log",
                       [CANDIDATES / "orient_framework-fabric-0.1.1.jar",
                        CANDIDATES / "orients_hotpot_chronicles-fabric-1.0.0-r130.jar", HARNESS_JAR])
    client_log = LOGS / "prod-client-client.log"
    jars = prepare_mods()
    classpath, natives = prepare_client()

    print("[1/6] 起生产服务端 ...")
    server = start_server(server_log)
    if not wait_ready(server, 300):
        kill_tree(server)
        raise SystemExit("生产服务端未就绪，见 " + str(server_log))
    try:
        print("[2/6] 布置带食材的锅 ...")
        rcon("forceload add 0 0 16 16")
        rcon("setworldspawn 0 -59 0")
        rcon("setblock 2 -59 0 orients_hotpot_chronicles:brick_stove[pot=iron]")
        rcon("data merge block 2 -59 0 {Base:0, FuelType:1, BurnTicks:2400, BurnTotal:2400, "
             'Items:[{Slot:30b,id:"orients_hotpot_chronicles:raw_ham_sausage",count:1}]}')

        print("[3/6] 起生产客户端（quickPlay 进服）...")
        command = [JAVA, "-Xmx2G", f"-Djava.library.path={natives}",
                   f"-Djna.tmpdir={natives}", f"-Dorg.lwjgl.system.SharedLibraryExtractPath={natives}",
                   f"-Dio.netty.native.workdir={natives}", "-Dminecraft.launcher.brand=harness",
                   "-Dlog4j2.formatMsgNoLookups=true",
                   "-cp", os.pathsep.join(classpath),
                   "net.fabricmc.loader.impl.launch.knot.KnotClient",
                   "--username", "Harness", "--uuid", "00000000000000000000000000000001",
                   "--accessToken", "0", "--userType", "legacy", "--version", "1.21.1",
                   "--versionType", "release", "--gameDir", str(CLIENT_DIR),
                   "--assetsDir", str(CLIENT_DIR / "assets"), "--assetIndex", "17",
                   "--width", "854", "--height", "480",
                   "--quickPlayMultiplayer", "127.0.0.1:25572"]
        handle = client_log.open("w", encoding="utf-8", errors="replace")
        client = subprocess.Popen(command, cwd=CLIENT_DIR, stdout=handle, stderr=subprocess.STDOUT)
        # 进服后把测试玩家拉到锅边：客户端只加载玩家附近的区块
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline and client.poll() is None:
            try:
                listing = rcon_lib.execute(SERVER_DIR / "server.properties", "list")[0]
            except (OSError, ValueError, rcon_lib._rcon.RconError):
                listing = ""
            if "Harness" in listing:
                rcon("gamemode creative Harness")
                rcon("tp Harness 0 -59 0")
                break
            time.sleep(2)
        try:
            client.wait(timeout=420)
        except subprocess.TimeoutExpired:
            kill_tree(client)
            client.wait(timeout=30)
        handle.close()
    finally:
        print("[4/6] 停生产服务端 ...")
        rcon("stop")
        try:
            server.wait(timeout=180)
        except subprocess.TimeoutExpired:
            kill_tree(server)
            server.wait(timeout=30)
        server._log_handle.close()  # type: ignore[attr-defined]

    return analyze(server_log, client_log, jars)


def analyze(server_log: Path, client_log: Path, jars: list[Path]) -> int:
    server_text = server_log.read_text(encoding="utf-8", errors="replace")
    client_text = client_log.read_text(encoding="utf-8", errors="replace")
    server_stats = last_summary(server_text, "server-stopped")
    client_stats = last_summary(client_text, "client-disconnect")
    problems: list[str] = []

    def net(stats, channel, direction, field):
        return stats["net"].get((channel, direction), {}).get(field)

    checks = [
        ("服务端 init=1", (server_stats["init"][0] if len(server_stats["init"]) == 1 else ""), "init=1"),
        ("服务端 config S2C sent=1", net(server_stats, "orients_hotpot_chronicles:config", "S2C", "sent"), "1"),
        ("服务端 aim C2S handled=1", net(server_stats, "orients_hotpot_chronicles:chopsticks_aim", "C2S", "handled"), "1"),
        ("客户端 init=1", (client_stats["init"][0] if len(client_stats["init"]) == 1 else ""), "init=1"),
        ("客户端 config S2C handled=1", net(client_stats, "orients_hotpot_chronicles:config", "S2C", "handled"), "1"),
        ("客户端 aim C2S sent=1", net(client_stats, "orients_hotpot_chronicles:chopsticks_aim", "C2S", "sent"), "1"),
    ]
    for label, actual, wanted in checks:
        if wanted == "init=1":
            fields = dict(re.findall(r"(\w+)=([^\s]+)", actual or ""))
            if fields.get("init") != "1" or fields.get("installs") != "1":
                problems.append(f"{label} 不满足：{actual!r}")
        elif actual != wanted:
            problems.append(f"{label} = {actual}（应为 {wanted}）")
    for who, stats in (("服务端", server_stats), ("客户端", client_stats)):
        for channel, direction in (("orients_hotpot_chronicles:config", "S2C"),
                                   ("orients_hotpot_chronicles:chopsticks_aim", "C2S"),
                                   ("orients_hotpot_chronicles:open_sauce_restock", "C2S")):
            fields = stats["net"].get((channel, direction))
            if fields is None:
                problems.append(f"{who}摘要缺少通道 {channel}/{direction}")
            elif fields.get("registered") != "1":
                problems.append(f"{who}通道 {channel}/{direction} registered={fields.get('registered')}（应为 1）")
    client_module = client_stats["modules"].get("orients_hotpot_chronicles", {})
    if client_module.get("client") != "1":
        problems.append(f"客户端模块 client 阶段不是 1：{client_module}")
    if "duplicate channel registration ignored" in server_text + client_text:
        problems.append("出现重复通道登记告警")
    if re.search(r"\[(?:[^\]]*/)?(?:ERROR|FATAL)\]", client_text):
        match = re.search(r".*\[(?:[^\]]*/)?(?:ERROR|FATAL)\].*", client_text)
        problems.append("客户端日志出现错误行：" + (match.group(0)[:160] if match else ""))
    if "[HARNESS] aim" not in client_text:
        problems.append("助爪没有发出瞄准包（见客户端日志）")

    report = {
        "status": "pass" if not problems else "fail",
        "problems": problems,
        "server": {"init": server_stats["init"], "modules": server_stats["modules"],
                   "net": {f"{k[0]}/{k[1]}": v for k, v in server_stats["net"].items()}},
        "client": {"init": client_stats["init"], "modules": client_stats["modules"],
                   "net": {f"{k[0]}/{k[1]}": v for k, v in client_stats["net"].items()}},
        "mods": [{"name": jar.name, "sha256": sha256(ROOT / "experiments/prod-fabric-client/mods" / jar.name)}
                 for jar in jars],
        "logs": [str(server_log.relative_to(ROOT)), str(client_log.relative_to(ROOT))],
    }
    (LOGS / "prod-client.json").write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not problems else 1




if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
