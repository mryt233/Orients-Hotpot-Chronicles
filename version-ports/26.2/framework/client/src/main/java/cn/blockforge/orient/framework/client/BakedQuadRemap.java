// 作者：mryt233
package cn.blockforge.orient.framework.client;

import cn.blockforge.orient.framework.client.mixin.ItemStackRenderStateAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 原版 {@link BakedQuad} 贴图与 UV 等比例重映射工具（纯原版类，供 Fabric / NeoForge 模型包装共用）。
 * mryt233 改：26.2 适配不可变 {@link BakedQuad} record 与 {@link UVPair} 压缩 UV。作者：mryt233
 */
public final class BakedQuadRemap {
    public record QuadSwapKey(BakedQuad original, TextureAtlasSprite target) {
    }

    // mryt233 改：按模型、状态、方向及兜底的身份分键，资源重载时一起清空。
    private static final int SPRITE_LIMIT = 2048, LOOKUP_DEPTH_LIMIT = 8;
    private static final Object CACHE_LOCK = new Object();
    private static final Map<SpriteKey, TextureAtlasSprite> SPRITE_CACHE = new LinkedHashMap<>(32, .75F, true);
    private static final ThreadLocal<Lookup> LOOKUP = new ThreadLocal<>();
    private static long generation;

    private record SpriteKey(BlockStateModel model, BlockState state, Direction face, BlockState fallback) {
        @Override public boolean equals(Object other) {
            return other instanceof SpriteKey key && model == key.model && state == key.state
                && face == key.face && fallback == key.fallback;
        }
        @Override public int hashCode() {
            int hash = 31 * System.identityHashCode(model) + System.identityHashCode(state);
            return 31 * (31 * hash + System.identityHashCode(face)) + System.identityHashCode(fallback);
        }
    }

    private static final class Lookup {
        final Map<BlockStateModel, Boolean> active = new IdentityHashMap<>();
        boolean tripped;
    }

    private BakedQuadRemap() {
    }

    /** mryt233 改（2026-11-23）：允许平台侧复制扩展渲染属性（如 NeoForge ambientOcclusion）。 */
    @FunctionalInterface
    public interface QuadFactory {
        BakedQuad create(BakedQuad original, long uv0, long uv1, long uv2, long uv3,
                         TextureAtlasSprite target, ChunkSectionLayer layer, RenderType itemRenderType);
    }

    /** 把单个四边形的贴图与压缩顶点 UV 从占位精灵比例映射到目标材质精灵，同时清掉染色索引。 */
    public static BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite target) {
        return remapQuad(quad, target, null);
    }

    public static BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite target, @Nullable QuadFactory factory) {
        BakedQuad.MaterialInfo oldInfo = quad.materialInfo();
        TextureAtlasSprite from = oldInfo.sprite();
        long uv0 = remapPackedUv(quad.packedUV0(), from, target);
        long uv1 = remapPackedUv(quad.packedUV1(), from, target);
        long uv2 = remapPackedUv(quad.packedUV2(), from, target);
        long uv3 = remapPackedUv(quad.packedUV3(), from, target);
        ChunkSectionLayer layer = ChunkSectionLayer.byTransparency(target.transparency());
        RenderType itemRenderType = target.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)
            ? (target.transparency().hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet())
            : (target.transparency().hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet());
        if (factory != null) {
            return factory.create(quad, uv0, uv1, uv2, uv3, target, layer, itemRenderType);
        }
        BakedQuad.MaterialInfo nextInfo = new BakedQuad.MaterialInfo(
            target, layer, itemRenderType, -1, oldInfo.shade(), oldInfo.lightEmission());
        return new BakedQuad(
            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            uv0, uv1, uv2, uv3, quad.direction(), nextInfo);
    }

    private static long remapPackedUv(long packed, TextureAtlasSprite from, TextureAtlasSprite to) {
        float u = UVPair.unpackU(packed);
        float v = UVPair.unpackV(packed);
        return UVPair.pack(remapU(from, to, u), remapV(from, to, v));
    }

    /** 按替换映射重绘一组原版四边形；无命中时直接返回原列表避免额外分配。 */
    public static List<BakedQuad> remapQuads(List<BakedQuad> src,
                                             @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps) {
        return remapQuads(src, swaps, null);
    }

    public static List<BakedQuad> remapQuads(List<BakedQuad> src,
                                             @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                             @Nullable QuadFactory factory) {
        if (src.isEmpty() || swaps == null || swaps.isEmpty()) {
            return src;
        }
        List<BakedQuad> out = null;
        for (int i = 0; i < src.size(); i++) {
            BakedQuad q = src.get(i);
            TextureAtlasSprite orig = q.materialInfo().sprite();
            TextureAtlasSprite next = swaps.get(orig);
            if (next != null && next != orig) {
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

    /** 包装一个 {@link BlockStateModelPart} 并在读取任意方向面片时按需缓存重映射后的四边形列表。 */
    public static BlockStateModelPart remapPart(BlockStateModelPart part,
                                                @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                                Material.@Nullable Baked overrideParticle) {
        return remapPart(part, swaps, overrideParticle, null);
    }

    public static BlockStateModelPart remapPart(BlockStateModelPart part,
                                                @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                                Material.@Nullable Baked overrideParticle,
                                                @Nullable QuadFactory factory) {
        if ((swaps == null || swaps.isEmpty()) && overrideParticle == null) {
            return part;
        }
        return new RemappedPart(part, swaps != null ? swaps : Map.of(), overrideParticle, factory);
    }

    /** 计算替换贴图引入的额外渲染标志位（半透明与动画精灵）。 */
    public static int combineMaterialFlags(int baseFlags,
                                           @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps) {
        if (swaps == null || swaps.isEmpty()) {
            return baseFlags;
        }
        int flags = baseFlags;
        for (TextureAtlasSprite sprite : swaps.values()) {
            if (sprite == null) {
                continue;
            }
            if (ChunkSectionLayer.byTransparency(sprite.transparency()).translucent()) {
                flags |= BakedQuad.FLAG_TRANSLUCENT;
            }
            if (sprite.contents().isAnimated()) {
                flags |= BakedQuad.FLAG_ANIMATED;
            }
        }
        return flags;
    }

    /** 判断替换贴图中是否包含动画精灵。 */
    public static boolean hasAnimatedSwap(@Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps) {
        if (swaps == null || swaps.isEmpty()) {
            return false;
        }
        for (TextureAtlasSprite sprite : swaps.values()) {
            if (sprite != null && sprite.contents().isAnimated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在物品模型 {@code update} 执行完毕后，将本次新增渲染层中的 {@link BakedQuad} 原地替换为目标材质，
     * 并借助按 {@link QuadSwapKey} 建立的缓存避免每帧重复构造四边形。
     */
    public static void remapItemRenderState(ItemStackRenderState output,
                                            int startLayer,
                                            @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                            Map<QuadSwapKey, BakedQuad> quadCache) {
        remapItemRenderState(output, startLayer, swaps, quadCache, null);
    }

    public static void remapItemRenderState(ItemStackRenderState output,
                                            int startLayer,
                                            @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                                            Map<QuadSwapKey, BakedQuad> quadCache,
                                            @Nullable QuadFactory factory) {
        if (swaps == null || swaps.isEmpty()) {
            return;
        }
        if (!(output instanceof ItemStackRenderStateAccessor accessor)) {
            return;
        }
        int endLayer = accessor.orient$activeLayerCount();
        if (endLayer <= startLayer) {
            return;
        }
        ItemStackRenderState.LayerRenderState[] layers = accessor.orient$layers();
        boolean animated = hasAnimatedSwap(swaps);
        for (int layerIdx = startLayer; layerIdx < endLayer; layerIdx++) {
            List<BakedQuad> quads = layers[layerIdx].prepareQuadList();
            for (int i = 0; i < quads.size(); i++) {
                BakedQuad quad = quads.get(i);
                TextureAtlasSprite orig = quad.materialInfo().sprite();
                TextureAtlasSprite next = swaps.get(orig);
                if (next != null && next != orig) {
                    if (quadCache.size() > 4096) {
                        quadCache.clear();
                    }
                    BakedQuad remapped = quadCache.computeIfAbsent(
                        new QuadSwapKey(quad, next),
                        key -> remapQuad(key.original(), key.target(), factory));
                    quads.set(i, remapped);
                }
            }
        }
        if (animated) {
            output.setAnimated();
        }
        output.appendModelIdentityElement(swaps);
    }

    /** 将归一化 UV [0..1] 按原精灵相对比例重映射到目标精灵。 */
    public static float remapU(TextureAtlasSprite from, TextureAtlasSprite to, float u) {
        float span = from.getU1() - from.getU0();
        float rel = span <= 1.0e-6F ? 0.5F : clamp01((u - from.getU0()) / span);
        return to.getU(rel);
    }

    public static float remapV(TextureAtlasSprite from, TextureAtlasSprite to, float v) {
        float span = from.getV1() - from.getV0();
        float rel = span <= 1.0e-6F ? 0.5F : clamp01((v - from.getV0()) / span);
        return to.getV(rel);
    }

    /** 模型可能由别的模组包装；重入回退到缺失精灵，不再访问被包装模型。 */
    public static TextureAtlasSprite resolveFaceSprite(BlockState state, Direction face, @Nullable BlockState fallback) {
        BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockState safe = fallback != null ? fallback : Blocks.OAK_PLANKS.defaultBlockState();
        BlockStateModel model = models.get(state);
        SpriteKey key = new SpriteKey(model, state, face, safe);
        Lookup lookup = LOOKUP.get();
        if (lookup != null && (lookup.active.size() >= LOOKUP_DEPTH_LIMIT || lookup.active.containsKey(model))) {
            lookup.tripped = true;
            return models.missingModel().particleMaterial().sprite();
        }
        long start;
        synchronized (CACHE_LOCK) {
            TextureAtlasSprite cached = SPRITE_CACHE.get(key);
            if (cached != null) return cached;
            start = generation;
        }
        if (lookup == null) { lookup = new Lookup(); LOOKUP.set(lookup); }
        lookup.active.put(model, Boolean.TRUE);
        try {
            TextureAtlasSprite result = model == null ? null : findFaceSpriteUncached(model, face);
            if (result == null) result = state == safe ? models.missingModel().particleMaterial().sprite() : resolveFaceSprite(safe, face, safe);
            synchronized (CACHE_LOCK) {
                if (!lookup.tripped && start == generation) {
                    SPRITE_CACHE.put(key, result);
                    if (SPRITE_CACHE.size() > SPRITE_LIMIT) SPRITE_CACHE.remove(SPRITE_CACHE.keySet().iterator().next());
                }
            }
            return result;
        } finally {
            lookup.active.remove(model);
            if (lookup.active.isEmpty()) LOOKUP.remove();
        }
    }

    private static @Nullable TextureAtlasSprite findFaceSpriteUncached(BlockStateModel model, Direction face) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(face);
            if (!quads.isEmpty()) {
                return quads.get(0).materialInfo().sprite();
            }
        }
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> general = part.getQuads(null);
            if (!general.isEmpty()) {
                for (BakedQuad q : general) {
                    if (q.direction() == face) {
                        return q.materialInfo().sprite();
                    }
                }
                return general.get(0).materialInfo().sprite();
            }
        }
        Material.Baked particle = model.particleMaterial();
        return particle == null ? null : particle.sprite();
    }

    public static long spriteGeneration() {
        synchronized (CACHE_LOCK) { return generation; }
    }

    public static void clearSpriteCache() {
        synchronized (CACHE_LOCK) {
            generation++;
            SPRITE_CACHE.clear();
        }
    }

    private static float clamp01(float v) {
        return v < 0F ? 0F : (v > 1F ? 1F : v);
    }

    private static final class RemappedPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final Map<TextureAtlasSprite, TextureAtlasSprite> swaps;
        private final Material.Baked particle;
        private final int flags;
        private final @Nullable QuadFactory factory;
        private volatile @Nullable List<BakedQuad> nullQuads;
        private final EnumMap<Direction, List<BakedQuad>> dirQuads = new EnumMap<>(Direction.class);

        private RemappedPart(BlockStateModelPart delegate,
                             Map<TextureAtlasSprite, TextureAtlasSprite> swaps,
                             Material.@Nullable Baked overrideParticle,
                             @Nullable QuadFactory factory) {
            this.delegate = delegate;
            this.swaps = swaps;
            this.particle = overrideParticle != null ? overrideParticle : delegate.particleMaterial();
            this.flags = combineMaterialFlags(delegate.materialFlags(), swaps);
            this.factory = factory;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction == null) {
                List<BakedQuad> cached = this.nullQuads;
                if (cached == null) {
                    cached = List.copyOf(remapQuads(delegate.getQuads(null), swaps, factory));
                    this.nullQuads = cached;
                }
                return cached;
            }
            synchronized (dirQuads) {
                return dirQuads.computeIfAbsent(direction, d -> List.copyOf(remapQuads(delegate.getQuads(d), swaps, factory)));
            }
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return flags;
        }
    }
}
