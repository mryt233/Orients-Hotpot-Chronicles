package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 数值可变的食物（对齐 mod2 的做法）。
 * 饥饿值和饱和度在<b>吃东西的那一刻</b>才从配置里读，所以改配置文件后
 * {@code /orientshotpot reload} 就能生效，不用重启游戏。
 * 注册时写进物品组件的数值只作为默认值和客户端显示用；其他字段（能不能随时吃、吃多快、
 * 吃完留下什么、附加效果）本模组的食物都没用到，这里只重建营养和饱和度两项。
 * 作者：mryt233 */
public class ConfiguredFoodItem extends Item {
    private final String foodId;

    public ConfiguredFoodItem(String foodId, Item.Properties settings) {
        super(settings);
        this.foodId = foodId;
    }

    /** 配置里的营养值；物品注册的组件保持默认值，客户端与服务端显示一致。 */
    public static FoodProperties food(ItemStack stack) {
        FoodProperties original = stack.get(DataComponents.FOOD);
        if (original == null || !(stack.getItem() instanceof ConfiguredFoodItem item)) {
            return original;
        }
        // 自定义栈组件优先，不覆盖数据包/其他模组单独设置的效果和营养。
        FoodProperties base = stack.getItem().components().get(DataComponents.FOOD);
        if (!original.equals(base)) {
            return original;
        }
        // Yarn 1.21.1 的 saturation 是实际饱和点数（构建器内部乘了 2×营养），不是系数。
        int nutrition = Config.foodHunger(item.foodId, original.nutrition());
        float defaultModifier = original.nutrition() == 0 ? 0 : original.saturation() / (2F * original.nutrition());
        float modifier = Config.foodSaturation(item.foodId, defaultModifier);
        return new FoodProperties(nutrition, 2F * nutrition * modifier, original.canAlwaysEat());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        FoodProperties adjusted = food(stack);
        if (adjusted == null) {
            return super.finishUsingItem(stack, world, user);
        }
        FoodProperties original = stack.get(DataComponents.FOOD);
        stack.set(DataComponents.FOOD, adjusted);
        try {
            return super.finishUsingItem(stack, world, user);
        } finally {
            stack.set(DataComponents.FOOD, original);
        }
    }
}