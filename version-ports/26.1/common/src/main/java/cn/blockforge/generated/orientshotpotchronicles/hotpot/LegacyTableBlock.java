// 作者：mryt233；mryt233 改（2026-11-16）：适配 26.1 BlockBehaviour.Properties 与 Level.isClientSide()。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 为旧 40 种预设矮火锅桌 ID 保留的兼容占位方块类，放置时立即原地升级为统一 {@code hotpot_table}。
 * 作者：mryt233
 */
public class LegacyTableBlock extends HotPotTableBlock implements TableMaterials.LegacyTableFlag {
    public LegacyTableBlock(Block like, boolean hardSurface, BlockBehaviour.Properties properties) {
        super(like, hardSurface, false, properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide()) {
            TableMigration.upgradeBlock(world, pos, world.getBlockState(pos));
        }
    }
}
