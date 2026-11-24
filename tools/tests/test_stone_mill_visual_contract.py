"""校验游戏方块、动态上盘及查看器的小石磨像素几何和状态入口。作者：mryt233"""
import json
from itertools import combinations
from pathlib import Path
import re
import sys
import unittest

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import gen_processing as gen
from export_stone_mill_pixel_preview import intersects


class StoneMillVisualContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(gen.MILL_GEOMETRY.read_bytes())
        cls.base = gen.mill_elements(cls.data['base'])
        cls.top = gen.mill_elements(cls.data['top'])
        cls.game = gen.MOD / 'common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot'

    def read_asset(self, relative):
        return json.loads((gen.ASSETS / relative).read_bytes())

    def test_block_item_and_runtime_use_confirmed_geometry(self):
        for suffix, elements in [('_base', self.base), ('_top', self.top), ('', self.base + self.top)]:
            model = self.read_asset(f'models/block/stone_mill{suffix}.json')
            self.assertEqual(model['elements'], elements)
            self.assertEqual(model['textures']['stone'], 'minecraft:block/cobblestone')
            self.assertEqual(model['textures']['particle'], 'minecraft:block/cobblestone')
            self.assertEqual(model['textures']['wood'], f'{gen.MODID}:block/stone_mill_wood')
        self.assertEqual(self.read_asset('models/item/stone_mill.json')['parent'], f'{gen.MODID}:block/stone_mill')
        self.assertEqual((gen.MILL_CLIENT / 'StoneMillGeometry.java').read_text(encoding='utf-8'), gen.mill_client_geometry(self.data))

    def test_all_mesh_coordinates_are_integral_and_mouth_stays_open(self):
        rows = self.data['base'] + self.data['top']
        self.assertTrue(all(type(n) is int for row in rows for n in row[:6]))
        self.assertFalse(any(intersects(a, b) for a, b in combinations(rows, 2)))
        r0, i0, i1, r1 = self.data['slot']['ring']
        y = self.data['slot']['floor']
        l, r, tip = self.data['slot']['mouth']
        self.assertFalse(any(intersects(row, [l, y, i1, r, y + 2, tip]) for row in rows))
        for x, z in ((8, i1 + .5), (8, r1 + .5), (8, tip - .5), ((r0 + i0) / 2, 8), ((i1 + r1) / 2, 8)):
            self.assertTrue(any(row[0] < x < row[3] and row[2] < z < row[5] and row[4] == y for row in rows))

    def test_four_facings_rotate_the_local_south_mouth_with_the_renderer(self):
        variants = self.read_asset('blockstates/stone_mill.json')['variants']
        self.assertEqual({f: m.get('y', 0) for f, m in variants.items()},
                         {f'facing={d}': y for d, y in [('south', 0), ('west', 90), ('north', 180), ('east', 270)]})
        self.assertTrue(all(m['model'].endswith(':block/stone_mill_base') for m in variants.values()))
        renderer = (gen.MILL_CLIENT / 'StoneMillRenderer.java').read_text(encoding='utf-8')
        self.assertIn('rotateAroundCenter(pose, facing.toYRot())', renderer)
        self.assertIn('Axis.YN.rotationDegrees(degrees)', renderer)

    def test_render_metadata_is_not_saved_and_only_follows_accepted_grind(self):
        be = (self.game / 'StoneMillBlockEntity.java').read_text(encoding='utf-8')
        grind = be.split('public boolean tryGrind(', 1)[1].split('public void dropContents()', 1)[0]
        accepted = grind.index('quarterTurns++;')
        self.assertLess(grind.index('input.getCount() < recipe.ingredientCount()'), accepted)
        self.assertLess(grind.index('waterUses <= 0'), accepted)
        self.assertEqual(len(re.findall(r'quarterTurns\+\+', be)), 1)
        self.assertIn('renderGrindWet = recipe.requiresWater();', grind)
        self.assertIn('renderGrindSpill = renderGrindWet;', grind)
        tag = be.split('CompoundTag getUpdateTag(', 1)[1].split('protected void saveAdditional(', 1)[0]
        saved = be.split('protected void saveAdditional(', 1)[1].split('protected void loadAdditional(', 1)[0]
        self.assertIn('tag.putInt("RenderGrindColor"', tag)
        self.assertNotIn('RenderGrind', saved)

    def test_renderer_boxes_ccw_winding_and_solid_pass_and_15_block_height(self):
        self.assertEqual(self.data['slot']['floor'], 14)
        self.assertEqual(self.data['slot']['seam'], 17)
        self.assertEqual(max(row[4] for row in self.data['top']), 24)
        geom = (gen.MILL_CLIENT / 'StoneMillGeometry.java').read_text(encoding='utf-8')
        self.assertIn('FLOOR = 14F;', geom)
        self.assertIn('SEAM = 17F;', geom)

        renderer = (gen.MILL_CLIENT / 'StoneMillRenderer.java').read_text(encoding='utf-8')
        self.assertIn('RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)', renderer)
        self.assertNotIn('entityCutoutNoCull', renderer)

        rb = (gen.MILL_CLIENT / 'RendererBoxes.java').read_text(encoding='utf-8')
        quad_body = rb.split('private static void quad(', 1)[1].split('private static void vertex(', 1)[0]
        order = re.findall(r'vertex\(vc,\s*e,\s*(x[0-3]),\s*y[0-3],\s*z[0-3],\s*(u[01]),\s*(v[01])', quad_body)
        self.assertEqual(order, [('x0', 'u0', 'v0'), ('x3', 'u0', 'v1'), ('x2', 'u1', 'v1'), ('x1', 'u1', 'v0')])

        # 校验 drawBox 的 6 个面与 topQuad 发射到 CCW quad(v0, v3, v2, v1) 后的几何外法线与传入法线一致
        sym = {'a': -1.0, 'd': 1.0, 'b': -1.0, 'f': 1.0, 'c': -1.0, 'g': 1.0,
               'ax': -1.0, 'bx': 1.0, 'fy': 0.5, 'cz': -1.0, 'dz': 1.0}
        def cross(u, v):
            return (u[1]*v[2] - u[2]*v[1], u[2]*v[0] - u[0]*v[2], u[0]*v[1] - u[1]*v[0])
        def sub(p, q):
            return (p[0]-q[0], p[1]-q[1], p[2]-q[2])
        def dot(u, v):
            return u[0]*v[0] + u[1]*v[1] + u[2]*v[2]

        quad_calls = re.findall(
            r'quad\(vc,\s*e,\s*sprite,\s*light,\s*r,\s*gr,\s*bl,\s*al,\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*[^;]*?'
            r'([a-z]{1,2}),\s*([a-z]{1,2}),\s*([a-z]{1,2}),\s*'
            r'([a-z]{1,2}),\s*([a-z]{1,2}),\s*([a-z]{1,2}),\s*'
            r'([a-z]{1,2}),\s*([a-z]{1,2}),\s*([a-z]{1,2}),\s*'
            r'([a-z]{1,2}),\s*([a-z]{1,2}),\s*([a-z]{1,2})\);',
            rb, re.S)
        self.assertEqual(len(quad_calls), 7)
        for m in quad_calls:
            n = (int(m[0]), int(m[1]), int(m[2]))
            v0 = (sym[m[3]], sym[m[4]], sym[m[5]])
            v3 = (sym[m[12]], sym[m[13]], sym[m[14]])
            v2 = (sym[m[9]], sym[m[10]], sym[m[11]])
            geom_n = cross(sub(v3, v0), sub(v2, v0))
            self.assertGreater(dot(geom_n, n), 0.0, f'normal mismatch for {m}')


if __name__ == '__main__':
    unittest.main()
