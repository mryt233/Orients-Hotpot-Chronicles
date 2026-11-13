package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import；26.1 先判下面方块再查标签，避免静态期抛 Tags not bound。
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 桌面小摆件贴桌面：方块标签 {@code table_decorations} 里的方块直接放在火锅桌上时，
 * 外观和选中框整体挪到桌面上；挂着的灯笼或横向附着的不动。
 * 作者：mryt233
 */
public final class TableDecor {
    public static final TagKey<Block> TABLE_DECORATIONS =
        TagKey.create(Registries.BLOCK, HotPot.id("table_decorations"));

    private TableDecor() {
    }

    /** 这个方块该往上（正）/ 下（负）挪多少格；不用挪返回 0。 */
    public static double liftOf(BlockGetter world, BlockPos pos, BlockState state) {
        if (world == null) {
            return 0;
        }
        // mryt233 改（26.1）：先看下面是不是火锅桌，再查标签，避开 Blocks 静态初始化阶段标签未绑定的问题。
        Block below = world.getBlockState(pos.below()).getBlock();
        double lift;
        if (below instanceof TallHotPotTableBlock) {
            lift = (TallHotPotTableBlock.TABLE_TOP - 16) / 16.0;
        } else if (below instanceof HotPotTableBlock) {
            lift = (HotPotTableBlock.TABLE_TOP - 16) / 16.0;
        } else {
            return 0;
        }
        if (!state.is(TABLE_DECORATIONS)
            || (state.hasProperty(BlockStateProperties.HANGING) && state.getValue(BlockStateProperties.HANGING))
            || (state.hasProperty(BlockStateProperties.FACING) && state.getValue(BlockStateProperties.FACING) != Direction.UP)) {
            return 0;
        }
        return lift;
    }
}
