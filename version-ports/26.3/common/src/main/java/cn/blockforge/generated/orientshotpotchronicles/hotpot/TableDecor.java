package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/**
 * 桌面小摆件贴桌面（用户要求，只挪小摆件）：MC 方块只能放整格，矮桌桌面低于整格 5 像素（摆件浮空）、
 * 高桌桌面高出整格 7 像素（摆件嵌进桌面）。方块标签 {@code table_decorations}（灯笼、花盆、蜡烛、蛋糕……）
 * 里的方块直接放在火锅桌上时，外观和选中框整体挪到桌面上；挂着的灯笼不动。
 * 碰撞不挪（挪了会伸出本格，原版缓存的形状对不上会让站上去的人抽搐，同盘子那次）。
 * 别的模组 / 数据包可以往这个标签里加方块。
 * 作者：mryt233 */
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
        // mryt233 改（26.1）：先看下面是不是火锅桌，再查标签。原版在 Blocks 初始化形状缓存时就会调到这里，
        // 那时标签还没绑定，26.1 的 is(标签) 会直接抛「Tags not bound」（1.21.1 只是返回 false）。
        // 初始化用的是空世界，下面不会是火锅桌，先判断它就走不到查标签；行为和原来一样。
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
            || (state.hasProperty(BlockStateProperties.FACING) && state.getValue(BlockStateProperties.FACING) != net.minecraft.core.Direction.UP)) {
            // 挂着的灯笼、横着贴在旁边的末地烛 / 紫水晶不是放在桌面上的，不挪
            return 0;
        }
        return lift;
    }
}
