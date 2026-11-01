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


if __name__ == "__main__":
    unittest.main(verbosity=1)
