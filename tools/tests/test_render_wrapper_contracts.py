"""模型修复的源码接线契约；仅守入口覆盖，不替代客户端渲染验证。作者：mryt233"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class RenderWrapperContracts(unittest.TestCase):
    def test_fabric_vanilla_emission_has_non_null_material(self):
        text = (ROOT / "fabric/src/main/java/cn/blockforge/orient/framework/platform/fabric/FabricClientContent.java").read_text(encoding="utf-8")
        self.assertIn("RenderMaterial material = standardMaterial(RendererAccess.INSTANCE.getRenderer())", text)
        self.assertIn("emitter.fromVanilla(q, material, dir)", text)
        self.assertNotRegex(text, r"fromVanilla\([^;]*,\s*null\s*,")

    def test_fabric_block_and_item_emission_use_cleanup_guard(self):
        text = (ROOT / "fabric/src/main/java/cn/blockforge/orient/framework/platform/fabric/FabricClientContent.java").read_text(encoding="utf-8")
        self.assertEqual(text.count("emitTransformed(context, quad ->"), 2)
        for call in ("wrapped.emitBlockQuads", "wrapped.emitItemQuads"):
            self.assertRegex(text, r"\},\s*\(\)\s*->\s*" + re.escape(call))

    def test_neoforge_all_four_quad_entries_use_platform_factory(self):
        text = (ROOT / "neoforge/src/main/java/cn/blockforge/orient/framework/platform/neoforge/NeoForgeClientContent.java").read_text(encoding="utf-8")
        calls = re.findall(r"BakedQuadRemap\.remapQuads\([^;]+;", text)
        self.assertEqual(len(calls), 4)
        self.assertTrue(all("QUAD_FACTORY" in call for call in calls))
        self.assertIn("original.hasAmbientOcclusion()", text)

    def test_shared_helper_does_not_depend_on_platform_apis(self):
        text = (ROOT / "framework/client/src/main/java/cn/blockforge/orient/framework/client/BakedQuadRemap.java").read_text(encoding="utf-8")
        self.assertNotIn("net.neoforged.", text)
        self.assertNotIn("net.fabricmc.", text)
        self.assertIn("public interface QuadFactory", text)
        self.assertIn("return remapQuad(quad, target, VANILLA_QUAD_FACTORY)", text)


if __name__ == "__main__":
    unittest.main()
