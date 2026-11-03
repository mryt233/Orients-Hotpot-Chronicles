package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 消费时通过原版接口读取热重载后的食物数值。作者：mryt233 */
public class ConfiguredFoodItem extends Item {
    private final String foodId;
    public ConfiguredFoodItem(String foodId, Item.Properties settings) {
        super(settings);
        this.foodId = foodId;
    }
    public static FoodProperties food(ItemStack stack) { return stack.getItem().getFoodProperties(); }
    @Override public FoodProperties getFoodProperties() {
        FoodProperties original = super.getFoodProperties();
        if (original == null) return null;
        FoodProperties.Builder builder = new FoodProperties.Builder()
            .nutrition(Config.foodHunger(foodId, original.getNutrition()))
            .saturationMod(Config.foodSaturation(foodId, original.getSaturationModifier())).alwaysEat();
        if (original.isMeat()) builder.meat();
        if (original.isFastFood()) builder.fast();
        for (var effect : original.getEffects()) builder.effect(effect.getFirst(), effect.getSecond());
        return builder.build();
    }
}
