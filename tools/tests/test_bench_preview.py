"""条凳预览、Java 参数和游戏资源隔离回归。作者：mryt233"""
import copy
import io
import json
import math
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from bench_preview import JAVA_PACKAGE, VARIANTS, complete_model, java_parameters, leg_elements, preview_root
from model_bake import ModelResolver
from pixkit.paths import MOD, MODID, SOURCE_RES
from check_final_jar import check_final_jar
from check_bench_preview import check_bench_preview
from project_env import mod_version, runtime_version


class BenchPreviewTests(unittest.TestCase):
    def test_checker_detects_java_drift_against_generated_files(self):
        self.assertTrue(check_bench_preview()[0])
        changed = java_parameters(); changed["LEG_SPREAD"] += 0.25
        with patch("check_bench_preview.java_parameters", return_value=changed):
            ok, lines = check_bench_preview()
        self.assertFalse(ok)
        self.assertTrue(any("不一致" in line for line in lines))

    def test_java_parameters_fail_closed_and_follow_changes(self):
        actual = java_parameters()
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for sub in (f"main/java/{JAVA_PACKAGE}/hotpot/BenchBlock.java",
                        f"client/java/{JAVA_PACKAGE}/client/BenchLegsRenderer.java"):
                file = root / "common/src" / sub
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text((MOD / "common/src" / sub).read_text(encoding="utf-8"), encoding="utf-8")
            self.assertEqual(java_parameters(root), actual)
            file.write_text(file.read_text(encoding="utf-8").replace("LEG_T = 1.5F", "LEG_T = 1.75F"), encoding="utf-8")
            changed = java_parameters(root)
            self.assertEqual(changed["LEG_T"], 1.75)
            self.assertNotEqual(leg_elements(False, False, actual), leg_elements(False, False, changed))
            file.write_text(file.read_text(encoding="utf-8").replace("LEG_T = 1.75F", "LEG_T = compute()"), encoding="utf-8")
            with self.assertRaises(ValueError): java_parameters(root)

    def test_leg_vertices_uv_and_connection_states_match_renderer(self):
        p = java_parameters()
        for flags, expected_x in { (False, False): [1.5, 12.5], (False, True): [4.5],
                                  (True, False): [9.5], (True, True): [] }.items():
            legs = leg_elements(*flags, p)
            self.assertEqual(len(legs), len(expected_x) * 2)
            for i, leg in enumerate(legs):
                x = expected_x[i // 2]; z = (6.5, 9.5)[i % 2]; sign = (1, -1)[i % 2]
                origin = leg["rotation"]["origin"]
                self.assertEqual(origin, [x, p["LEG_TOP"], z])
                angle = math.radians(leg["rotation"]["angle"])
                dy = leg["from"][1] - p["LEG_TOP"]
                self.assertAlmostEqual(dy * math.cos(angle) + p["LEG_TOP"], 0)
                self.assertAlmostEqual(dy * math.sin(angle) + z, z - sign * p["LEG_SPREAD"])
                self.assertEqual(leg["to"][0] - leg["from"][0], p["LEG_W"])
                self.assertEqual(leg["to"][2] - leg["from"][2], p["LEG_T"])
                for face, uv in leg["faces"].items():
                    w = p["LEG_T"] if face in ("east", "west") else p["LEG_W"]
                    h = p["LEG_T"] if face in ("up", "down") else math.hypot(p["LEG_TOP"], p["LEG_SPREAD"])
                    self.assertEqual(uv["uv"], [0, 0, w, h])

    def test_blockstate_model_mapping_and_facing(self):
        states = json.loads((SOURCE_RES / f"assets/{MODID}/blockstates/oak_bench.json").read_text())["variants"]
        for flags, tail in VARIANTS.items():
            left, right = (str(flag).lower() for flag in flags)
            for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                entry = states[f"facing={facing},left={left},right={right}"]
                self.assertEqual(entry["model"], f"{MODID}:block/oak_bench{tail}")
                self.assertEqual(entry.get("y", 0), angle)

    def test_explanation_cannot_change_baked_model(self):
        with tempfile.TemporaryDirectory() as folder:
            resources = Path(folder)
            model = resources / "assets/test/models/bench.json"; model.parent.mkdir(parents=True)
            data = {"textures": {"particle": "minecraft:block/oak_planks"}, "elements": [
                {"from": [0, 14, 5], "to": [16, 16, 11], "faces": {"up": {"texture": "minecraft:block/oak_planks"}}}],
                "display": {"gui": {"scale": [0.625] * 3}}}
            model.write_text(json.dumps(data))
            before = ModelResolver(resources).bake("test:bench")
            data["_说明"] = "作者：mryt233；完整造型见 preview_models/"
            model.write_text(json.dumps(data))
            self.assertEqual(before, ModelResolver(resources).bake("test:bench"))

    def test_preview_is_sibling_of_isolated_resources(self):
        self.assertEqual(preview_root(SOURCE_RES), MOD / "preview_models")
        isolated = MOD / "build/bench_preview/test-resources"
        self.assertFalse(preview_root(isolated).is_relative_to(isolated))
        self.assertFalse(preview_root(SOURCE_RES).is_relative_to(SOURCE_RES))

    def test_jar_rejects_preview_json_in_outer_and_nested_jar(self):
        def archive(entries):
            out = io.BytesIO()
            with zipfile.ZipFile(out, "w") as jar:
                for name, value in entries.items(): jar.writestr(name, value)
            return out.getvalue()
        version = runtime_version(); nested = "META-INF/jars/runtime.jar"
        runtime = {"fabric.mod.json": json.dumps({"id": "orient_framework", "version": version})}
        outer = {"fabric.mod.json": json.dumps({"id": MODID, "version": mod_version(),
                 "depends": {"orient_framework": ">=" + version + " <0.2.0"}, "jars": [{"file": nested}]}),
                 nested: archive(runtime)}
        with tempfile.TemporaryDirectory() as folder, patch("check_final_jar.resource_problems", return_value=[]):
            jar = Path(folder) / "a.jar"; jar.write_bytes(archive(outer))
            self.assertTrue(check_final_jar(jar)[0])
            for in_nested in (False, True):
                dirty = copy.deepcopy(outer)
                target = dict(runtime) if in_nested else dirty
                target["preview_models/assets/test/models/bench.json"] = "{}"
                if in_nested: dirty[nested] = archive(target)
                jar.write_bytes(archive(dirty))
                self.assertFalse(check_final_jar(jar)[0])


if __name__ == "__main__":
    unittest.main()
