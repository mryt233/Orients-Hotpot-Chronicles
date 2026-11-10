"""NeoForge 独立生产客户端 + 服务端端到端验证（候选 JAR）。作者：mryt233

用法：
    python tools/prod_neo_client_check.py
前置：experiments/prod-neoforge-client 已由 NeoForge 安装器安装；
      experiments/harness-client-neoforge 已构建。
流程：起生产服务端 → 布置带食材的锅 → 用 Neo client（BootstrapLauncher + 候选 jar + 助爪）连入 →
      助爪走真实瞄准回调后退出 → 停服 → 解析两端统计。输出 build/exp-pkg/logs/prod-neo-client*.log / prod-neo-client.json
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

# mryt233 改（2026-11-10）：版本与 JDK 读取唯一配置源。
from project_env import jdk21, mod_version, runtime_version

ROOT = Path(__file__).resolve().parent.parent
LOGS = ROOT / "build/exp-pkg/logs"
JAVA = str(jdk21() / "bin/java.exe")
SERVER_DIR = ROOT / "experiments/prod-neoforge-server"
CLIENT_DIR = ROOT / "experiments/prod-neoforge-client"
GRADLE_MODULES = Path.home() / ".gradle/caches/modules-2/files-2.1"
LOOM = Path.home() / ".gradle/caches/fabric-loom"
HARNESS_JAR = ROOT / "experiments/harness-client-neoforge/build/libs/hotpot-harness-neoforge-1.0.0.jar"
CANDIDATES = ROOT / "build/exp-pkg/candidate"
VERSION = "neoforge-21.1.265"

sys.path.insert(0, str(ROOT / "tools"))
import rcon_lib  # noqa: E402


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
        name = rule.get("os") or {}
        if rule.get("action") == "allow" and (name.get("name") in (None, "windows")):
            allow = True
        elif rule.get("action") == "disallow" and name.get("name") == "windows":
            allow = False
    return allow


def resolve(artifact: dict, base: Path) -> Path | None:
    dest = base / artifact["path"]
    if dest.is_file():
        return dest
    found = next((p for p in GRADLE_MODULES.rglob(Path(artifact["path"]).name)
                  if p.is_file() and "sources" not in p.name and "javadoc" not in p.name), None)
    if found is not None:
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(found, dest)
        return dest
    try:
        dest.parent.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve(artifact["url"], dest)
        return dest
    except Exception as exc:
        print(f"下载失败 {artifact['path']}: {exc}", file=sys.stderr)
        return None


def prepare_mods() -> list[Path]:
    mods = CLIENT_DIR / "mods"
    if mods.exists():
        shutil.rmtree(mods)
    mods.mkdir(parents=True)
    jars = [
        CANDIDATES / f"orient_framework-neoforge-{runtime_version()}.jar",
        CANDIDATES / f"orients_hotpot_chronicles-neoforge-{mod_version()}.jar",
        HARNESS_JAR,
    ]
    for jar in jars:
        shutil.copy(jar, mods / jar.name)
    return jars


def prepare_command() -> list[str]:
    """按 NeoForge 官方安装器的做法组装：模块路径启动 BootstrapLauncher，
    legacyClassPath 用文件给出（FML 库 + 原版客户端库），NeoForge/MC jar 由 libraryDirectory 约定解析。"""
    neo = json.loads((CLIENT_DIR / f"versions/{VERSION}/{VERSION}.json").read_text(encoding="utf-8"))
    vanilla = json.loads((CLIENT_DIR / "versions/1.21.1/1.21.1.json").read_text(encoding="utf-8"))
    libraries = CLIENT_DIR / "libraries"
    natives = CLIENT_DIR / "natives"
    natives.mkdir(parents=True, exist_ok=True)

    # 客户端库（原版库，含 natives 解压）
    entries: list[str] = []
    for library in vanilla["libraries"]:
        if not allowed(library):
            continue
        artifact = (library.get("downloads") or {}).get("artifact")
        if artifact is None:
            continue
        jar = resolve(artifact, libraries)
        if jar is None:
            continue
        entries.append(str(jar))
        if "natives-windows" in artifact["path"]:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith(".dll"):
                        with archive.open(name) as source, (natives / Path(name).name).open("wb") as target:
                            shutil.copyfileobj(source, target)

    # NeoForge 运行时库：从服务端安装器的 win_args 取同一份列表（顺序一致）
    win_args = SERVER_DIR / "libraries/net/neoforged/neoforge/21.1.265/win_args.txt"
    parts = shlex.split(win_args.read_text(encoding="utf-8")) if win_args.is_file() else []
    server_legacy = next((part[len("-DlegacyClassPath="):] for part in parts
                          if part.startswith("-DlegacyClassPath=")), "")
    # rel 里已经带 libraries/ 前缀，必须相对 CLIENT_DIR 解析（否则变成 libraries/libraries）
    fml_entries = [str(CLIENT_DIR / rel.replace("/", os.sep)) for rel in server_legacy.split(";") if rel]
    ordered: list[str] = []
    seen: set[str] = set()
    for entry in fml_entries + entries:
        if entry not in seen:
            seen.add(entry)
            ordered.append(entry)
    classpath_file = CLIENT_DIR / "legacy-classpath.txt"
    classpath_file.write_text(chr(10).join(ordered), encoding="utf-8")

    assets = CLIENT_DIR / "assets"
    (assets / "indexes").mkdir(parents=True, exist_ok=True)
    shutil.copy(LOOM / "assets/indexes/1.21.1-17.json", assets / "indexes/17.json")
    objects = assets / "objects"
    if not objects.exists():
        subprocess.run(["cmd", "/c", "mklink", "/J", str(objects), str(LOOM / "assets/objects")],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
        if not objects.exists():
            raise SystemExit("无法创建 assets/objects 目录联接")

    replacements = {
        "${library_directory}": str(libraries),
        "${classpath_separator}": ";",
        "${version_name}": VERSION,
        "${natives_directory}": str(natives),
        "${launcher_name}": "harness",
        "${launcher_version}": "1.0",
        "${classpath}": ";".join(ordered),
        "${auth_player_name}": "HarnessNeo",
        "${auth_uuid}": "00000000000000000000000000000002",
        "${auth_access_token}": "0",
        "${clientid}": "0",
        "${auth_xuid}": "0",
        "${user_type}": "legacy",
        "${version_type}": "release",
        "${game_directory}": str(CLIENT_DIR),
        "${assets_root}": str(assets),
        "${assets_index_name}": "17",
        "${resolution_width}": "854",
        "${resolution_height}": "480",
        "${quickPlayPath}": "",
    }

    def fill(value):
        for key, replacement in replacements.items():
            value = value.replace(key, replacement)
        return value

    command = [JAVA, "-Xmx2G", "-DlegacyClassPath.file=" + str(classpath_file)]
    # 原版 JVM 参数里跳过 -cp 及其值（改用 legacyClassPath.file 提供类路径）
    vanilla_jvm = vanilla["arguments"]["jvm"]
    index = 0
    while index < len(vanilla_jvm):
        argument = vanilla_jvm[index]
        index += 1
        if isinstance(argument, dict):
            continue
        if argument == "-cp":
            index += 1  # 同时跳过紧随其后的类路径值
            continue
        command.append(fill(argument))
    for argument in neo["arguments"]["jvm"]:
        command.append(fill(argument))
    command.append(neo["mainClass"])
    for argument in neo["arguments"]["game"]:
        command.append(fill(argument))
    for argument in vanilla["arguments"]["game"]:
        if isinstance(argument, dict):
            continue
        command.append(fill(argument))
    command.extend(["--quickPlayMultiplayer", "127.0.0.1:25573"])
    return command


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
    server_log = LOGS / "prod-neo-client-server.log"
    if "--from-logs" in sys.argv:
        return analyze(server_log, LOGS / "prod-neo-client-client.log",
                       [CANDIDATES / f"orient_framework-neoforge-{runtime_version()}.jar",
                        CANDIDATES / f"orients_hotpot_chronicles-neoforge-{mod_version()}.jar", HARNESS_JAR])
    client_log = LOGS / "prod-neo-client-client.log"
    jars = prepare_mods()
    command = prepare_command()
    (LOGS / "prod-neo-client-command.txt").write_text(" ".join(command), encoding="utf-8")

    print("[1/6] 起生产 NeoForge 服务端 ...")
    handle = server_log.open("w", encoding="utf-8", errors="replace")
    server = subprocess.Popen([JAVA, "-Xmx2G", "@user_jvm_args.txt",
                               "@libraries/net/neoforged/neoforge/21.1.265/win_args.txt"],
                              cwd=SERVER_DIR, stdout=handle, stderr=subprocess.STDOUT)
    ready = False
    deadline = time.monotonic() + 600
    while time.monotonic() < deadline:
        if server.poll() is not None:
            break
        if rcon("list"):
            ready = True
            break
        time.sleep(3)
    if not ready:
        handle.close()
        raise SystemExit("生产 NeoForge 服务端未就绪，见 " + str(server_log))
    try:
        print("[2/6] 布置带食材的锅 ...")
        rcon("forceload add 0 0 16 16")
        rcon("setworldspawn 0 -59 0")
        rcon("setblock 2 -59 0 orients_hotpot_chronicles:brick_stove[pot=iron]")
        rcon("data merge block 2 -59 0 {Base:0, FuelType:1, BurnTicks:2400, BurnTotal:2400, "
             'Items:[{Slot:30b,id:"orients_hotpot_chronicles:raw_ham_sausage",count:1}]}')

        print("[3/6] 起生产 NeoForge 客户端 ...")
        client_handle = client_log.open("w", encoding="utf-8", errors="replace")
        client = subprocess.Popen(command, cwd=CLIENT_DIR, stdout=client_handle, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 240
        while time.monotonic() < deadline and client.poll() is None:
            try:
                listing = rcon_lib.execute(SERVER_DIR / "server.properties", "list")[0]
            except (OSError, ValueError, rcon_lib._rcon.RconError):
                listing = ""
            if "HarnessNeo" in listing:
                rcon("gamemode creative HarnessNeo")
                rcon("tp HarnessNeo 0 -59 0")
                break
            time.sleep(2)
        try:
            client.wait(timeout=420)
        except subprocess.TimeoutExpired:
            subprocess.run(["taskkill", "/F", "/T", "/PID", str(client.pid)], capture_output=True)
            client.wait(timeout=30)
        client_handle.close()
    finally:
        print("[4/6] 停生产 NeoForge 服务端 ...")
        rcon("stop")
        try:
            server.wait(timeout=180)
        except subprocess.TimeoutExpired:
            subprocess.run(["taskkill", "/F", "/T", "/PID", str(server.pid)], capture_output=True)
            server.wait(timeout=30)
        handle.close()

    return analyze(server_log, client_log, jars)


def analyze(server_log: Path, client_log: Path, jars: list[Path]) -> int:
    server_text = server_log.read_text(encoding="utf-8", errors="replace")
    client_text = client_log.read_text(encoding="utf-8", errors="replace")
    server_stats = last_summary(server_text, "server-stopped")
    client_stats = last_summary(client_text, "client-disconnect")
    problems: list[str] = []

    def net(stats, channel, direction, field):
        return stats["net"].get((channel, direction), {}).get(field)

    for who, stats in (("server", server_stats), ("client", client_stats)):
        if len(stats["init"]) != 1:
            problems.append(f"{who} init 行数 {len(stats['init'])}（应为 1）：{stats['init']}")
        else:
            fields = dict(re.findall(r"(\w+)=([^\s]+)", stats["init"][0]))
            if fields.get("init") != "1" or fields.get("installs") != "1":
                problems.append(f"{who} init/installs 不是 1：{stats['init'][0]}")
        module = stats["modules"].get("orients_hotpot_chronicles", {})
        if module.get("register") != "1" or module.get("runtime") != "1":
            problems.append(f"{who} 模块计数异常：{module}")
    for who, stats in (("服务端", server_stats), ("客户端", client_stats)):
        for channel, direction in (("orients_hotpot_chronicles:config", "S2C"),
                                   ("orients_hotpot_chronicles:chopsticks_aim", "C2S"),
                                   ("orients_hotpot_chronicles:open_sauce_restock", "C2S")):
            fields = stats["net"].get((channel, direction))
            if fields is None:
                problems.append(f"{who}摘要缺少通道 {channel}/{direction}")
            elif fields.get("registered") != "1":
                problems.append(f"{who}通道 {channel}/{direction} registered={fields.get('registered')}（应为 1）")
    if client_stats["modules"].get("orients_hotpot_chronicles", {}).get("client") != "1":
        problems.append("客户端模块 client 阶段不是 1")
    checks = [
        ("服务端 config S2C sent=1", net(server_stats, "orients_hotpot_chronicles:config", "S2C", "sent"), "1"),
        ("服务端 aim C2S handled=1", net(server_stats, "orients_hotpot_chronicles:chopsticks_aim", "C2S", "handled"), "1"),
        ("客户端 config S2C handled=1", net(client_stats, "orients_hotpot_chronicles:config", "S2C", "handled"), "1"),
        ("客户端 aim C2S sent=1", net(client_stats, "orients_hotpot_chronicles:chopsticks_aim", "C2S", "sent"), "1"),
    ]
    for label, actual, wanted in checks:
        if actual != wanted:
            problems.append(f"{label} = {actual}（应为 {wanted}）")
    if "duplicate channel registration ignored" in server_text + client_text:
        problems.append("出现重复通道登记告警")
    if re.search(r"\[(?:[^\]]*/)?(?:ERROR|FATAL)\]", client_text):
        match = re.search(r".*\[(?:[^\]]*/)?(?:ERROR|FATAL)\].*", client_text)
        problems.append("客户端日志出现错误行：" + (match.group(0)[:160] if match else ""))
    if "[HARNESS] aim" not in client_text:
        problems.append("助爪没有发出瞄准包（见客户端日志）")

    report = {
        "loader": "neoforge",
        "status": "pass" if not problems else "fail",
        "problems": problems,
        "server": {"init": server_stats["init"], "modules": server_stats["modules"],
                   "net": {f"{k[0]}/{k[1]}": v for k, v in server_stats["net"].items()}},
        "client": {"init": client_stats["init"], "modules": client_stats["modules"],
                   "net": {f"{k[0]}/{k[1]}": v for k, v in client_stats["net"].items()}},
        "mods": [{"name": jar.name, "sha256": sha256(CLIENT_DIR / "mods" / jar.name)} for jar in jars],
        "logs": [str(server_log.relative_to(ROOT)), str(client_log.relative_to(ROOT))],
    }
    (LOGS / "prod-neo-client.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not problems else 1




if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())