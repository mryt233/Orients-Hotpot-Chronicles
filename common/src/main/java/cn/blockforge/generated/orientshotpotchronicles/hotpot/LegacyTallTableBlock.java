// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 为旧 40 种预设高火锅桌 ID 保留的兼容占位方块类，放置时立即原地升级为统一 {@code tall_hotpot_table}。
 * 作者：mryt233
 */
public class LegacyTallTableBlock extends TallHotPotTableBlock implements TableMaterials.LegacyTableFlag {
    public LegacyTallTableBlock(Block like, boolean hardSurface) {
        super(like, hardSurface, false);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            TableMigration.upgradeBlock(world, pos, world.getBlockState(pos));
        }
    }
}
