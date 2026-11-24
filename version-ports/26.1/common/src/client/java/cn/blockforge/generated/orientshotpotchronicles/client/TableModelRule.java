// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableMaterialData;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableMaterials;
import cn.blockforge.orient.framework.client.BakedQuadRemap;
import cn.blockforge.orient.framework.client.SwappableModelRule;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 统一火锅桌与 3 种备菜架的跨加载器烘焙模型贴图槽位替换规则（方案二）。作者：mryt233；mryt233 改（2026-11-16）：适配 26.1 Identifier。
 */
public final class TableModelRule implements SwappableModelRule {
    private static final Set<Identifier> TARGETS = Set.of(
        HotPot.id("hotpot_table"),
        HotPot.id("tall_hotpot_table"),
        HotPot.id("multi_tier_prep_rack"),
        HotPot.id("desktop_prep_rack"),
        HotPot.id("hanging_prep_rack")
    );

    @Override
    public boolean matches(Identifier modelId) {
        return modelId != null && TARGETS.contains(modelId);
    }

    @Override
    public @Nullable Map<TextureAtlasSprite, TextureAtlasSprite> resolveSwaps(
        @Nullable Object renderData, @Nullable ItemStack stack,
        @Nullable BlockState state, Function<Identifier, TextureAtlasSprite> blockAtlas) {
        TableMaterialData data = extractData(renderData, stack);
        TextureAtlasSprite slotTopCap = blockAtlas.apply(TableMaterials.SLOT_TOP_CAP);
        TextureAtlasSprite slotTopSide = blockAtlas.apply(TableMaterials.SLOT_TOP_SIDE);
        TextureAtlasSprite slotBaseCap = blockAtlas.apply(TableMaterials.SLOT_BASE_CAP);
        TextureAtlasSprite slotBaseSide = blockAtlas.apply(TableMaterials.SLOT_BASE_SIDE);
        TextureAtlasSprite slotTallRing = blockAtlas.apply(TableMaterials.SLOT_TALL_RING);
        if (slotTopCap == null || slotTopSide == null || slotBaseCap == null || slotBaseSide == null) {
            return null;
        }
        BlockState topState = TableMaterials.resolveDisplayState(
            data.top(), Blocks.OAK_PLANKS.defaultBlockState());
        BlockState baseState = TableMaterials.resolveDisplayState(
            data.base(), Blocks.BRICKS.defaultBlockState());
        BlockState ringState = !data.legacyRing().isEmpty()
            ? TableMaterials.resolveDisplayState(data.legacyRing(), baseState)
            : baseState;

        Map<TextureAtlasSprite, TextureAtlasSprite> swaps = new HashMap<>(8);
        swaps.put(slotTopCap, BakedQuadRemap.resolveFaceSprite(topState, Direction.UP, Blocks.OAK_PLANKS.defaultBlockState()));
        swaps.put(slotTopSide, BakedQuadRemap.resolveFaceSprite(topState, Direction.NORTH, Blocks.OAK_PLANKS.defaultBlockState()));
        swaps.put(slotBaseCap, BakedQuadRemap.resolveFaceSprite(baseState, Direction.UP, Blocks.BRICKS.defaultBlockState()));
        swaps.put(slotBaseSide, BakedQuadRemap.resolveFaceSprite(baseState, Direction.NORTH, Blocks.BRICKS.defaultBlockState()));
        if (slotTallRing != null) {
            swaps.put(slotTallRing, BakedQuadRemap.resolveFaceSprite(ringState, Direction.NORTH, Blocks.BRICKS.defaultBlockState()));
        }
        return swaps;
    }

    @Override
    public @Nullable TextureAtlasSprite particleSprite(
        @Nullable Object renderData, @Nullable BlockState state,
        Function<Identifier, TextureAtlasSprite> blockAtlas) {
        TableMaterialData data = extractData(renderData, null);
        BlockState topState = TableMaterials.resolveDisplayState(data.top(), Blocks.OAK_PLANKS.defaultBlockState());
        return BakedQuadRemap.resolveFaceSprite(topState, Direction.UP, Blocks.OAK_PLANKS.defaultBlockState());
    }

    private static TableMaterialData extractData(@Nullable Object renderData, @Nullable ItemStack stack) {
        if (renderData instanceof TableMaterialData data) {
            return data;
        }
        if (stack != null && !stack.isEmpty()) {
            TableMaterialData comp = stack.get(HotPotContent.TABLE_MATERIALS);
            if (comp != null) {
                return comp;
            }
        }
        return TableMaterialData.EMPTY;
    }
}
