// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

/**
 * 一碗汤（r99，用户要求）：拿碗点有汤的锅盛出来（锅里的汤不会少），能喝、能像盘子一样摆在桌上。
 * 四种都回 4 点饥饿值；清汤解反胃，高汤饱和度高一些，菌汤 6 秒生命恢复 I，红汤 9 秒力量 I + 3 秒饥饿（用户定的）。
 * 喝完还一只空碗（同原版蘑菇煲）。
 * 作者：mryt233 */
public class SoupBowlItem extends Item {
    private final SoupBase soup;

    public SoupBowlItem(SoupBase soup, Item.Properties properties) {
        super(properties.stacksTo(1).food(foodOf(soup)).usingConvertsTo(Items.BOWL));
        this.soup = soup;
    }

    private static FoodProperties foodOf(SoupBase soup) {
        return new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(soup == SoupBase.RICH_STOCK || soup == SoupBase.THREE_FRESH ? 0.9F : 0.6F)
            .alwaysEdible()   // 饱了也能喝（同本模组其他食物，mryt233 改 2026-11-01）
            .build();
    }

    public SoupBase soup() {
        return soup;
    }

    /** 点在方块顶面：像盘子一样摆上去；别的情况交给原版（对着空气 / 侧面右键就是喝）。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return PlateItem.placeOn(context, HotPotContent.BOWL_BLOCK, true);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, world, user);
        if (!world.isClientSide()) {
            if (soup == SoupBase.CLEAR || soup == SoupBase.TOMATO) {
                user.removeEffect(MobEffects.NAUSEA);
            }
            // 菌汤 / 红汤的效果和 SoupBase.dishBonusEffects 里的一样；清汤、高汤按用户定的（不用那里的饱和 / 伤害吸收）
            if (soup == SoupBase.MUSHROOM || soup == SoupBase.SPICY || soup == SoupBase.TOMATO || soup == SoupBase.THREE_FRESH) {
                for (net.minecraft.world.effect.MobEffectInstance effect : soup.dishBonusEffects()) {
                    user.addEffect(effect);
                }
            }
        }
        return result;
    }
}
