// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 桌子与备菜架共享换材质规则、方块 ID 编解码、挖掘手感折算与旧 40 桌预设映射（纯公共逻辑）。
 * 作者：mryt233
 */
public final class TableMaterials {
    public static final TagKey<Block> ALLOW_TAG = TagKey.create(Registries.BLOCK, HotPot.id("table_materials_allow"));
    public static final TagKey<Block> DENY_TAG = TagKey.create(Registries.BLOCK, HotPot.id("table_materials_deny"));

    public static final String DEFAULT_TOP_ID = "minecraft:oak_planks";
    public static final String DEFAULT_BASE_ID = "minecraft:bricks";

    public static final Identifier SLOT_TOP_CAP = HotPot.id("block/mat_slot_top_cap");
    public static final Identifier SLOT_TOP_SIDE = HotPot.id("block/mat_slot_top_side");
    public static final Identifier SLOT_BASE_CAP = HotPot.id("block/mat_slot_base_cap");
    public static final Identifier SLOT_BASE_SIDE = HotPot.id("block/mat_slot_base_side");
    public static final Identifier SLOT_TALL_RING = HotPot.id("block/mat_slot_tall_ring");

    public enum Slot {
        TOP,
        BASE
    }

    public record Preset(String key, String topId, String lowBaseId, String tallRingId, String tallWallId, boolean hardSurface) {
    }

    private static final Map<String, Preset> PRESETS_BY_LEGACY_KEY;
    static {
        Map<String, Preset> map = new LinkedHashMap<>();
        for (String wood : HotPotContent.TABLE_WOODS) {
            String planks = "minecraft:" + wood + "_planks";
            Preset low = new Preset(wood + "_hotpot_table", planks, planks, "", DEFAULT_BASE_ID, false);
            Preset tall = new Preset(wood + "_tall_hotpot_table", planks, DEFAULT_BASE_ID, planks, DEFAULT_BASE_ID, false);
            map.put(low.key(), low);
            map.put(tall.key(), tall);
        }
        for (String[] stone : HotPotContent.STONE_TABLES) {
            String blockId = "minecraft:" + stone[1];
            Preset low = new Preset(stone[0] + "_hotpot_table", blockId, DEFAULT_BASE_ID, "", DEFAULT_BASE_ID, true);
            Preset tall = new Preset(stone[0] + "_tall_hotpot_table", blockId, DEFAULT_BASE_ID, blockId, DEFAULT_BASE_ID, true);
            map.put(low.key(), low);
            map.put(tall.key(), tall);
        }
        PRESETS_BY_LEGACY_KEY = Collections.unmodifiableMap(map);
    }

    private TableMaterials() {
    }

    public static Map<String, Preset> legacyPresets() {
        return PRESETS_BY_LEGACY_KEY;
    }

    public static boolean isLegacyTable(BlockState state) {
        return state != null && state.getBlock() instanceof LegacyTableFlag;
    }

    public static @Nullable Preset legacyPreset(Block block) {
        if (block == null) {
            return null;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (!HotPot.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        return PRESETS_BY_LEGACY_KEY.get(id.getPath());
    }

    /** 检查按当前配置是否允许将 {@code state} 用作桌子或架子材质。 */
    public static boolean accepts(BlockState state) {
        return accepts(state, Config.tableMaterialsMode());
    }

    public static boolean accepts(BlockState state, String mode) {
        return acceptsWithTags(state, mode,
            state != null && safeIsTag(state, ALLOW_TAG),
            state != null && safeIsTag(state, DENY_TAG));
    }

    /** 供纯单元测试注入标签态的判定内核。 */
    public static boolean acceptsWithTags(BlockState state, String mode, boolean taggedAllow, boolean taggedDeny) {
        if (Config.TABLE_MAT_OFF.equals(mode) || state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        if (isExcludedOwnBlock(block) || taggedDeny) {
            return false;
        }
        if (taggedAllow) {
            return block.asItem() != Items.AIR;
        }
        if (Config.TABLE_MAT_TAG_ONLY.equals(mode)) {
            return false;
        }
        return state.getRenderShape() == RenderShape.MODEL
            && !(block instanceof EntityBlock)
            && state.isSolidRender()
            && state.getLightEmission() == 0
            && state.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO) >= 0.0F
            && block.asItem() != Items.AIR;
    }

    private static boolean safeIsTag(BlockState state, TagKey<Block> tag) {
        try {
            return state.is(tag);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isExcludedOwnBlock(Block block) {
        return block instanceof HotPotTableBlock
            || block instanceof StoveBlock
            || block instanceof StoveTopBlock
            || block instanceof TopProxyBlock
            || block instanceof HotPotBlock
            || block instanceof PlateHolderBlock
            || block instanceof CuttingBoardBlock
            || block instanceof SauceStationBlock
            || block instanceof BenchBlock
            || block instanceof DrinkCauldronBlock
            || block instanceof StoneMillBlock
            || block instanceof WoodenVatBlock
            || block instanceof HotPotCropBlock
            || block instanceof WildCropBlock
            || block instanceof OrangeLeavesBlock
            || block instanceof OrangeSaplingBlock
            || block instanceof PrepRackBlock;
    }

    public static String encodeBlock(Block block) {
        if (block == null || block == Blocks.AIR) {
            return "";
        }
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static @Nullable Block resolveBlock(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Identifier rl = Identifier.tryParse(id.trim());
        if (rl == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
        return block == Blocks.AIR ? null : block;
    }

    public static BlockState resolveDisplayState(String id, BlockState fallback) {
        Block resolved = resolveBlock(id);
        return resolved != null ? resolved.defaultBlockState() : fallback;
    }

    /** 固定硬度 2.0F、工具速度随材质走，空手依然可拆而不卡黑曜石。 */
    public static float destroyProgress(BlockState matState, Player player, BlockGetter world, BlockPos pos) {
        BlockState effective = matState == null || matState.isAir() ? Blocks.OAK_PLANKS.defaultBlockState() : matState;
        float speed = Math.max(1.0F, player.getDestroySpeed(effective));
        return speed / 2.0F / 30.0F;
    }

    public static BlockState primaryState(TableMaterialData data) {
        if (data != null && !data.top().isEmpty()) {
            return resolveDisplayState(data.top(), Blocks.OAK_PLANKS.defaultBlockState());
        }
        if (data != null && !data.base().isEmpty()) {
            return resolveDisplayState(data.base(), Blocks.BRICKS.defaultBlockState());
        }
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    /** 标记仍由旧存档过渡保留的 40 张旧预设桌类。 */
    public interface LegacyTableFlag {
    }
}
