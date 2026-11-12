// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 可摆上桌的物品接口：声明物品对应的桌面占位方块、是否自动摆放、是否整件保留、上桌前规整与摆放音效。
 */
public interface TableItem {
    /** 摆进 {@link PlateHolderBlock} 时使用的占位方块。 */
    PlateBlock tableBlock(ItemStack stack);

    /** 右键已存在的盘子格或倒盘进锅时，是否按常规桌面餐具自动参与摆放与分流。 */
    default boolean autoPlace(ItemStack stack) {
        return true;
    }

    /** 摆上桌后是否把物品栈整件保留在 {@link PlateHolderBlockEntity.Placed#self} 里。 */
    default boolean keepsSelf(ItemStack stack) {
        return true;
    }

    /** 摆上桌前对物品栈做无副作用转换，避免把非法状态带进桌面存储。 */
    default ItemStack prepareForTable(ItemStack stack) {
        return stack;
    }

    /** 新建桌面格子或常规补摆上桌时播放的音效。 */
    default void playPlaceSound(Level world, BlockPos pos, ItemStack stack) {
        PlateBlock block = tableBlock(stack);
        if (block != null) {
            world.playSound(null, pos, block.defaultBlockState().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** 已有 {@link PlateHolderBlock} 格内直接摆入时的音效（默认复用 {@link #playPlaceSound}）。 */
    default void playAddSound(Level world, BlockPos pos, ItemStack stack) {
        playPlaceSound(world, pos, stack);
    }
}
