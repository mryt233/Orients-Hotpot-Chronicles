// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

/** 筷子和锅界面进食共用原版消耗流程，营养在结算时读取。 */
final class FoodConsumption {
    private FoodConsumption() { }

    static void consume(Level level, LivingEntity user, ItemStack source, FoodProperties food) {
        ItemStack portion = source.copyWithCount(1);
        if (food != null) {
            portion.set(DataComponents.FOOD, food);
        }
        portion.getOrDefault(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).onConsume(level, user, portion);
    }

    static int consumeTicks(ItemStack source) {
        return source.getOrDefault(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD).consumeTicks();
    }

    static void returnRemainder(ItemStack source, Player player) {
        var remainder = source.get(DataComponents.USE_REMAINDER);
        if (remainder != null && !player.hasInfiniteMaterials()) {
            player.getInventory().placeItemBackInInventory(remainder.convertInto().create());
        }
    }
}
