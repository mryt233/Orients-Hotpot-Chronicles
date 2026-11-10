"""生成工具、父模型与打包验收的离线回归。作者：mryt233"""
import json
import os
from pathlib import Path
import sys
import tempfile
import hashlib
import io
import ast
import subprocess
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import project_env
from model_bake import ModelResolver, compare
from pixkit.io import write_legacy_json
from pixkit.materials import ids
import check_content
from compact_json import compact_json
from check_generated import check_manifest
from check_final_jar import resource_problems, check_final_jar
import zipfile
import build_candidates as packaging


class EnvironmentTests(unittest.TestCase):
    def test_jdk_precedence_never_uses_path(self):
        for env, expected in (({}, project_env.DEFAULT_JDK21), ({"JAVA_HOME": "B"}, "B"),
                              ({"JAVA_HOME": "B", "HOTPOT_JDK21": "A"}, "A")):
            with patch.dict(os.environ, env, clear=True):
                self.assertEqual(project_env.jdk21(), Path(expected))

    def test_versions_follow_sources(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "gradle.properties").write_text("mod_version=2.3.4-r9\n", encoding="utf-8")
            api = root / "framework/loader/src/main/java/cn/blockforge/orient/framework/loader/ApiVersion.java"
            api.parent.mkdir(parents=True)
            api.write_text('public static final String RUNTIME = "0.5.2";', encoding="utf-8")
            self.assertEqual(project_env.mod_version(root), "2.3.4-r9")
            self.assertEqual(project_env.runtime_version(root), "0.5.2")
            with self.assertRaises(ValueError):
                project_env.property_version("absent", root)

    def test_material_order_detects_java_drift(self):
        code = 'TABLE_WOODS = java.util.List.of(' + ','.join(json.dumps(w) for w in ids("woods")) + ')'
        self.assertEqual(check_content.check_materials(code), [])
        self.assertTrue(check_content.check_materials(code.replace('"oak"', '"absent"')))

    def test_legacy_writer_preserves_bytes_and_newline(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder) / "a.json"
            expected = b'{\r\n  "x": 1\r\n}\r\n'
            p.write_bytes(expected)
            write_legacy_json(p, {"x": 1})
            self.assertEqual(p.read_bytes(), expected)


class ModelTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.base = self.root / "assets/test/models"
        self.base.mkdir(parents=True)
        self.resolver = ModelResolver(self.root)

    def tearDown(self):
        self.tmp.cleanup()

    def model(self, name, data):
        (self.base / (name + ".json")).write_text(json.dumps(data), encoding="utf-8")

    def test_parent_alias_resolves_against_child_overrides(self):
        self.model("base", {"textures": {"particle": "#base", "alias": "#top"}, "elements": [
            {"from": [0, 0, 0], "to": [1, 1, 1], "faces": {"up": {"texture": "#alias"}}}],
            "ambientocclusion": False, "display": {"gui": {"scale": [1, 1, 1]}}})
        self.model("child", {"parent": "test:base", "textures": {"top": "test:block/a", "base": "test:block/b"}})
        baked = self.resolver.bake("test:child")
        self.assertEqual(baked["elements"][0]["faces"]["up"]["texture"], "test:block/a")
        self.assertEqual(baked["particle"], "test:block/b")
        self.assertFalse(baked["ambientocclusion"])
        self.assertIn("gui", baked["display"])

    def test_parent_and_variable_cycles_fail(self):
        self.model("a", {"parent": "test:b"})
        self.model("b", {"parent": "test:a"})
        with self.assertRaisesRegex(ValueError, "parent 循环"):
            self.resolver.bake("test:a")
        with self.assertRaisesRegex(ValueError, "变量循环"):
            self.resolver.texture("#a", {"a": "#b", "b": "#a"})
        with self.assertRaisesRegex(ValueError, "未定义"):
            self.resolver.texture("#a", {})

    def test_empty_elements_inherit_and_display_merges_per_pose(self):
        self.model("base", {"elements": [{"from": [0, 0, 0], "to": [1, 1, 1]}],
                            "display": {"gui": {"scale": [2, 2, 2]}, "ground": {"scale": [1, 1, 1]}}})
        self.model("child", {"parent": "test:base", "elements": [], "display": {"gui": {"scale": [3, 3, 3]}}})
        baked = self.resolver.bake("test:child")
        self.assertEqual(len(baked["elements"]), 1)
        self.assertEqual(baked["display"]["gui"]["scale"], [3, 3, 3])
        self.assertIn("ground", baked["display"])

    def test_compare_detects_particle_changes_in_all_original_models(self):
        ns = "orients_hotpot_chronicles"
        before, after = self.root / "before", self.root / "after"
        for root, particle in ((before, "test:a"), (after, "test:b")):
            file = root / f"assets/{ns}/models/block/a.json"
            file.parent.mkdir(parents=True)
            file.write_text(json.dumps({"textures": {"particle": particle}}), encoding="utf-8")
        result = compare(before, after)
        self.assertEqual(result["models"], 1)
        self.assertEqual(len(result["differences"]), 1)


class AcceptanceTests(unittest.TestCase):
    def test_bad_json_does_not_destroy_an_existing_jar(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(packaging, "COMPACT_JSON", True):
            p = Path(folder) / "existing.jar"
            p.write_bytes(b"keep existing")
            with self.assertRaisesRegex(ValueError, "打包 JSON 解析失败"):
                packaging.write_jar(p, {"assets/a.json": b"{broken}"})
            self.assertEqual(p.read_bytes(), b"keep existing")

    def test_trial_package_keeps_archive_directories_untouched(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            candidate, trial = root / "candidate", root / "trial"
            version = project_env.mod_version()
            for loader in ("fabric", "neoforge"):
                packaging.write_jar(candidate / f"orient_framework-{loader}-{packaging.RUNTIME_VERSION}.jar", {"runtime.txt": b"runtime"})
                metadata = "fabric.mod.json" if loader == "fabric" else "META-INF/neoforge.mods.toml"
                packaging.write_jar(candidate / f"orients_hotpot_chronicles-{loader}-{version}.jar", {metadata: b"{}" if loader == "fabric" else b'version="test"'})
            with patch.multiple(packaging, OUT=candidate, TRIAL_DIR=trial, DEV_DIR=root/"forbidden-dev", PER_DIR=root/"forbidden-per"), patch.object(packaging, "build"):
                result = packaging.build_release()
            self.assertEqual(set(result), {"trial-fabric", "trial-neoforge"})
            self.assertFalse((root / "forbidden-dev").exists())
            self.assertFalse((root / "forbidden-per").exists())

    def test_language_default_is_preserved_and_out_is_read_only_for_tutorial(self):
        from unittest.mock import Mock
        source = Path(__file__).resolve().parents[1] / "gen_lang.py"
        node = ast.parse(source.read_text(encoding="utf-8")).body[-1]
        code = compile(ast.Module(body=[node], type_ignores=[]), str(source), "exec")
        for args, expected in (([], 1), (["--no-tutorial"], 0), (["--out", "isolated"], 0)):
            main, tutorial = Mock(), Mock()
            with patch.object(sys, "argv", ["gen_lang.py", *args]):
                exec(code, {"__name__": "__main__", "sys": sys, "main": main, "sync_r146e_tutorial_placeholders": tutorial})
            main.assert_called_once_with()
            self.assertEqual(tutorial.call_count, expected)

    def test_generator_records_unchanged_outputs_after_success(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            target = root / "common/src/main/resources/a.json"
            target.parent.mkdir(parents=True)
            target.write_bytes(b"same")
            tools = str(Path(__file__).resolve().parents[1])
            script = root / "capture.py"
            script.write_text("import sys\nfrom pathlib import Path\nsys.path.insert(0," + repr(tools) + ")\nfrom pixkit.manifest import start_session\nsys.argv[0]='gen_fixture.py'\nroot=Path(" + repr(str(root)) + ")\nstart_session(root,root/'common/src/main/resources')\n(root/'common/src/main/resources/a.json').write_bytes(b'same')\n", encoding="utf-8")
            result = subprocess.run([sys.executable, str(script)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            manifest = json.loads((root / "content/generated_manifest.json").read_text(encoding="utf-8"))
            digest = manifest["generators"]["tools/gen_fixture.py"]["files"]["common/src/main/resources/a.json"]
            self.assertEqual(digest, hashlib.sha256(b"same").hexdigest())

    def test_unused_warnings_do_not_count_language_definitions_as_usage(self):
        import check_unused
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            assets = root / "common/src/main/resources/assets/orients_hotpot_chronicles"
            (assets / "lang").mkdir(parents=True)
            (assets / "models").mkdir()
            (assets / "models/orphan.json").write_text("{}", encoding="utf-8")
            (assets / "lang/en_us.json").write_text('{"text.orients_hotpot_chronicles.unused":"unused"}', encoding="utf-8")
            (root / "tools").mkdir()
            (root / "tools/content_usage_allowlist.json").write_text('{"models":{},"model_prefixes":{},"language_prefixes":{}}', encoding="utf-8")
            with patch.multiple(check_unused, MOD=root, RES=assets.parents[1], ASSETS=assets), patch.object(check_unused, "registered_ids", return_value={"item": set(), "block": set()}):
                ok, lines = check_unused.check_unused()
            self.assertTrue(ok)
            self.assertTrue(any("未引用语言键：text.orients_hotpot_chronicles.unused" in line for line in lines))
            self.assertTrue(any("未引用模型：orphan" in line for line in lines))

    def test_compaction_preserves_tokens_and_order(self):
        data = b'{ "z": 1e-012, "a": 123456789012345678901234567890, "s": "a \\t \\" b", "zero": -0.0 }'
        compact = compact_json(data)
        self.assertEqual(json.loads(data), json.loads(compact))
        self.assertIn(b'1e-012', compact)
        self.assertIn(b'123456789012345678901234567890', compact)
        self.assertIn(b'"zero":-0.0', compact)
        self.assertLess(compact.index(b'"z"'), compact.index(b'"a"'))
        for invalid in (b'{bad}', b'{"x":NaN}', b'{"x":Infinity}'):
            with self.assertRaises(ValueError):
                compact_json(invalid)

    def test_manifest_tampering_and_duplicate_owners(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "a").write_bytes(b"original")
            digest = hashlib.sha256(b"original").hexdigest()
            manifest = {"generators": {"one": {"files": {"a": digest}}}, "merges": {}}
            self.assertTrue(check_manifest(root, manifest)[0])
            (root / "a").write_bytes(b"changed")
            self.assertFalse(check_manifest(root, manifest)[0])
            (root / "a").write_bytes(b"original")
            manifest["generators"]["two"] = {"files": {"a": digest}}
            self.assertFalse(check_manifest(root, manifest)[0])
            manifest["merges"]["a"] = {"order": ["one", "two"], "sha256": digest, "reason": "追加标签"}
            self.assertTrue(check_manifest(root, manifest)[0])

    def test_final_resource_chain_and_required_registry_ids(self):
        ns = "orients_hotpot_chronicles"
        entries = {
            f"assets/{ns}/models/block/base.json": json.dumps({"textures": {"side": ns + ":block/a"}, "elements": [{"faces": {"up": {"texture": "#side"}}}]}).encode(),
            f"assets/{ns}/models/item/a.json": json.dumps({"parent": ns + ":block/base"}).encode(),
            f"assets/{ns}/textures/block/a.png": b"texture",
            f"data/{ns}/tags/item/required.json": json.dumps({"values": [ns + ":a"]}).encode(),
            f"data/{ns}/recipe/a.json": json.dumps({"result": {"id": ns + ":a"}}).encode(),
        }
        registry = {"item": {"a"}, "block": set()}
        self.assertEqual(resource_problems(entries, registry), [])
        missing = dict(entries)
        del missing[f"assets/{ns}/textures/block/a.png"]
        self.assertTrue(any("缺贴图" in e for e in resource_problems(missing, registry)))
        missing = dict(entries)
        del missing[f"assets/{ns}/models/block/base.json"]
        self.assertTrue(any("缺模型" in e for e in resource_problems(missing, registry)))
        errors = resource_problems(entries, {"item": set(), "block": set()})
        self.assertTrue(any("必选" in e for e in errors))
        self.assertTrue(any("配方产物" in e for e in errors))

    def test_nested_runtime_and_forbidden_classes(self):
        def archive(entries):
            out = io.BytesIO()
            with zipfile.ZipFile(out, "w") as jar:
                for name, data in entries.items():
                    jar.writestr(name, data)
            return out.getvalue()
        ns = "orients_hotpot_chronicles"
        version = project_env.runtime_version()
        nested = "META-INF/jars/runtime.jar"
        outer = {"fabric.mod.json": json.dumps({"id": ns, "version": project_env.mod_version(),
                  "depends": {"orient_framework": ">=" + version + " <0.2.0"}, "jars": [{"file": nested}]}).encode(),
                 nested: archive({"fabric.mod.json": json.dumps({"id": "orient_framework", "version": version})})}
        with tempfile.TemporaryDirectory() as folder, patch("check_final_jar.resource_problems", return_value=[]):
            p = Path(folder) / "a.jar"
            p.write_bytes(archive(outer))
            self.assertTrue(check_final_jar(p)[0])
            dirty = dict(outer)
            dirty["mezz/jei/api/Stub.class"] = b"bad"
            p.write_bytes(archive(dirty))
            self.assertFalse(check_final_jar(p)[0])
            dirty = dict(outer)
            dirty[nested] = archive({"fabric.mod.json": json.dumps({"id": "orient_framework", "version": "0.0.0"})})
            p.write_bytes(archive(dirty))
            self.assertFalse(check_final_jar(p)[0])


if __name__ == "__main__":
    unittest.main()
