"""内嵌打包与开发版归档离线回归。作者：mryt233。"""
import contextlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build_candidates as packaging


class PackagingTests(unittest.TestCase):
    def test_default_and_release_embed_once(self):
        for args in ([], ["--release"], ["--compile"], ["--compile", "--release"]):
            with self.subTest(args=args), patch.object(sys, "argv", ["build_candidates.py", *args]), \
                    patch.object(packaging, "compile_sources") as compile_sources, \
                    patch.object(packaging, "build_release") as build_release, \
                    patch.object(packaging, "build") as build:
                self.assertEqual(packaging.main(), 0)
                build_release.assert_called_once_with()
                build.assert_not_called()
                self.assertEqual(compile_sources.call_count, int("--compile" in args))

    def test_single_jars_metadata_and_archive_buckets(self):
        for version, bucket in (("1.0.0-r141", "r100-r149"),
                                ("1.0.0-r149", "r100-r149"),
                                ("1.0.0-r150-hotfix1", "r150-r199"),
                                ("0.2.2-alpha", None)):
            with self.subTest(version=version), tempfile.TemporaryDirectory() as folder:
                root = Path(folder)
                out = root / "candidate"
                release = root / "releases"
                dev = root / "dev/1.21.1"
                per = root / "per/1.21.1"
                for loader in ("fabric", "neoforge"):
                    runtime = out / f"orient_framework-{loader}-{packaging.RUNTIME_VERSION}.jar"
                    packaging.write_jar(runtime, {"runtime.txt": b"framework"})
                    meta = ("fabric.mod.json" if loader == "fabric" else "META-INF/neoforge.mods.toml")
                    data = (json.dumps({"version": version, "entrypoints": {"modmenu": ["test.Menu"]}}).encode()
                            if loader == "fabric" else f'version="{version}"'.encode())
                    packaging.write_jar(out / f"orients_hotpot_chronicles-{loader}-{version}.jar",
                                        {meta: data, "content.txt": b"hotpot"})
                history = dev / "fabric/mod/r100-r149/old.jar"
                history.parent.mkdir(parents=True)
                history.write_bytes(b"keep history")
                with patch.multiple(packaging, ROOT=root, OUT=out, RELEASE_DIR=release, DEV_DIR=dev, PER_DIR=per), \
                        patch.object(packaging, "mod_version", return_value=version), \
                        patch.object(packaging, "build") as build, contextlib.redirect_stdout(io.StringIO()):
                    results = packaging.build_release()
                build.assert_called_once_with()
                # mryt233 改（2026-11-09）：开发版保留四份归档，预览版只输出双端单 JAR。
                self.assertEqual(len(results), 4 if bucket else 2)
                self.assertEqual(history.read_bytes(), b"keep history")
                for loader in ("fabric", "neoforge"):
                    expected_name = f"orients_hotpot_chronicles-1.21.1-{loader}-{version}.jar"
                    full = results[f"dev-{loader}" if bucket else f"per-{loader}"]
                    self.assertEqual(full.name, expected_name)
                    runtime = out / f"orient_framework-{loader}-{packaging.RUNTIME_VERSION}.jar"
                    with zipfile.ZipFile(full) as jar:
                        nested = f"META-INF/jars/{runtime.name}"
                        self.assertEqual(jar.read(nested), runtime.read_bytes())
                        self.assertEqual(jar.read("content.txt"), b"hotpot")
                        if loader == "fabric":
                            meta = json.loads(jar.read("fabric.mod.json"))
                            self.assertEqual(meta["version"], version)
                            self.assertEqual(meta["jars"], [{"file": nested}])
                            self.assertEqual(meta["entrypoints"]["modmenu"], ["test.Menu"])
                        else:
                            meta = json.loads(jar.read("META-INF/jarjar/metadata.json"))["jars"][0]
                            self.assertEqual(meta["path"], nested)
                            self.assertEqual(meta["version"]["artifactVersion"], packaging.RUNTIME_VERSION)
                            self.assertEqual(meta["version"]["range"], packaging.RUNTIME_RANGE)
                    if bucket:
                        archived = dev / loader / "mod" / bucket / expected_name
                        self.assertEqual(archived.read_bytes(), full.read_bytes())
                        self.assertFalse((dev / loader / "mod" / bucket / f"orients_hotpot_chronicles-{loader}-{version}.jar").exists())
                        self.assertFalse(release.exists())
                        self.assertEqual((dev / loader / "framework" / bucket / runtime.name).read_bytes(),
                                         runtime.read_bytes())
                    else:
                        self.assertEqual(list(dev.rglob("*.jar")), [history])
                        self.assertEqual((per / version / expected_name).read_bytes(), full.read_bytes())
                        self.assertFalse(release.exists())

    def test_missing_candidates_fail_before_single_jar(self):
        with tempfile.TemporaryDirectory() as folder, \
                patch.object(packaging, "OUT", Path(folder)), \
                patch.object(packaging, "build"), \
                patch.object(packaging, "mod_version", return_value="1.0.0-r141"):
            with self.assertRaisesRegex(SystemExit, "缺少候选"):
                packaging.build_release()

    def test_list_remains_read_only(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(packaging, "OUT", Path(folder)), \
                patch.object(sys, "argv", ["build_candidates.py", "list", "--compile"]), \
                patch.object(packaging, "compile_sources") as compile_sources, \
                patch.object(packaging, "build_release") as build_release:
            self.assertEqual(packaging.main(), 0)
            compile_sources.assert_not_called()
            build_release.assert_not_called()


if __name__ == "__main__":
    unittest.main()
