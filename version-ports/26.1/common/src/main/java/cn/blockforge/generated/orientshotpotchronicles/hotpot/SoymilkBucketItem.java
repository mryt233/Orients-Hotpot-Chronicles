// 作者：mryt233；mryt233 改（2026-11-16）：适配 26.1 Item.Properties 与 Consumables。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;

/**
 * 豆浆桶：石磨磨豆产出，可倒入开口大木桶用于点卤压制豆腐，也可直接饮用并返还空桶。
 * 作者：mryt233
 */
public class SoymilkBucketItem extends Item {
    private static final FoodProperties NUTRITION = new FoodProperties.Builder()
        .nutrition(2).saturationModifier(0.25F).alwaysEdible().build();

    public SoymilkBucketItem(Item.Properties properties) {
        super(properties
            .stacksTo(1)
            .craftRemainder(Items.BUCKET)
            .food(NUTRITION)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
            .usingConvertsTo(Items.BUCKET));
    }
}
