package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.ItemStack;

/** 筷子上夹着的那一样食物（物品组件 held_food，空的时候不带这个组件）。 */
public final class Chopsticks {
    private Chopsticks() {
    }

    public static ItemStack held(ItemStack chopsticks) {
        ItemStack held = chopsticks.get(HotPotContent.HELD_FOOD);
        return held == null ? ItemStack.EMPTY : held;
    }

    public static void hold(ItemStack chopsticks, ItemStack food) {
        if (food.isEmpty()) {
            chopsticks.remove(HotPotContent.HELD_FOOD);
        } else {
            chopsticks.set(HotPotContent.HELD_FOOD, food.copyWithCount(1));
        }
        PlateItem.syncFoodModel(chopsticks, !food.isEmpty());
    }
}
