package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.item.ItemStack;

/** 筷子上夹着的那一样食物（物品组件 held_food，空的时候不带这个组件）。 */
public final class Chopsticks {
    private Chopsticks() {
    }

    public static ItemStack held(ItemStack chopsticks) {
        HeldFood held = cn.blockforge.orient.framework.compat.ItemData.get(chopsticks, HotPotContent.HELD_FOOD);
        return held == null ? ItemStack.EMPTY : held.stack();
    }

    public static void hold(ItemStack chopsticks, ItemStack food) {
        if (food.isEmpty()) {
            cn.blockforge.orient.framework.compat.ItemData.remove(chopsticks, HotPotContent.HELD_FOOD);
        } else {
            cn.blockforge.orient.framework.compat.ItemData.set(chopsticks, HotPotContent.HELD_FOOD, HeldFood.of(food));
        }
        PlateItem.syncFoodModel(chopsticks, !food.isEmpty());
    }
}
