package cn.blockforge.orient.framework.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 创造模式不扣物品，和新版 consume 一致。作者：mryt233 */
public final class ItemStackCompat {
    private ItemStackCompat() { }
    public static void consume(ItemStack stack, int count, Player player) {
        if (player == null || !player.getAbilities().instabuild) stack.shrink(count);
    }
}
