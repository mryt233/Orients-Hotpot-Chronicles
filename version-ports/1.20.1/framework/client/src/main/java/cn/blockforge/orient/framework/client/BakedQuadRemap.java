// 作者：mryt233
package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原版 {@link BakedQuad} 贴图与 UV 等比例重映射工具（纯原版类，供 Fabric / Forge 1.20.1 模型包装共用）。
 * 作者：mryt233
 */
public final class BakedQuadRemap {
    private static final int VERTEX_STRIDE = Math.max(8, DefaultVertexFormat.BLOCK.getVertexSize() / 4);
    private static final int UV_OFFSET = computeUvOffset();
    private static final Map<BakedModel, EnumMap<Direction, TextureAtlasSprite>> SPRITE_CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private BakedQuadRemap() {
    }

    private static int computeUvOffset() {
        int offset = 0;
        for (VertexFormatElement element : DefaultVertexFormat.BLOCK.getElements()) {
            if (element == DefaultVertexFormat.ELEMENT_UV0) {
                return offset / 4;
            }
            offset += element.getByteSize();
        }
        return 4;
    }

    @FunctionalInterface
    public interface QuadFactory {
        BakedQuad create(BakedQuad original, int[] vertices, TextureAtlasSprite target);
    }

    private static final QuadFactory VANILLA_QUAD_FACTORY = (original, vertices, target) ->
        new BakedQuad(vertices, -1, original.getDirection(), target, original.isShade());

    /** 把单个四边形的贴图与顶点 UV 从占位精灵比例映射到目标材质精灵，同时清掉染色索引。 */
    public static BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite target) {
        return remapQuad(quad, target, VANILLA_QUAD_FACTORY);
    }

    /** 允许平台构造方保留原四边形上的扩展属性（如 Forge AO 标记）。 */
    public static BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite target, QuadFactory factory) {
        TextureAtlasSprite from = quad.getSprite();
        int[] src = quad.getVertices();
        int[] dst = src.clone();
        float u0 = from.getU0();
        float uSpan = from.getU1() - u0;
        float v0 = from.getV0();
        float vSpan = from.getV1() - v0;
        for (int v = 0; v < 4; v++) {
            int base = v * VERTEX_STRIDE + UV_OFFSET;
            if (base + 1 >= dst.length) {
                break;
            }
            float u = Float.intBitsToFloat(src[base]);
            float vv = Float.intBitsToFloat(src[base + 1]);
            float relU = uSpan <= 1.0e-6F ? 0.5F : clamp01((u - u0) / uSpan);
            float relV = vSpan <= 1.0e-6F ? 0.5F : clamp01((vv - v0) / vSpan);
            // mryt233 改（1.20.1 适配）：sprite.getU / getV 接受 0..16 像素坐标。
            dst[base] = Float.floatToRawIntBits(target.getU(relU * 16.0F));
            dst[base + 1] = Float.floatToRawIntBits(target.getV(relV * 16.0F));
        }
        return factory.create(quad, dst, target);
    }

    /** 按替换映射重绘一组原版四边形；无命中时直接返回原列表避免额外分配。 */
    public static List<BakedQuad> remapQuads(List<BakedQuad> src,
                                             Map<TextureAtlasSprite, TextureAtlasSprite> swaps) {
        return remapQuads(src, swaps, VANILLA_QUAD_FACTORY);
    }

    /** 按替换映射重绘一组原版四边形，并使用指定构造方保留平台扩展属性。 */
    public static List<BakedQuad> remapQuads(List<BakedQuad> src,
                                             Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                             QuadFactory factory) {
        if (src.isEmpty() || swaps == null || swaps.isEmpty()) {
            return src;
        }
        List<BakedQuad> out = null;
        for (int i = 0; i < src.size(); i++) {
            BakedQuad q = src.get(i);
            TextureAtlasSprite next = swaps.get(q.getSprite());
            if (next != null && next != q.getSprite()) {
                if (out == null) {
                    out = new ArrayList<>(src.size());
                    for (int j = 0; j < i; j++) {
                        out.add(src.get(j));
                    }
                }
                out.add(remapQuad(q, next, factory));
            } else if (out != null) {
                out.add(q);
            }
        }
        return out != null ? out : src;
    }

    /** 将归一化 UV [0..1] 按原精灵相对比例重映射到目标精灵（1.20.1 使用 0..16 像素参数）。 */
    public static float remapU(TextureAtlasSprite from, TextureAtlasSprite to, float u) {
        float span = from.getU1() - from.getU0();
        float rel = span <= 1.0e-6F ? 0.5F : clamp01((u - from.getU0()) / span);
        return to.getU(rel * 16.0F);
    }

    public static float remapV(TextureAtlasSprite from, TextureAtlasSprite to, float v) {
        float span = from.getV1() - from.getV0();
        float rel = span <= 1.0e-6F ? 0.5F : clamp01((v - from.getV0()) / span);
        return to.getV(rel * 16.0F);
    }

    /** 从任意已烘焙方块模型中安全提取指定朝向面的主贴图精灵（缺失面自动退回无剔除面或粒子精灵）。 */
    public static TextureAtlasSprite resolveFaceSprite(BlockState state, Direction face, BlockState fallback) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        if (model == null) {
            return dispatcher.getBlockModel(fallback != null ? fallback : Blocks.OAK_PLANKS.defaultBlockState())
                .getParticleIcon();
        }
        synchronized (SPRITE_CACHE) {
            EnumMap<Direction, TextureAtlasSprite> map = SPRITE_CACHE.computeIfAbsent(
                model, m -> new EnumMap<>(Direction.class));
            TextureAtlasSprite cached = map.get(face);
            if (cached != null) {
                return cached;
            }
        }
        TextureAtlasSprite resolved = findFaceSpriteUncached(model, state, face, fallback);
        synchronized (SPRITE_CACHE) {
            SPRITE_CACHE.computeIfAbsent(model, m -> new EnumMap<>(Direction.class)).put(face, resolved);
        }
        return resolved;
    }

    private static TextureAtlasSprite findFaceSpriteUncached(BakedModel model, BlockState state,
                                                             Direction face, BlockState fallback) {
        RandomSource rand = RandomSource.create(42L);
        List<BakedQuad> quads = model.getQuads(state, face, rand);
        if (!quads.isEmpty()) {
            return quads.get(0).getSprite();
        }
        List<BakedQuad> general = model.getQuads(state, null, rand);
        if (!general.isEmpty()) {
            for (BakedQuad q : general) {
                if (q.getDirection() == face) {
                    return q.getSprite();
                }
            }
            return general.get(0).getSprite();
        }
        TextureAtlasSprite particle = model.getParticleIcon();
        if (particle != null) {
            return particle;
        }
        BlockState safe = fallback != null ? fallback : Blocks.OAK_PLANKS.defaultBlockState();
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(safe).getParticleIcon();
    }

    public static void clearSpriteCache() {
        SPRITE_CACHE.clear();
    }

    private static float clamp01(float v) {
        return v < 0F ? 0F : (v > 1F ? 1F : v);
    }
}
