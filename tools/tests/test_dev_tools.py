"""开发工具离线回归，不启动 Minecraft。作者：mryt233。"""
import importlib.util
import os
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import check_all
import rcon
import sync_shared


def packet(i, kind, text=""):
    body = struct.pack("<ii", i, kind) + text.encode() + b"\0\0"
    return struct.pack("<i", len(body)) + body


class Socket:
    def __init__(self, data):
        self.data = data
        self.sent = []

    def recv(self, size):
        chunk, self.data = self.data[:min(size, 3)], self.data[min(size, 3):]
        return chunk

    def sendall(self, data):
        self.sent.append(data)


class RconTests(unittest.TestCase):
    def test_fragmented_response_and_command_barrier(self):
        sock = Socket(packet(1, 2) + packet(2, 0, "中文") + packet(2, 0, "结果") + packet(3, 0))
        client = rcon.Rcon(sock)
        client.authenticate("local-test-only")
        self.assertEqual(client.command("list"), "中文结果")

    def test_marker_waits_for_command_response(self):
        class SequentialSocket(Socket):
            def recv(self, size):
                if not self.data:
                    return b""
                # 首包未读完前绝不能发送第二条请求。
                if len(self.data) > len(packet(2, 0)):
                    self.assertion()
                return super().recv(size)

        sock = SequentialSocket(packet(1, 0, "ok") + packet(2, 0))
        sock.assertion = lambda: self.assertEqual(len(sock.sent), 1)
        self.assertEqual(rcon.Rcon(sock).command("list"), "ok")

    def test_oversize_request_refused(self):
        with self.assertRaisesRegex(rcon.RconError, "单包上限"):
            rcon.Rcon(Socket(b"")).send(2, "x" * 1500)

    def test_auth_failure(self):
        with self.assertRaisesRegex(rcon.RconError, "认证失败"):
            rcon.Rcon(Socket(packet(-1, 2))).authenticate("local-test-only")

    def test_optional_empty_auth_response(self):
        rcon.Rcon(Socket(packet(1, 0) + packet(1, 2))).authenticate("local-test-only")

    def test_eof(self):
        with self.assertRaises(rcon.RconError):
            rcon.Rcon(Socket(b"\x01")).packet()

    def test_bad_length(self):
        with self.assertRaises(rcon.RconError):
            rcon.Rcon(Socket(struct.pack("<i", -1))).packet()

    def test_properties(self):
        with tempfile.TemporaryDirectory() as tmp:
            file = Path(tmp) / "server.properties"
            file.write_bytes(b"# test\nenable-rcon=true\nrcon.port=25576\nrcon.password=a=b\n")
            self.assertEqual(rcon.read_properties(file)["rcon.password"], "a=b")


class ArtifactTests(unittest.TestCase):
    def test_absent_is_not_pass(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(check_all, "ROOT", Path(tmp)):
            self.assertIsNone(check_all.check_jars()[0])

    def test_stale_is_not_pass(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(check_all, "ROOT", Path(tmp)):
            root = Path(tmp)
            libs = root / "fabric/build/libs"
            libs.mkdir(parents=True)
            jar = libs / "mod.jar"
            jar.write_bytes(b"old")
            os.utime(jar, (1, 1))
            (root / "build.gradle").write_text("new")
            self.assertIsNone(check_all.check_jars()[0])

    def test_wrong_metadata_and_devtest(self):
        with tempfile.TemporaryDirectory() as tmp:
            jar = Path(tmp) / "fixture.zip"
            with zipfile.ZipFile(jar, "w") as archive:
                for n in range(50):
                    archive.writestr(f"test/hotpot/Core{n}.class", b"")
                archive.writestr("fabric.mod.json", "{}")
                archive.writestr("META-INF/neoforge.mods.toml", "")
                archive.writestr("test/devtest/Test.class", b"")
            self.assertEqual(len(check_all.inspect_jar(jar, "fabric.mod.json", "META-INF/neoforge.mods.toml")), 2)


class SyncTests(unittest.TestCase):
    def test_old_layout_refused_before_write(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertIn("旧布局", sync_shared.validate_target(Path(tmp)))
            self.assertEqual(list(Path(tmp).iterdir()), [])

    def test_unknown_mapping_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for folder in ("common/src/main/java", "common/src/main/resources", "fabric/src/main/java"):
                (root / folder).mkdir(parents=True)
            self.assertIn("Mojang", sync_shared.validate_target(root))


if __name__ == "__main__":
    unittest.main()
