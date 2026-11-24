// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * 0.3.1 旧 40 种火锅桌向 2 种统一火锅桌的幂等迁移工具：
 * <ul>
 *     <li>区块加载时通过调色板预判仅扫描含旧桌的 16³ 小节，就地转成新方块并保留预设材质与旧锁定标记；</li>
 *     <li>玩家登录时检查背包将旧桌子物品转为带 {@link TableMaterialData} 组件的新桌子物品；</li>
 *     <li>支持旧存档重开，旧预设不可拆出退还方块以杜绝刷物。</li>
 * </ul>
 * 作者：mryt233
 */
public final class TableMigration {
    private TableMigration() {
    }

    /** 将世界区块内遗留的旧 40 种火锅桌无损迁移成 {@code hotpot_table} / {@code tall_hotpot_table}。 */
    public static int migrateChunk(ServerLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) {
            return 0;
        }
        int converted = 0;
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(TableMaterials::isLegacyTable)) {
                continue;
            }
            int sectionY = (minSectionY + s) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState oldState = section.getBlockState(x, y, z);
                        if (!TableMaterials.isLegacyTable(oldState)) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(baseX + x, sectionY + y, baseZ + z);
                        if (upgradeBlock(level, pos, oldState)) {
                            converted++;
                        }
                    }
                }
            }
        }
        if (converted > 0) {
            chunk.markUnsaved();
            HotPot.LOGGER.info("[hotpot] 已在区块 ({}, {}) 自动升级 {} 张旧版火锅桌",
                chunk.getPos().x(), chunk.getPos().z(), converted);
        }
        return converted;
    }

    /** 将单个旧版火锅桌方块原地升级为统一桌子并写入不可拆预设材质；返回是否发生转换。 */
    public static boolean upgradeBlock(Level level, BlockPos pos, BlockState oldState) {
        if (level == null || level.isClientSide() || !TableMaterials.isLegacyTable(oldState)) {
            return false;
        }
        TableMaterials.Preset preset = TableMaterials.legacyPreset(oldState.getBlock());
        boolean tall = oldState.getBlock() instanceof TallHotPotTableBlock;
        BlockState next = (tall ? HotPotContent.TALL_HOTPOT_TABLE : HotPotContent.HOTPOT_TABLE).defaultBlockState()
            .setValue(HotPotTableBlock.NORTH, oldState.getValue(HotPotTableBlock.NORTH))
            .setValue(HotPotTableBlock.EAST, oldState.getValue(HotPotTableBlock.EAST))
            .setValue(HotPotTableBlock.SOUTH, oldState.getValue(HotPotTableBlock.SOUTH))
            .setValue(HotPotTableBlock.WEST, oldState.getValue(HotPotTableBlock.WEST));
        if (tall) {
            next = next.setValue(TallHotPotTableBlock.OPEN, oldState.getValue(TallHotPotTableBlock.OPEN));
        }
        // UPDATE_KNOWN_SHAPE 避免触发上方盘架与邻接桌子断连或反复重算。
        level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (preset != null && level.getBlockEntity(pos) instanceof HotPotTableBlockEntity be) {
            be.setMaterials(TableMaterialData.fromLegacyPreset(preset, tall));
        }
        return true;
    }

    /** 玩家登录时将背包中旧 40 种火锅桌物品转换成带预设组件的统一桌子物品（保留总数与旧锁标识）。 */
    public static int migrateInventory(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        Inventory inv = player.getInventory();
        int convertedStacks = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            ItemStack migrated = convertItemStack(stack);
            if (migrated != stack) {
                inv.setItem(i, migrated);
                convertedStacks++;
            }
        }
        if (convertedStacks > 0) {
            inv.setChanged();
        }
        return convertedStacks;
    }

    public static ItemStack convertItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return stack;
        }
        Block block = blockItem.getBlock();
        if (!(block instanceof TableMaterials.LegacyTableFlag)) {
            return stack;
        }
        TableMaterials.Preset preset = TableMaterials.legacyPreset(block);
        boolean tall = block instanceof TallHotPotTableBlock;
        ItemStack out = new ItemStack(
            tall ? HotPotContent.TALL_HOTPOT_TABLE_ITEM : HotPotContent.HOTPOT_TABLE_ITEM,
            stack.getCount());
        if (preset != null) {
            out.set(HotPotContent.TABLE_MATERIALS, TableMaterialData.fromLegacyPreset(preset, tall));
        }
        return out;
    }
}
