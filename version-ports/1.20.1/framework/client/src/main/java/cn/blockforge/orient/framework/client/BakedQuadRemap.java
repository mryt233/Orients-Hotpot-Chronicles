// 作者：mryt233
package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
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
    // mryt233 改：不同状态与回退材质分别缓存；图集代际和第三方模型回调分离。
    private static final Object CACHE_LOCK = new Object();
    private static final int MAX_SPRITES = 2048;
    private static final int MAX_ITEM_MODELS = 128;
    private static final Map<SpriteKey, TextureAtlasSprite> SPRITE_CACHE = new LinkedHashMap<>(128, .75F, true);
    private static final Set<ItemCache> ITEM_CACHES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final ThreadLocal<Lookup> LOOKUP = new ThreadLocal<>();
    private static long generation;

    private record SpriteKey(BakedModel model, BlockState state, Direction face, BlockState fallback) {
        @Override public boolean equals(Object other) {
            return other instanceof SpriteKey key && model == key.model && state == key.state
                && face == key.face && fallback == key.fallback;
        }
        @Override public int hashCode() {
            return Objects.hash(System.identityHashCode(model), state, face, fallback);
        }
    }

    private static final class Lookup {
        final BakedModel[] active = new BakedModel[8];
        int size;
        boolean reentered;
    }

    /** 按实际精灵映射缓存物品包装，容量固定；重烘焙时清空仍存活的旧包装。 */
    public static final class ItemCache {
        private final Map<Map<TextureAtlasSprite, TextureAtlasSprite>, BakedModel> values =
            new LinkedHashMap<>(16, .75F, true);

        public ItemCache() {
            synchronized (CACHE_LOCK) {
                ITEM_CACHES.add(this);
            }
        }

        public BakedModel getOrCreate(Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                      Function<Map<TextureAtlasSprite, TextureAtlasSprite>, BakedModel> factory) {
            synchronized (CACHE_LOCK) {
                BakedModel cached = values.get(swaps);
                if (cached != null) return cached;
                var copy = new LinkedHashMap<TextureAtlasSprite, TextureAtlasSprite>();
                swaps.forEach((from, to) -> { if (from != null && to != null) copy.put(from, to); });
                var key = Collections.unmodifiableMap(copy);
                BakedModel next = factory.apply(key); // 本地包装构造器，不得回调外部模型。
                if (values.size() >= MAX_ITEM_MODELS) values.remove(values.keySet().iterator().next());
                values.put(key, next);
                return next;
            }
        }
    }

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

    /** 同一线程不重复进入同个模型；循环分支使用图集的缺失精灵，而不再次调用模型。 */
    public static TextureAtlasSprite resolveFaceSprite(BlockState state, Direction face, BlockState fallback) {
        BlockState safe = fallback != null ? fallback : Blocks.OAK_PLANKS.defaultBlockState();
        if (state == null) state = safe;
        var minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        var key = new SpriteKey(model, state, face, safe);
        long before;
        synchronized (CACHE_LOCK) {
            TextureAtlasSprite cached = SPRITE_CACHE.get(key);
            if (cached != null) return cached;
            before = generation;
        }
        Lookup lookup = LOOKUP.get();
        if (lookup == null) {
            lookup = new Lookup();
            LOOKUP.set(lookup);
        }
        for (int i = 0; i < lookup.size; i++) {
            if (lookup.active[i] == model) {
                lookup.reentered = true;
                return missingSprite(minecraft);
            }
        }
        if (lookup.size == lookup.active.length) {
            lookup.reentered = true;
            return missingSprite(minecraft);
        }
        lookup.active[lookup.size++] = model;
        try {
            TextureAtlasSprite resolved = model == null ? null : findFaceSpriteUncached(model, state, face);
            if (resolved == null) resolved = state == safe ? missingSprite(minecraft) : resolveFaceSprite(safe, face, safe);
            if (!lookup.reentered) {
                synchronized (CACHE_LOCK) {
                    if (before == generation) {
                        if (SPRITE_CACHE.size() >= MAX_SPRITES) SPRITE_CACHE.remove(SPRITE_CACHE.keySet().iterator().next());
                        SPRITE_CACHE.put(key, resolved);
                    }
                }
            }
            return resolved;
        } finally {
            lookup.active[--lookup.size] = null;
            if (lookup.size == 0) LOOKUP.remove();
        }
    }

    private static TextureAtlasSprite missingSprite(Minecraft minecraft) {
        return minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)
            .getSprite(MissingTextureAtlasSprite.getLocation());
    }

    private static TextureAtlasSprite findFaceSpriteUncached(BakedModel model, BlockState state,
                                                             Direction face) {
        RandomSource rand = RandomSource.create(42L);
        List<BakedQuad> quads = model.getQuads(state, face, rand);
        if (!quads.isEmpty()) return quads.get(0).getSprite();
        List<BakedQuad> general = model.getQuads(state, null, rand);
        if (!general.isEmpty()) {
            for (BakedQuad q : general) if (q.getDirection() == face) return q.getSprite();
            return general.get(0).getSprite();
        }
        return model.getParticleIcon();
    }

    /** mryt233 改：货架轮廓缓存与同一图集代次绑定，不跨资源重载复用。 */
    public static long spriteGeneration() {
        synchronized (CACHE_LOCK) { return generation; }
    }

    public static void clearSpriteCache() {
        synchronized (CACHE_LOCK) {
            generation++;
            SPRITE_CACHE.clear();
            for (ItemCache cache : ITEM_CACHES) cache.values.clear();
        }
    }

    private static float clamp01(float v) {
        return v < 0F ? 0F : (v > 1F ? 1F : v);
    }
}
