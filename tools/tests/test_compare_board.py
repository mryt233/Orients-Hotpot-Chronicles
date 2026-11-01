"""方案对比板冒烟测试：两套假方案能出图、尺寸对、坏图的自检结果会出现。作者：mryt233（2026-11-01）

    python tools/tests/test_compare_board.py
"""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from PIL import Image  # noqa: E402

import compare_board  # noqa: E402
from pixkit import canvas  # noqa: E402


class BoardTest(unittest.TestCase):
    def test_png_board(self):
        a = canvas.blank()
        for y in range(5, 11):
            for x in range(5, 11):
                a.putpixel((x, y), (180 + x, 60, 50, 255))
        canvas.outline_outer(a, (60, 10, 8, 255))
        b = a.copy()
        b.putpixel((8, 8), (200, 40, 30, 100))            # 半透明，应在自检里出现
        with tempfile.TemporaryDirectory() as d:
            out = compare_board.board("测试", [{"png": a, "label": "1 号"}, {"png": b, "label": "2 号"}],
                                      "item/test_beef_loin", out=Path(d) / "b.png")
            with Image.open(out) as im:
                self.assertEqual(im.size[0], 60 + 3 * compare_board.COL_W + 10)   # 两套 + 原版参考
        self.assertEqual(compare_board.check_image(b, "item/test_beef_loin")[0][1], "semi")


if __name__ == "__main__":
    unittest.main(verbosity=1)
