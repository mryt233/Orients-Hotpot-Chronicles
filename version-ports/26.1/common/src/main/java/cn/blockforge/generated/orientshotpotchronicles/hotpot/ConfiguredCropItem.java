// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 既是可食用食材、又是可直接下地播种的作物种子物品。
 * <p>26.1：通过 {@code Item.Properties#useItemDescriptionPrefix()} 保留 {@code item.<ns>.<id>} 本地化键，
 * 并与 {@link ConfiguredFoodItem} 共用配置读取。</p>
 */
public class ConfiguredCropItem extends BlockItem implements ConfigurableFood {
    private final String foodId;

    public ConfiguredCropItem(Block block, String foodId, Item.Properties settings) {
        super(block, settings.useItemDescriptionPrefix());
        this.foodId = foodId;
    }

    @Override
    public String foodId() {
        return foodId;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        return ConfiguredFoodItem.finishConfiguredFood(stack, world, user,
            () -> super.finishUsingItem(stack, world, user));
    }
}
