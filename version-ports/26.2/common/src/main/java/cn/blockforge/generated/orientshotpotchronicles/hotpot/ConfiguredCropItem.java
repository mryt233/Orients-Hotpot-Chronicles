// 作者：mryt233；mryt233 改：26.2 BlockItem（使用物品自身译名键）与 FOOD 组件适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 既是可食用食材、又是可直接下地播种的作物种子物品。
 * <p>继承 {@link BlockItem} 且不调用 {@code useBlockDescriptionPrefix()}，保留原有 {@code item.<ns>.<id>} 本地化键与既有物品 ID，
 * 并通过 {@link ConfiguredFoodItem#food(ItemStack)} 热读取 {@code food.<id>} 数值配置。</p>
 */
public class ConfiguredCropItem extends BlockItem implements ConfigurableFood {
    private final String foodId;

    public ConfiguredCropItem(Block block, String foodId, Item.Properties settings) {
        super(block, settings);
        this.foodId = foodId;
    }

    @Override
    public String foodId() {
        return foodId;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        FoodProperties adjusted = ConfiguredFoodItem.food(stack);
        if (adjusted == null) {
            return stack;
        }
        stack.set(DataComponents.FOOD, adjusted);
        return super.finishUsingItem(stack, world, user);
    }
}
