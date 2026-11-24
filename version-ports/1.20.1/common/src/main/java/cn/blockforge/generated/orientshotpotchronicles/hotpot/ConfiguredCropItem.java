// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;

/**
 * 既是可食用食材、又是可直接下地播种的作物种子物品（1.20.1 适配）。
 * <p>继承 {@link ItemNameBlockItem} 保留原有 {@code item.<ns>.<id>} 本地化键与既有物品 ID，
 * 并通过 {@link #getFoodProperties()} 现读 {@code food.<id>} 数值配置。</p>
 * 作者：mryt233
 */
public class ConfiguredCropItem extends ItemNameBlockItem implements ConfigurableFood {
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
    public FoodProperties getFoodProperties() {
        return ConfiguredFoodItem.buildFood(foodId, super.getFoodProperties());
    }
}
