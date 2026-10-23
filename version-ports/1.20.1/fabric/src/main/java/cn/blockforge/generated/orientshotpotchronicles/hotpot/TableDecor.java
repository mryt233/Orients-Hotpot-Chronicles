package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.block.Block;

/**
 * 桌面小摆件贴桌面（用户要求，只挪小摆件）：MC 方块只能放整格，矮桌桌面低于整格 5 像素（摆件浮空）、
 * 高桌桌面高出整格 7 像素（摆件嵌进桌面）。方块标签 {@code table_decorations}（灯笼、花盆、蜡烛、蛋糕……）
 * 里的方块直接放在火锅桌上时，外观和选中框整体挪到桌面上；挂着的灯笼不动。
 * 碰撞不挪（挪了会伸出本格，原版缓存的形状对不上会让站上去的人抽搐，同盘子那次）。
 * 别的模组 / 数据包可以往这个标签里加方块。
 *
 */
public final class TableDecor {
    public static final TagKey<Block> TABLE_DECORATIONS =
        TagKey.of(RegistryKeys.BLOCK, GeneratedMod.id("table_decorations"));

    private TableDecor() {
    }

    /** 这个方块该往上（正）/ 下（负）挪多少格；不用挪返回 0。 */
    public static double liftOf(BlockView world, BlockPos pos, BlockState state) {
        if (world == null || !state.isIn(TABLE_DECORATIONS)
            || (state.contains(Properties.HANGING) && state.get(Properties.HANGING))
            || (state.contains(Properties.FACING) && state.get(Properties.FACING) != net.minecraft.util.math.Direction.UP)) {
            // 挂着的灯笼、横着贴在旁边的末地烛 / 紫水晶不是放在桌面上的，不挪
            return 0;
        }
        Block below = world.getBlockState(pos.down()).getBlock();
        if (below instanceof TallHotPotTableBlock) {
            return (TallHotPotTableBlock.TABLE_TOP - 16) / 16.0;
        }
        if (below instanceof HotPotTableBlock) {
            return (HotPotTableBlock.TABLE_TOP - 16) / 16.0;
        }
        return 0;
    }
}
