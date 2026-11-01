"""开发服 RCON 客户端；密码只从指定运行目录读取，不回显。作者：mryt233

python tools/rcon.py --run-dir fabric/run-server "list"
python tools/rcon.py --properties neoforge/run-server/server.properties "stop"
相对路径以 mod/ 为根；默认选择 Fabric 开发服，不回退到旧 run/。
"""
from __future__ import annotations

import argparse
from pathlib import Path
import socket
import struct
import sys

ROOT = Path(__file__).resolve().parent.parent
MAX_PACKET = 4 * 1024 * 1024


class RconError(Exception):
    """不包含凭据的协议错误。"""


def read_properties(path: Path) -> dict[str, str]:
    props = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, sep, value = line.partition("=")
        if sep:
            props[key.strip()] = value.strip()
    return props


class Rcon:
    def __init__(self, sock: socket.socket):
        self.sock = sock
        self.request_id = 0

    def read_exact(self, size: int) -> bytes:
        parts = bytearray()
        while len(parts) < size:
            data = self.sock.recv(size - len(parts))
            if not data:
                raise RconError("RCON 连接在收齐响应前关闭")
            parts.extend(data)
        return bytes(parts)

    def packet(self) -> tuple[int, int, str]:
        size = struct.unpack("<i", self.read_exact(4))[0]
        if not 10 <= size <= MAX_PACKET:
            raise RconError("RCON 响应长度无效")
        data = self.read_exact(size)
        if data[-2:] != b"\0\0":
            raise RconError("RCON 响应缺少结束标记")
        request_id, kind = struct.unpack("<ii", data[:8])
        return request_id, kind, data[8:-2].decode("utf-8", "replace")

    def send(self, kind: int, text: str) -> int:
        self.request_id += 1
        data = struct.pack("<ii", self.request_id, kind) + text.encode("utf-8") + b"\0\0"
        if len(data) + 4 > 1460:
            raise RconError("RCON 请求超过原版服务端单包上限（1460 字节），请拆分命令")
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        return self.request_id

    def authenticate(self, password: str) -> None:
        request_id = self.send(3, password)
        # 有的实现先发一个空 RESPONSE_VALUE，再发 AUTH_RESPONSE。
        for _ in range(2):
            got_id, kind, _ = self.packet()
            if got_id == -1:
                raise RconError("RCON 认证失败，请核对所选开发服配置")
            if got_id != request_id:
                raise RconError("RCON 认证响应编号不匹配")
            if kind == 2:
                return
            if kind != 0:
                break
        raise RconError("RCON 未返回认证成功响应")

    def command(self, text: str) -> str:
        request_id = self.send(2, text)
        # 原版 RconClient 每次 read 只接受一条请求。先收首个响应再发屏障，
        # 否则 TCP 合并两条请求时服务端会直接断开连接。
        got_id, kind, body = self.packet()
        if got_id != request_id or kind != 0:
            raise RconError("RCON 命令响应无效")
        parts = [body]
        marker = self.send(2, "")
        while True:
            got_id, kind, body = self.packet()
            if kind != 0:
                raise RconError("RCON 命令响应类型无效")
            if got_id == marker:
                return "".join(parts)
            if got_id != request_id:
                raise RconError("RCON 命令响应编号不匹配")
            parts.append(body)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    paths = parser.add_mutually_exclusive_group()
    paths.add_argument("--run-dir", type=Path, default=Path("fabric/run-server"))
    paths.add_argument("--properties", type=Path)
    parser.add_argument("commands", nargs="+")
    args = parser.parse_args(argv)
    path = args.properties or args.run_dir / "server.properties"
    if not path.is_absolute():
        path = ROOT / path
    try:
        props = read_properties(path)
        if props.get("enable-rcon", "false").lower() != "true":
            raise RconError("所选开发服未开启 RCON")
        password = props.get("rcon.password", "")
        if not password:
            raise RconError("所选开发服未设置 RCON 密码")
        port = int(props.get("rcon.port", "25575"))
        if not 1 <= port <= 65535:
            raise RconError("RCON 端口超出范围")
        with socket.create_connection(("127.0.0.1", port), timeout=10) as sock:
            client = Rcon(sock)
            client.authenticate(password)
            for command in args.commands:
                print(">", command)
                if command.strip().lstrip("/").lower() == "stop":
                    client.send(2, command)
                    print("已发送正常关服命令；请以服务端停止日志确认。")
                    break
                print(client.command(command))
        return 0
    except (OSError, ValueError, RconError) as exc:
        # 不输出 socket 请求内容或整份 properties，避免泄露认证信息。
        print(f"RCON 失败：{exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
