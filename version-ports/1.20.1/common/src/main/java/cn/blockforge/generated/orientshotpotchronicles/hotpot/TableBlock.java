// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.compat.DataComponents;
import cn.blockforge.orient.framework.compat.ItemContainerContents;
import cn.blockforge.orient.framework.compat.ItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 桌面占位方块接口：统一描述摆在 {@link PlateHolderBlock} 中的物件高度、模型、粒子、回收物品、加载初始化与交互。
 */
public interface TableBlock {
    /** 摆在桌上的这一件能否往里放菜 / 从里夹菜。 */
    default boolean holdsFood(PlateHolderBlockEntity.Placed placed) {
        return placed.self.isEmpty();
    }

    /** 摆在桌上的这一件能否把筷子架在沿上。 */
    default boolean restsChopsticks(PlateHolderBlockEntity.Placed placed) {
        return true;
    }

    /** 摆在桌上的这一件有多高（像素），供选中框、碰撞箱与架筷高度使用。 */
    default double tableHeight(PlateHolderBlockEntity.Placed placed) {
        return ((PlateBlock) this).size().height();
    }

    /** 渲染与默认碎屑粒子使用的方块状态。 */
    default BlockState modelState(PlateHolderBlockEntity.Placed placed) {
        return ((PlateBlock) this).defaultBlockState().setValue(PlateBlock.AXIS, placed.axis);
    }

    /** 挖掉这件桌面物件时用于生成破坏粒子的方块状态。 */
    default BlockState breakParticleState(PlateHolderBlockEntity.Placed placed) {
        return modelState(placed);
    }

    /** 拿起来或挖掉时变回手里的物品栈。 */
    default ItemStack toTableItem(PlateHolderBlockEntity.Placed placed) {
        return placed.self.isEmpty() ? containerItem((PlateBlock) this, placed) : placed.self.copy();
    }

    /** 新摆上桌或从存档读出时同步与摆放朝向相关的状态。 */
    default void onLoadedOrAdded(PlateHolderBlockEntity.Placed placed, Direction facing) {
    }

    /** 拿着物品右键已摆在桌上的这件物件时的专属交互；未命中则交回 {@link UseOutcome#PASS}。 */
    default UseOutcome useOnPlaced(PlateHolderBlockEntity.Placed target, Level world, BlockPos pos,
                                   PlateHolderBlockEntity be, Player player, InteractionHand hand, ItemStack stack) {
        return UseOutcome.PASS;
    }

    /** 把盘 / 碗里盛着的菜打包成带容器组件和对应模型的物品栈。 */
    static ItemStack containerItem(ItemLike item, PlateHolderBlockEntity.Placed placed) {
        ItemStack stack = new ItemStack(item);
        if (placed.items.stream().anyMatch(s -> !s.isEmpty())) {
            ItemData.set(stack, DataComponents.CONTAINER, ItemContainerContents.fromItems(placed.items));
        }
        PlateItem.syncPlateModel(stack);
        return stack;
    }
}
