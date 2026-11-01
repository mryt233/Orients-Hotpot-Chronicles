"""pixkit 公共库测试：每个函数和迁移前各脚本里的旧写法逐一对照。作者：mryt233（2026-11-01）

    python tools/tests/test_pixkit.py
"""
import json
import random
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from PIL import Image, ImageDraw  # noqa: E402

from pixkit import anim, canvas, color, io, model  # noqa: E402

RND = random.Random(20261101)
COLORS = [tuple(RND.randrange(256) for _ in range(4)) for _ in range(200)]
TS = [i / 40 for i in range(-4, 45)] + [0.5, 0.25, 0.75, 1 / 3]


# ---- 旧写法（迁移前从各 gen_*.py 原样抄来，作为对照）
def old_mix_cup(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def old_mix_boiling(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3))


def old_shade_round(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def old_clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def old_outline_inner(im, color):
    px = im.load()
    solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for (x, y) in solid:
        if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = color + (255,)


def old_outline_outer(im, color):
    src = im.copy()
    s, d = src.load(), im.load()
    for y in range(16):
        for x in range(16):
            if s[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and s[x + dx, y + dy][3] > 0
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                d[x, y] = color
    return im


def random_sprite(seed):
    r = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for _ in range(40):
        px[r.randrange(1, 15), r.randrange(1, 15)] = (r.randrange(256), r.randrange(256), r.randrange(256), 255)
    return im


class ColorTest(unittest.TestCase):
    def test_mix_matches_cup(self):
        for a in COLORS[:60]:
            for b in COLORS[60:80]:
                for t in (0.28, 0.5, 0.35, 0.3, 0.45, 0.4):
                    self.assertEqual(color.rgba(color.mix(a, b, t)), old_mix_cup(a, b, t))

    def test_lerp_matches_boiling(self):
        for a in COLORS[:40]:
            for b in COLORS[40:60]:
                for t in TS:
                    self.assertEqual(color.lerp(a, b, t), old_mix_boiling(a, b, t))

    def test_shade_variants(self):
        for c in COLORS:
            for k in (0.6, 0.62, 0.92, 1.06, 1.3, 0.0):
                self.assertEqual(color.shade(c[:3], k), old_shade_round(c[:3], k))
                self.assertEqual(color.shade_floor(c[:3], k), old_clamp(tuple(v * k for v in c[:3])))

    def test_clamp_hexc_rgba(self):
        self.assertEqual(color.clamp((-3, 12.9, 300)), (0, 12, 255))
        self.assertEqual(color.hexc("#3F76E4"), (63, 118, 228, 255))
        self.assertEqual(color.rgba((1, 2, 3, 9), 128), (1, 2, 3, 128))

    def test_ramp_dark_to_light(self):
        r = color.ramp((120, 60, 40), 5)
        self.assertEqual(len(r), 5)
        self.assertTrue(all(color.luma(r[i]) <= color.luma(r[i + 1]) for i in range(4)))


class CanvasTest(unittest.TestCase):
    def test_outline_inner_matches(self):
        for s in range(30):
            a, b = random_sprite(s), random_sprite(s)
            old_outline_inner(a, (30, 20, 10))
            canvas.outline_inner(b, (30, 20, 10))
            self.assertEqual(a.tobytes(), b.tobytes())

    def test_outline_outer_matches(self):
        for s in range(30):
            a, b = random_sprite(s), random_sprite(s)
            old_outline_outer(a, (30, 20, 10, 255))
            canvas.outline_outer(b, (30, 20, 10, 255))
            self.assertEqual(a.tobytes(), b.tobytes())

    def test_sphere_level_range(self):
        levels = {canvas.sphere_level(x, y, 8, 8, 6) for x in range(16) for y in range(16)}
        self.assertEqual(levels, {0, 1, 2, 3})
        self.assertGreater(canvas.sphere_level(5, 5, 8, 8, 6), canvas.sphere_level(11, 11, 8, 8, 6))

    def test_speckle_reproducible(self):
        a, b = random_sprite(1), random_sprite(1)
        canvas.speckle(a, random.Random(3), 0.5, 0.9)
        canvas.speckle(b, random.Random(3), 0.5, 0.9)
        self.assertEqual(a.tobytes(), b.tobytes())

    def test_iso_cube_and_rows(self):
        im = canvas.blank()
        canvas.iso_cube(ImageDraw.Draw(im), 8, 2, 10, 5, (250, 250, 240, 255), (220, 220, 200, 255), (200, 200, 180, 255))
        self.assertTrue(im.getbbox())
        im2 = canvas.from_rows([".X", "X."], {"X": (1, 2, 3, 255)}, 2)
        self.assertEqual(im2.getpixel((1, 0)), (1, 2, 3, 255))
        self.assertEqual(im2.getpixel((0, 0)), (0, 0, 0, 0))

    def test_recolor_by_luma(self):
        src = canvas.from_rows(["AB"], {"A": (10, 10, 10, 255), "B": (240, 240, 240, 255)}, 2, 1)
        out = canvas.recolor_by_luma(src, [(1, 0, 0), (0, 0, 9)])
        self.assertEqual(out.getpixel((0, 0)), (1, 0, 0, 255))
        self.assertEqual(out.getpixel((1, 0)), (0, 0, 9, 255))


class ModelIoAnimTest(unittest.TestCase):
    def test_box_and_models(self):
        b = model.box([0, 0, 0], [16, 1, 16], "#d", faces=("up",), uv={"up": [0, 0, 16, 16]})
        self.assertEqual(b, {"from": [0, 0, 0], "to": [16, 1, 16], "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#d"}}})
        self.assertEqual(model.item_generated("tofu")["textures"]["layer0"], "orients_hotpot_chronicles:item/tofu")
        bm = model.block_model([], {"d": "x:block/y"})
        self.assertEqual(list(bm["textures"]), ["d", "particle"])
        self.assertEqual(model.small_block_display(6)["gui"]["translation"], [0, 6, 0])

    def test_write_json_lf(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "a/b.json"
            io.write_json(p, {"k": 1})
            self.assertEqual(p.read_bytes(), b'{\n  "k": 1\n}\n')

    def test_anim_strip_save(self):
        frames = [canvas.blank(4, 4, (i, 0, 0, 255)) for i in range(3)]
        sheet = anim.strip(frames)
        self.assertEqual(sheet.size, (4, 12))
        self.assertEqual(sheet.getpixel((0, 8)), (2, 0, 0, 255))
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "t.png"
            anim.save(sheet, anim.mcmeta(2, interpolate=False), p)
            self.assertEqual(json.loads((Path(d) / "t.png.mcmeta").read_text()),
                             {"animation": {"frametime": 2, "interpolate": False}})


if __name__ == "__main__":
    unittest.main(verbosity=1)
