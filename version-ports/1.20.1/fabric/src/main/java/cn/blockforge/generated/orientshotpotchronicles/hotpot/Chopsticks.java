// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.ItemStack;

/** 筷子上夹着的那一样食物（物品组件 held_food，空的时候不带这个组件）。 */
public final class Chopsticks {
    private Chopsticks() {
    }

    public static ItemStack held(ItemStack chopsticks) {
        return ItemData.heldFood(chopsticks);   // 1.20.1 移植：存 NBT（Claude 改）
    }

    public static void hold(ItemStack chopsticks, ItemStack food) {
        ItemData.setHeldFood(chopsticks, food);
        PlateItem.syncFoodModel(chopsticks, !food.isEmpty());
    }
}
