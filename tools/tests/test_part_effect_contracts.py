"""第二批粒子/掉落的接线与预览数据回归；不替代实际游戏验证。作者：mryt233"""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'common/src/main/java/cn/blockforge/generated/orientshotpotchronicles'


class PartEffectContracts(unittest.TestCase):
    def test_drop_bridge_is_server_safe_and_registered(self):
        mixins = json.loads((ROOT / 'common/src/main/resources/orients_hotpot_chronicles.mixins.json').read_text())
        self.assertIn('BlockDropInvoker', mixins['mixins'])
        self.assertIn('ParticleEnginePartMixin', mixins['client'])
        source = (JAVA / 'hotpot/StoveBlockEntity.java').read_text(encoding='utf-8')
        self.assertIn('dropSpawner.spawn(level', source)
        self.assertNotIn('level.addFreshEntity', source)
        self.assertNotIn('cn.blockforge.generated.orientshotpotchronicles.mixin', source)
        for name in ['fabric/src/main/java/cn/blockforge/generated/orientshotpotchronicles/platform/FabricPlatform.java',
                     'neoforge/src/main/java/cn/blockforge/generated/orientshotpotchronicles/neoforge/NeoForgePlatform.java']:
            self.assertIn('BlockDropInvoker::hotpot$popResource', (ROOT / name).read_text(encoding='utf-8'))

    def test_effect_channel_checks_capability_and_chunk_watchers(self):
        source = (JAVA / 'network/HotPotNetwork.java').read_text(encoding='utf-8')
        self.assertIn('network.canSendTo(player, PotBreakEffectPayload.ID)', source)
        self.assertIn('chunkMap.getPlayers', source)
        self.assertIn('registerS2C(PotBreakEffectPayload.ID', source)
        self.assertNotIn('registerC2S(PotBreakEffectPayload.ID', source)
        self.assertIn('ClientboundLevelEventPacket', source)

    def test_client_reads_snapshot_not_live_pot_state_for_break(self):
        path = ROOT / 'common/src/client/java/cn/blockforge/generated/orientshotpotchronicles/client/BlockPartParticles.java'
        source = path.read_text(encoding='utf-8')
        self.assertIn('Block.stateById(payload.stoveStateId())', source)
        self.assertNotIn('getBlockState(payload.pos())', source)
        self.assertIn('BlockBreakSamples.destroy(StoveBlock.potShape(stove))', source)

    def test_fabric_material_mixin_is_client_only(self):
        mixins = json.loads((ROOT / 'fabric/src/main/resources/orients_hotpot_chronicles.fabric.mixins.json').read_text())
        self.assertIn('TerrainParticleMaterialMixin', mixins['client'])
        self.assertNotIn('TerrainParticleMaterialMixin', mixins['mixins'])

    def test_exported_preview_has_real_geometry_coordinates(self):
        data = json.loads((ROOT / 'viewer/pot_break_samples.json').read_text())
        self.assertEqual(data['author'], 'mryt233')
        self.assertEqual(data['tall_lift'], 10 / 16)
        self.assertEqual(data['drop_y'], 1.125)
        self.assertGreater(len(data['x']), 0)
        self.assertEqual(len(data['x']), len(data['z']))
        for row in data['x'] + data['z']:
            self.assertEqual(len(row), 6)
            self.assertTrue(7.6 / 16 <= row[1] <= 1)

    def test_viewer_drop_model_uses_mod_namespace(self):
        source = (ROOT / 'viewer/animations.js').read_text(encoding='utf-8')
        self.assertIn('ctx.buildItem(`${NS}:${s.pot}_nine_grid_hotpot`)', source)
        self.assertIn('f=viewer/pot_break_samples.json', source)


if __name__ == '__main__':
    unittest.main()
