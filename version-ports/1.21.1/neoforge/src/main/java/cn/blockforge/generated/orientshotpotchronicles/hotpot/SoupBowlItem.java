package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * 一碗汤（r99，用户要求）：拿碗点有汤的锅盛出来（锅里的汤不会少），能喝、能像盘子一样摆在桌上。
 * 四种都回 4 点饥饿值；清汤解反胃，高汤饱和度高一些，菌汤 6 秒生命恢复 I，红汤 9 秒力量 I + 3 秒饥饿（用户定的）。
 * 喝完还一只空碗（同原版蘑菇煲）。
 *
 */
public class SoupBowlItem extends Item {
    private final SoupBase soup;

    public SoupBowlItem(SoupBase soup) {
        super(new Item.Settings().maxCount(1).food(foodOf(soup)));
        this.soup = soup;
    }

    private static FoodComponent foodOf(SoupBase soup) {
        return new FoodComponent.Builder()
            .nutrition(4)
            .saturationModifier(soup == SoupBase.RICH_STOCK || soup == SoupBase.THREE_FRESH ? 0.9F : 0.6F)
            .usingConvertsTo(Items.BOWL)
            .build();
    }

    public SoupBase soup() {
        return soup;
    }

    /** 点在方块顶面：像盘子一样摆上去；别的情况交给原版（对着空气 / 侧面右键就是喝）。 */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return PlateItem.placeOn(context, HotPotContent.BOWL_BLOCK, true);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient) {
            if (soup == SoupBase.CLEAR || soup == SoupBase.TOMATO) {
                user.removeStatusEffect(StatusEffects.NAUSEA);
            }
            // 菌汤 / 红汤的效果和 SoupBase.dishBonusEffects 里的一样；清汤、高汤按用户定的（不用那里的饱和 / 伤害吸收）
            if (soup == SoupBase.MUSHROOM || soup == SoupBase.SPICY || soup == SoupBase.TOMATO || soup == SoupBase.THREE_FRESH) {
                for (net.minecraft.entity.effect.StatusEffectInstance effect : soup.dishBonusEffects()) {
                    user.addStatusEffect(effect);
                }
            }
        }
        return result;
    }
}
