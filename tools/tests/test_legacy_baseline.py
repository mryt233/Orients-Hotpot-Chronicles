"""旧档基线工具（legacy_baseline.py）的离线回归：SNBT 解析、对比、允许差异、夹具、场景表自检。作者：mryt233"""
import contextlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import legacy_baseline as lb


def entry(name, out):
    return {"name": name, "cmd": "data get block 0 0 0", "out": out}


def block(snbt):
    return "1, 2, 3 has the following block data: " + snbt


class SnbtTests(unittest.TestCase):
    SAMPLE = ('{Doneness: [I; 0, 5, 0], Cooked: [B; 0B, 1B], Spicy: 2, PendingXp: 1.5f, '
              'Items: [{count: 1, Slot: 0b, components: {"orients_hotpot_chronicles:sauce": {bites: 9, mask: 32767}}, '
              'id: "orients_hotpot_chronicles:dipping_sauce"}], Base: -1, Plate: "a\\"b"}')

    def test_roundtrip_keeps_types_and_order(self):
        node = lb.parse_snbt(self.SAMPLE)
        self.assertEqual(lb.to_snbt(node), self.SAMPLE)
        self.assertIsInstance(node["Doneness"], lb.Arr)
        self.assertEqual(node["Doneness"].kind, "I")
        self.assertEqual(node["Spicy"], "2")
        self.assertEqual(node["Plate"], 'a"b')

    def test_empty_containers_and_nested_quotes(self):
        node = lb.parse_snbt('{Items: [], Fuel: {Items: []}, Name: \'x\'}')
        self.assertEqual(node["Items"], [])
        self.assertEqual(node["Fuel"], {"Items": []})
        self.assertEqual(node["Name"], "x")

    def test_trailing_garbage_is_an_error(self):
        with self.assertRaises(lb.SnbtError):
            lb.parse_snbt("{a: 1} x")


class CompareTests(unittest.TestCase):
    def dump(self, *entries):
        return {"meta": {}, "entries": list(entries)}

    def test_identical_and_order_only_difference(self):
        old = self.dump(entry("a", block("{x: 1, y: 2}")))
        new = self.dump(entry("a", block("{y: 2, x: 1}")))
        report = lb.compare_dumps(old, new, [])
        self.assertEqual(report["identical"], ["a"])
        self.assertEqual(report["text_differs"], ["a"])   # 内容相同但原文不同，要单独报出来

    def test_changed_added_removed_paths(self):
        old = self.dump(entry("pot", block("{Base: 3, Spicy: 2, Gone: 1}")))
        new = self.dump(entry("pot", block("{SoupId: \"hotpot:spicy\", Spicy: 2, Base: 4}")))
        report = lb.compare_dumps(old, new, [])
        kinds = {(k, p) for k, p, _, _, _ in report["unexpected"][0][1]}
        self.assertEqual(kinds, {("changed", "Base"), ("removed", "Gone"), ("added", "SoupId")})

    def test_allow_rules_match_entry_path_and_kind(self):
        old = self.dump(entry("pot_a", block("{Base: 3}")), entry("chest.slot1", block("{Base: 3}")))
        new = self.dump(entry("pot_a", block("{SoupId: \"x\"}")), entry("chest.slot1", block("{SoupId: \"x\"}")))
        rules = [{"entry": "pot_.*", "path": "Base", "kind": "removed", "reason": "旧键被新键取代"},
                 {"entry": "pot_.*", "path": "SoupId", "kind": "added", "reason": "新键"}]
        report = lb.compare_dumps(old, new, rules)
        self.assertEqual([n for n, _ in report["unexpected"]], ["chest.slot1"])
        self.assertEqual([n for n, _ in report["allowed"]], ["pot_a"])

    def test_missing_and_extra_entries(self):
        old = self.dump(entry("a", block("{x: 1}")), entry("b", block("{x: 1}")))
        new = self.dump(entry("a", block("{x: 1}")), entry("c", block("{x: 1}")))
        report = lb.compare_dumps(old, new, [])
        self.assertEqual(report["missing"], ["b"])
        self.assertEqual(report["extra"], ["c"])

    def test_non_data_replies_compare_as_raw_text(self):
        old = self.dump(entry("a", "The target block is not a block entity"))
        new = self.dump(entry("a", "The target block is not a block entity"))
        self.assertEqual(lb.compare_dumps(old, new, [])["identical"], ["a"])
        changed = self.dump(entry("a", block("{x: 1}")))
        self.assertTrue(lb.compare_dumps(old, changed, [])["unexpected"])


class FixtureTests(unittest.TestCase):
    def test_fixtures_split_item_and_compare_against_dump(self):
        dump = {"meta": {"scenario": 1, "created": "t"}, "entries": [
            entry("chest.slot0", block('{count: 1, Slot: 0b, components: {"m:sauce": {bites: 9, mask: 3}}, id: "m:bowl"}')),
            entry("chest.slot1", "Found no elements matching Items[{Slot:1b}]"),
        ]}
        with tempfile.TemporaryDirectory() as tmp:
            src, dst = Path(tmp) / "dump.json", Path(tmp) / "fixtures.json"
            src.write_text(json.dumps(dump), encoding="utf-8")

            class Args:
                pass
            args = Args()
            args.dump, args.output, args.loader_label = str(src), str(dst), "fabric"
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(lb.cmd_fixtures(args), 0)
            fixtures = json.loads(dst.read_text(encoding="utf-8"))
            item = fixtures["entries"]["chest.slot0"]["item"]
            self.assertEqual(item["id"], "m:bowl")
            self.assertEqual(item["components_snbt"], '{"m:sauce": {bites: 9, mask: 3}}')
            self.assertIn("raw", fixtures["entries"]["chest.slot1"])
            # 夹具文件也能当「标准答案」拿去对比
            loaded = lb.load_dump(dst)
            report = lb.compare_dumps(dump, loaded, [])
            self.assertEqual(len(report["identical"]), 2)
            self.assertFalse(report["unexpected"] or report["missing"] or report["extra"])


class ScenarioTests(unittest.TestCase):
    def test_scenario_is_well_formed(self):
        spots = lb.scenario()
        names = [name for name, _ in lb.all_queries(spots)]
        self.assertEqual(len(names), len(set(names)), "查询名必须唯一")
        for spot in spots:
            for command in spot.build + [c for _, c in spot.queries]:
                self.assertLess(len(command.encode("utf-8")), 1400, f"{spot.name} 的命令超过 RCON 单包上限")

    def test_seasoning_table_matches_mask_width(self):
        self.assertEqual(len(lb.SEASONINGS), 15)
        self.assertEqual(lb.FULL_MASK, 32767)

    def test_config_edits_keep_old_layout(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "c.json"
            data = {"soup_bowl": {"spicy": {"duration": 120}, "clear": {"hunger": 4}},
                    "three": 1}
            for dotted in lb.CONFIG_EDITS:
                node = data
                parts = dotted.split(".")
                for part in parts[:-1]:
                    node = node.setdefault(part, {})
                node.setdefault(parts[-1], 0)
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            lb.apply_config_edits(path)
            text = path.read_text(encoding="utf-8")
            self.assertTrue(text.endswith("\n"))
            self.assertEqual(json.loads(text)["soup_bowl"]["spicy"]["duration"], 240)


class RuleValueAndConfigTests(unittest.TestCase):
    def test_rule_can_restrict_old_and_new_values(self):
        rules = [{"entry": "t", "path": "Facing", "kind": "added", "new": "east|south", "reason": "r"}]
        self.assertEqual(lb.rule_allows(rules, "t", "added", "Facing", "", "east"), "r")
        self.assertIsNone(lb.rule_allows(rules, "t", "added", "Facing", "", "up"))

    def test_shipped_r146_allow_file_loads_and_matches_defaults(self):
        rules = lb.load_allow(str(Path(lb.__file__).with_name("legacy_baseline_allow_r146.json")))
        self.assertTrue(lb.rule_allows(rules, "table_oak_mixed", "added", "Plates[3].Facing", "", "south"))
        self.assertTrue(lb.rule_allows(rules, "table_oak_mixed", "added", "Plates[3].ChopsticksYaw", "", "135.0f"))
        self.assertIsNone(lb.rule_allows(rules, "pot_clear", "added", "Facing", "", "east"))

    def test_compare_config_separates_values_from_doc_text(self):
        old = {"_format": 1, "_字段说明": {"a": "旧"}, "x": {"y": 1, "z": 2}}
        new = {"_format": 1, "_字段说明": {"a": "新"}, "x": {"y": 1, "z": 3, "w": 4}}
        r = lb.compare_config(old, new)
        self.assertEqual(r["changed"], [("x.z", 2, 3)])
        self.assertEqual(r["added"], ["x.w"])
        self.assertEqual(r["removed"], [])
        self.assertEqual(r["doc_changed"], ["_字段说明.a"])


if __name__ == "__main__":
    unittest.main()
