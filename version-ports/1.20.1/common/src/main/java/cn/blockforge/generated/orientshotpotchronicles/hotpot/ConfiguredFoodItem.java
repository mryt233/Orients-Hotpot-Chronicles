package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 数值可变的食物（1.20.1 适配）。
 * 消费时通过原版 {@link #getFoodProperties()} 读取热重载后的食物数值；同时实现 {@link ConfigurableFood}，
 * 供 {@link #food(ItemStack)} 与创造栏分类等共享配置路径。
 * 作者：mryt233
 */
public class ConfiguredFoodItem extends Item implements ConfigurableFood {
    private final String foodId;

    public ConfiguredFoodItem(String foodId, Item.Properties settings) {
        super(settings);
        this.foodId = foodId;
    }

    @Override
    public String foodId() {
        return foodId;
    }

    public static FoodProperties food(ItemStack stack) {
        return stack.getItem().getFoodProperties();
    }

    @Override
    public FoodProperties getFoodProperties() {
        return buildFood(foodId, super.getFoodProperties());
    }

    /** 根据当前配置重建食物饥饿与饱和度属性，供普通食物与可食用作物共享。 */
    public static FoodProperties buildFood(String foodId, FoodProperties original) {
        if (original == null) {
            return null;
        }
        FoodProperties.Builder builder = new FoodProperties.Builder()
            .nutrition(Config.foodHunger(foodId, original.getNutrition()))
            .saturationMod(Config.foodSaturation(foodId, original.getSaturationModifier()));
        if (original.canAlwaysEat()) {
            builder.alwaysEat();
        }
        if (original.isMeat()) {
            builder.meat();
        }
        if (original.isFastFood()) {
            builder.fast();
        }
        for (var effect : original.getEffects()) {
            builder.effect(effect.getFirst(), effect.getSecond());
        }
        return builder.build();
    }
}
