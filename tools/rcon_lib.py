"""进程内 RCON 调用（复用 tools/rcon.py 的协议实现，避免子进程句柄问题）。作者：mryt233

用法：
    from tools import rcon_lib
    rcon_lib.execute(Path(".../server.properties"), "list")
"""
from __future__ import annotations

import socket
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
if str(TOOLS) not in sys.path:
    sys.path.insert(0, str(TOOLS))
import rcon as _rcon  # noqa: E402


def execute(properties: Path, *commands: str, timeout: float = 10) -> list[str]:
    """按顺序执行命令并返回回执；连接/认证失败抛 OSError / _rcon.RconError。"""
    props = _rcon.read_properties(properties)
    if props.get("enable-rcon", "false").lower() != "true":
        raise _rcon.RconError("所选服务端未开启 RCON")
    password = props.get("rcon.password", "")
    port = int(props.get("rcon.port", "25575"))
    results: list[str] = []
    with socket.create_connection(("127.0.0.1", port), timeout=timeout) as sock:
        sock.settimeout(timeout)
        client = _rcon.Rcon(sock)
        client.authenticate(password)
        for command in commands:
            if command.strip().lstrip("/").lower() == "stop":
                client.send(2, command)
                results.append("sent stop")
                break
            results.append(client.command(command))
    return results


def available(properties: Path) -> bool:
    try:
        execute(properties, "list")
        return True
    except (OSError, ValueError, _rcon.RconError):
        return False
