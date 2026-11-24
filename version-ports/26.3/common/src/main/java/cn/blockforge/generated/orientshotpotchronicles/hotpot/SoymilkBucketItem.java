// 作者：mryt233；mryt233 改：26.2 饮食组件 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

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
        super(properties.stacksTo(1)
            .food(NUTRITION, Consumables.DEFAULT_DRINK)
            .usingConvertsTo(Items.BUCKET)
            .craftRemainder(Items.BUCKET));
    }
}
