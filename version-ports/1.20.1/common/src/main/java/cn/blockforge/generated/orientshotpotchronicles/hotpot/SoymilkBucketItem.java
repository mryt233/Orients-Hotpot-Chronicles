// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 豆浆桶：石磨磨豆产出，可倒入开口大木桶用于点卤压制豆腐，也可直接饮用并返还空桶（1.20.1 适配）。
 * 作者：mryt233
 */
public class SoymilkBucketItem extends Item {
    private static final int DRINK_DURATION = 32;
    private static final FoodProperties NUTRITION = new FoodProperties.Builder()
        .nutrition(2).saturationMod(0.25F).alwaysEat().build();

    public SoymilkBucketItem() {
        super(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (user instanceof Player player) {
            player.getFoodData().eat(NUTRITION.getNutrition(), NUTRITION.getSaturationModifier());
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET), false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
