"""贴图自检测试：故意造坏图，看能不能被抓出来。作者：mryt233（2026-11-01）

    python tools/tests/test_check_textures.py
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from check_textures import check_image  # noqa: E402
from pixkit import canvas  # noqa: E402


def codes(res):
    return {c for _, c, _ in res}


def good_meat():
    """像原版生肉的一块：居中、实心、深色描边、7 级色阶。"""
    im = canvas.blank()
    px = im.load()
    ramp = [(120, 20, 18), (160, 30, 26), (190, 40, 34), (215, 60, 52), (226, 98, 90), (232, 120, 110), (238, 140, 125)]
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = ramp[(x + y) % 7] + (255,)
    canvas.outline_outer(im, (60, 10, 8, 255))
    return im


class CheckImageTest(unittest.TestCase):
    def test_good_image_passes(self):
        self.assertEqual(check_image(good_meat(), "item/test_beef_loin"), [])

    def test_semi_transparent(self):
        im = good_meat()
        im.putpixel((8, 8), (200, 40, 30, 120))
        self.assertIn("semi", codes(check_image(im, "item/test_beef_loin")))

    def test_off_center(self):
        im = canvas.blank()
        im.paste(good_meat().crop((3, 3, 13, 13)), (0, 0))
        self.assertIn("center", codes(check_image(im, "item/test_beef_loin")))

    def test_no_outline_vs_vanilla_meat(self):
        im = canvas.blank()
        for y in range(4, 12):
            for x in range(4, 12):
                im.putpixel((x, y), (200 + (x % 3) * 15, 60, 50, 255))
        self.assertIn("outline", codes(check_image(im, "item/test_beef_loin")))

    def test_too_many_colors(self):
        im = canvas.blank()
        for y in range(2, 14):
            for x in range(2, 14):
                im.putpixel((x, y), (100 + x * 9, 20 + y * 7, 30 + x * y % 50, 255))
        self.assertIn("colors", codes(check_image(im, "item/cooked_test_meatball")))

    def test_tall_without_mcmeta(self):
        im = canvas.blank(16, 64)
        self.assertIn("anim", codes(check_image(im, "block/test_anim")))

    def test_frame_index_out_of_range(self):
        im = canvas.blank(16, 32)
        res = check_image(im, "block/test_anim", {"animation": {"frames": [0, 1, 5]}})
        self.assertTrue(any(c == "anim" and "越界" in m for _, c, m in res))

    def test_bad_width(self):
        self.assertIn("size", codes(check_image(canvas.blank(15, 15), "block/odd")))

    def test_labpbr_valid_pair_skips_item_warnings(self):
        base = good_meat()
        norm = canvas.blank(16, 16, (128, 128, 255, 220))
        spec = canvas.blank(16, 16, (190, 230, 0, 255))
        self.assertEqual(check_image(norm, "item/iron_kitchen_knife_n", base_im=base), [])
        self.assertEqual(check_image(spec, "item/iron_kitchen_knife_s", base_im=base), [])

    def test_labpbr_detects_zero_alpha_bad_normal_reserved_f0_and_mismatch(self):
        base = canvas.blank(16, 16, (200, 200, 200, 255))
        bad_norm = canvas.blank(16, 16, (0, 0, 255, 0))
        res_n = codes(check_image(bad_norm, "block/hot_pot_copper_side_n", base_im=base))
        self.assertIn("pbr_alpha", res_n)
        self.assertIn("pbr_normal", res_n)

        bad_spec = canvas.blank(16, 16, (120, 240, 0, 255))
        res_s = codes(check_image(bad_spec, "block/hot_pot_copper_side_s", base_im=base))
        self.assertIn("pbr_f0", res_s)

        bad_size = canvas.blank(32, 32, (128, 128, 255, 255))
        res_size = codes(check_image(bad_size, "block/hot_pot_copper_side_n", base_im=base))
        self.assertIn("pbr_size", res_size)

        res_meta = codes(check_image(canvas.blank(16, 32, (128, 128, 255, 255)), "block/a_n", {"animation": {"frametime": 2}}, base_im=canvas.blank(16, 32, (200, 200, 200, 255)), base_meta=None))
        self.assertIn("pbr_meta", res_meta)

    def test_pbr_generator_contract_on_animated_and_composite_textures(self):
        import json
        from PIL import Image
        import gen_pbr
        cfg = json.loads((Path(__file__).resolve().parents[1] / "pbr_materials.json").read_text(encoding="utf-8"))
        # 验证普通扁平生肉/熟肉不在 PBR 映射中（控制包体与显存）
        for flat in ("item/beef_loin", "item/cooked_beef_loin", "item/tomato", "item/greens"):
            self.assertNotIn(flat, cfg["textures"])
        # 验证动画火焰与复合铁菜刀生成结果通过 check_image 且自发光只作用火焰区
        tex_root = Path(__file__).resolve().parents[2] / "common/src/main/resources/assets/orients_hotpot_chronicles/textures"
        stove_base = tex_root / "block/stove_front_lit.png"
        meta = json.loads(stove_base.with_name(stove_base.name + ".mcmeta").read_text(encoding="utf-8"))
        sn, ss = gen_pbr.generate_pair(stove_base, cfg["presets"][cfg["textures"]["block/stove_front_lit"]])
        with Image.open(stove_base).convert("RGBA") as sb:
            self.assertEqual(check_image(sn, "block/stove_front_lit_n", mcmeta=meta, base_im=sb, base_meta=meta), [])
            self.assertEqual(check_image(ss, "block/stove_front_lit_s", mcmeta=meta, base_im=sb, base_meta=meta), [])
        # 顶角砖块非自发光(255)，火膛中心存在线性自发光(1..254)
        self.assertEqual(ss.getpixel((0, 0))[3], 255)
        self.assertTrue(any(1 <= ss.getpixel((x, y))[3] < 255 for y in range(10, 15) for x in range(4, 12)))


if __name__ == "__main__":
    unittest.main(verbosity=1)
