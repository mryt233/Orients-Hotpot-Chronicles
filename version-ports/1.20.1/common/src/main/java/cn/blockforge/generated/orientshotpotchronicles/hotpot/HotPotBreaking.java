package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家挖方块之前的「只挖一部分」：对着灶上的锅只挖锅、盘子格只挖准星对着的那个盘子、砧板只挖上面的东西、
 * 高灶台上半截对着锅也只挖锅。返回 true = 已经处理，平台事件要取消原版破坏。
 * <p>以前 Fabric（PlayerBlockBreakEvents.BEFORE）和 NeoForge（BlockEvent.BreakEvent）各写一份，
 * NeoForge 那份开头「没有方块实体就返回」，让没有方块实体的高灶台上半截分支永远走不到：
 * 站在上半截对着锅挖会把整个灶台挖掉（mryt233 2026-11-01，代码检查 W1 / B9）。现在两边都调这里。
 * 作者：mryt233
 */
public final class HotPotBreaking {
    private HotPotBreaking() {
    }

    public static boolean redirect(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        if (player == null) {
            return false;
        }
        // 砧板：对着板上的东西挖只挖东西，砧板留着（0.1.2-alpha 后用户报）
        if (be instanceof CuttingBoardBlockEntity board && !board.item().isEmpty()
            && CuttingBoardBlock.aimsAtItem(pos, player)) {
            CuttingBoardBlock.breakItem(world, pos, board, player);
            return true;
        }
        // 灶上有锅：对着锅挖只挖下锅（连汤带菜打包掉落），灶台留着；对着灶身挖才挖整个灶台
        if (be instanceof StoveBlockEntity stove && StoveBlock.aimsAtPot(state, pos, player)) {
            stove.breakPot(player);
            return true;
        }
        // 盘子格：只挖准星对着的那一个盘子（连菜掉出来），别的盘子留着（r87）
        if (be instanceof PlateHolderBlockEntity holder) {
            PlateHolderBlock.breakOne(world, pos, holder, player);
            return true;
        }
        // 高灶台的「上半截」（没有方块实体）：对着锅挖同样只挖锅
        if (state.is(HotPotContent.STOVE_TOP) && world.getBlockEntity(pos.below()) instanceof StoveBlockEntity stove
            && StoveBlock.aimsAtPot(world.getBlockState(pos.below()), pos.below(), player)) {
            stove.breakPot(player);
            return true;
        }
        return false;
    }
}
