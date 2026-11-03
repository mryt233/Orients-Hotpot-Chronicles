package cn.blockforge.orient.framework.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/** 旧版原版玩家的方块交互距离。作者：mryt233 */
public final class PlayerCompat {
    private PlayerCompat() { }
    public static double blockInteractionRange(Player player) { return player.isCreative() ? 5.0 : 4.5; }
    public static boolean canInteractWithBlock(Player player, BlockPos pos, double extra) {
        double distance = blockInteractionRange(player) + extra;
        return new net.minecraft.world.phys.AABB(pos).distanceToSqr(player.getEyePosition()) < distance * distance;
    }
}
