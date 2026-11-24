package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 数值可变的食物（对齐 mod2 的做法）。
 * 饥饿值和饱和度在<b>吃东西的那一刻</b>才从配置里读，所以改配置文件后
 * {@code /orientshotpot reload} 就能生效，不用重启游戏。
 * 作者：mryt233；mryt233 改（2026-11-16）：接入 {@link ConfigurableFood} 供作物种子食物复用。
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

    /** 配置里的营养值；物品注册的组件保持默认值，客户端与服务端显示一致。 */
    public static FoodProperties food(ItemStack stack) {
        FoodProperties original = stack.get(DataComponents.FOOD);
        if (original == null || !(stack.getItem() instanceof ConfigurableFood item)) {
            return original;
        }
        // 自定义栈组件优先，不覆盖数据包/其他模组单独设置的效果和营养。
        FoodProperties base = stack.getItem().components().get(DataComponents.FOOD);
        if (!original.equals(base)) {
            return original;
        }
        int nutrition = Config.foodHunger(item.foodId(), original.nutrition());
        float defaultModifier = original.nutrition() == 0 ? 0 : original.saturation() / (2F * original.nutrition());
        float modifier = Config.foodSaturation(item.foodId(), defaultModifier);
        return new FoodProperties(nutrition, 2F * nutrition * modifier, original.canAlwaysEat());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        return finishConfiguredFood(stack, world, user, () -> super.finishUsingItem(stack, world, user));
    }

    static ItemStack finishConfiguredFood(ItemStack stack, Level world, LivingEntity user,
                                            java.util.function.Supplier<ItemStack> fallback) {
        FoodProperties adjusted = food(stack);
        if (adjusted == null) {
            return fallback.get();
        }
        FoodProperties original = stack.get(DataComponents.FOOD);
        stack.set(DataComponents.FOOD, adjusted);
        try {
            return fallback.get();
        } finally {
            stack.set(DataComponents.FOOD, original);
        }
    }
}
