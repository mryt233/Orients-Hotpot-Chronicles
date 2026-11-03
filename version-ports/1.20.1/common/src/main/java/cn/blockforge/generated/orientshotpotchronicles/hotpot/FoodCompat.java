package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.SuspiciousStewItem;

/** 旧版食物没有 usingConvertsTo，保持可夹取食物原有的容器返还。作者：mryt233 */
final class FoodCompat {
    private FoodCompat() { }
    static void returnContainer(Player player, ItemStack eaten) {
        if (player.getAbilities().instabuild) return;
        ItemStack remainder = ItemStack.EMPTY;
        if (eaten.getItem() instanceof BowlFoodItem || eaten.getItem() instanceof SuspiciousStewItem
            || eaten.getItem() instanceof SoupBowlItem) remainder = new ItemStack(Items.BOWL);
        else if (eaten.is(Items.HONEY_BOTTLE)) remainder = new ItemStack(Items.GLASS_BOTTLE);
        else if (eaten.getItem().hasCraftingRemainingItem()) remainder = new ItemStack(eaten.getItem().getCraftingRemainingItem());
        if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
    }
}
