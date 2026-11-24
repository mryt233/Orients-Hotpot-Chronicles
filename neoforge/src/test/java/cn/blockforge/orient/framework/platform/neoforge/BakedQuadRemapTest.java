// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.client.BakedQuadRemap;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.util.TriState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 只构造 UV 数据，不创建原生图像、窗口、图形上下文或游戏世界。 */
class BakedQuadRemapTest {
    @Test void platformFactoryPreservesAoAndShadeForAllCombinations() throws Exception {
        TextureAtlasSprite from = sprite(0, 1, 0, 1);
        TextureAtlasSprite to = sprite(.25F, .5F, .5F, .75F);
        for (boolean ao : new boolean[]{false, true}) {
            for (boolean shade : new boolean[]{false, true}) {
                BakedQuad source = quad(from, ao, shade);
                BakedQuad remapped = BakedQuadRemap.remapQuad(source, to, NeoForgeClientContent::copyRemappedQuad);
                assertEquals(ao, remapped.hasAmbientOcclusion());
                assertEquals(shade, remapped.isShade());
                assertEquals(Direction.UP, remapped.getDirection());
                assertSame(to, remapped.getSprite());
                assertEquals(-1, remapped.getTintIndex());
            }
        }
    }

    @Test void uvRemapCopiesVerticesWithoutChangingSource() throws Exception {
        TextureAtlasSprite from = sprite(0, 1, 0, 1);
        TextureAtlasSprite to = sprite(.25F, .5F, .5F, .75F);
        BakedQuad source = quad(from, false, true);
        int[] before = source.getVertices().clone();
        BakedQuad result = BakedQuadRemap.remapQuad(source, to, NeoForgeClientContent::copyRemappedQuad);
        assertNotSame(source.getVertices(), result.getVertices());
        assertArrayEquals(before, source.getVertices());
        for (int v = 0; v < 4; v++) {
            assertEquals(.25F + .25F * v / 4F, Float.intBitsToFloat(result.getVertices()[v * 8 + 4]), 1e-6F);
            assertEquals(.5F + .25F * v / 4F, Float.intBitsToFloat(result.getVertices()[v * 8 + 5]), 1e-6F);
            assertEquals(before[v * 8], result.getVertices()[v * 8]);
        }
    }

    @Test void noMatchReusesListAndDoesNotInvokeFactory() throws Exception {
        TextureAtlasSprite from = sprite(0, 1, 0, 1);
        TextureAtlasSprite other = sprite(0, 1, 0, 1);
        List<BakedQuad> input = List.of(quad(from, false, true));
        BakedQuadRemap.QuadFactory unexpected = (original, vertices, target) -> {
            throw new AssertionError("无命中时不应复制四边形");
        };
        assertSame(input, BakedQuadRemap.remapQuads(input, null, unexpected));
        assertSame(input, BakedQuadRemap.remapQuads(input, Map.of(), unexpected));
        assertSame(input, BakedQuadRemap.remapQuads(input, Map.of(other, from), unexpected));
        assertSame(input, BakedQuadRemap.remapQuads(input, Map.of(from, from), unexpected));
    }

    @Test void onlyMatchedQuadIsCopiedWithPlatformFactory() throws Exception {
        TextureAtlasSprite from = sprite(0, 1, 0, 1);
        TextureAtlasSprite to = sprite(.25F, .5F, .5F, .75F);
        TextureAtlasSprite other = sprite(0, 1, 0, 1);
        BakedQuad keep = quad(other, false, true);
        BakedQuad replace = quad(from, false, false);
        List<BakedQuad> input = List.of(keep, replace, keep);
        AtomicInteger copies = new AtomicInteger();
        List<BakedQuad> result = BakedQuadRemap.remapQuads(input, Map.of(from, to), (original, vertices, target) -> {
            copies.incrementAndGet();
            return NeoForgeClientContent.copyRemappedQuad(original, vertices, target);
        });
        assertEquals(1, copies.get());
        assertSame(keep, result.get(0));
        assertSame(keep, result.get(2));
        assertNotSame(replace, result.get(1));
        assertFalse(result.get(1).hasAmbientOcclusion());
        assertFalse(result.get(1).isShade());
        assertSame(replace, input.get(1));
    }

    @Test void oldVanillaEntryPointsRemainUsable() throws Exception {
        TextureAtlasSprite from = sprite(0, 1, 0, 1);
        TextureAtlasSprite to = sprite(.25F, .5F, .5F, .75F);
        BakedQuad source = quad(from, true, false);
        BakedQuad result = BakedQuadRemap.remapQuad(source, to);
        assertSame(to, result.getSprite());
        assertFalse(result.isShade());
        assertSame(source, BakedQuadRemap.remapQuads(List.of(source), Map.of()).get(0));
        assertSame(to, BakedQuadRemap.remapQuads(List.of(source), Map.of(from, to)).get(0).getSprite());
    }

    // mryt233 改：灶台只能提升 DEFAULT，不覆盖模型显式关闭的汤面。
    @Test void modelAoOnlyPromotesDefault() {
        for (TriState original : TriState.values()) {
            assertEquals(original, NeoForgeClientContent.resolveModelAo(original, false));
            assertEquals(original == TriState.DEFAULT ? TriState.TRUE : original,
                NeoForgeClientContent.resolveModelAo(original, true));
        }
    }

    @Test void copyWithAoReusesUnchangedAndPreservesAttributesWhenFlipped() throws Exception {
        TextureAtlasSprite sprite = sprite(0, 1, 0, 1);
        BakedQuad aoTrue = quad(sprite, true, false);
        assertSame(aoTrue, NeoForgeClientContent.copyWithAo(aoTrue, true));

        BakedQuad flippedOff = NeoForgeClientContent.copyWithAo(aoTrue, false);
        assertNotSame(aoTrue, flippedOff);
        assertFalse(flippedOff.hasAmbientOcclusion());
        assertFalse(flippedOff.isShade());
        assertEquals(3, flippedOff.getTintIndex());
        assertEquals(Direction.UP, flippedOff.getDirection());
        assertSame(sprite, flippedOff.getSprite());
        assertArrayEquals(aoTrue.getVertices(), flippedOff.getVertices());

        BakedQuad flippedBack = NeoForgeClientContent.copyWithAo(flippedOff, true);
        assertTrue(flippedBack.hasAmbientOcclusion());
    }

    private static BakedQuad quad(TextureAtlasSprite sprite, boolean ao, boolean shade) {
        int[] vertices = new int[32];
        for (int v = 0; v < 4; v++) {
            vertices[v * 8] = 123 + v;
            vertices[v * 8 + 4] = Float.floatToRawIntBits(v / 4F);
            vertices[v * 8 + 5] = Float.floatToRawIntBits(v / 4F);
        }
        return new BakedQuad(vertices, 3, Direction.UP, sprite, shade, ao);
    }

    private static TextureAtlasSprite sprite(float u0, float u1, float v0, float v1) throws Exception {
        Field singleton = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) singleton.get(null);
        TextureAtlasSprite sprite = (TextureAtlasSprite) unsafe.allocateInstance(TextureAtlasSprite.class);
        for (String name : new String[]{"u0", "u1", "v0", "v1"}) {
            Field field = TextureAtlasSprite.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setFloat(sprite, switch (name) {
                case "u0" -> u0;
                case "u1" -> u1;
                case "v0" -> v0;
                default -> v1;
            });
        }
        return sprite;
    }
}
