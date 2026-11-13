// 作者：mryt233
// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 饮料的一口结算；瓶/杯的效果先后顺序和容器消耗各自保留。 */
public final class DrinkSip {
    private DrinkSip() { }

    static ItemStack finish(ItemStack stack, Level world, LivingEntity user, DrinkKind kind, Item item, boolean bottle) {
        if (kind == null) return stack;
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.GENERIC_DRINK,
            SoundSource.PLAYERS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        if (world.isClientSide()) return stack;
        // 杯先应用效果、瓶后应用；牛奶清除效果等时序不能交换。
        if (!bottle) kind.applySip(user);
        boolean cheered = !bottle && Boolean.TRUE.equals(stack.get(HotPotContent.CHEERED));
        if (user instanceof Player player) {
            if (bottle || kind.hasBottle()) {
                player.getFoodData().eat(DrinkItem.sipFood());
                if (bottle) player.awardStat(Stats.ITEM_USED.get(item));
                if (kind == DrinkKind.PLANT_PROTEIN_DRINK) HotPotCriterion.fire(player, "drink_author");
            }
            if (cheered) player.getFoodData().eat(Config.cheersBonusHunger(), Config.cheersBonusSaturation());
            if (!bottle) player.awardStat(Stats.ITEM_USED.get(item));
        }
        if (bottle) kind.applySip(user);
        if (user instanceof Player player && player.getAbilities().instabuild) return stack;
        if (!bottle) return new ItemStack(HotPotContent.GLASS_CUP);
        if (stack.getDamageValue() + 1 >= DrinkItem.SIPS) return new ItemStack(HotPotContent.emptyBottle(kind));
        stack.setDamageValue(stack.getDamageValue() + 1);
        return stack;
    }
}
