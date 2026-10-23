// 0.1.2 的 1.20.1 Forge 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
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
 * 四种都回 4 点饥饿值；清汤解反胃，浓汤饱和度高一些，菌汤 6 秒生命恢复 I，红汤 9 秒力量 I + 3 秒饥饿（用户定的）。
 * 喝完还一只空碗（同原版蘑菇煲）。
 * 作者：Claude
 */
public class SoupBowlItem extends Item {
    private final SoupBase soup;

    public SoupBowlItem(SoupBase soup) {
        super(new Item.Settings().maxCount(1).food(foodOf(soup)));
        this.soup = soup;
    }

    private static FoodComponent foodOf(SoupBase soup) {
        return new FoodComponent.Builder()
            .hunger(4)
            .saturationModifier(soup == SoupBase.MILKY ? 0.9F : 0.6F)
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
            if (soup == SoupBase.CLEAR) {
                user.removeStatusEffect(StatusEffects.NAUSEA);
            }
            // 菌汤 / 红汤的效果和 SoupBase.dishBonusEffects 里的一样；清汤、浓汤按用户定的（不用那里的饱和 / 伤害吸收）
            if (soup == SoupBase.MUSHROOM || soup == SoupBase.SPICY) {
                for (net.minecraft.entity.effect.StatusEffectInstance effect : soup.dishBonusEffects()) {
                    user.addStatusEffect(effect);
                }
            }
        }
        // 1.20.1 移植（Claude 改）：1.20 的食物没有 usingConvertsTo，喝完还碗照原版蘑菇煲写
        if (user instanceof PlayerEntity player && player.getAbilities().creativeMode) {
            return result;
        }
        if (result.isEmpty()) {
            return new ItemStack(Items.BOWL);
        }
        if (user instanceof PlayerEntity player && !player.getInventory().insertStack(new ItemStack(Items.BOWL))) {
            player.dropItem(new ItemStack(Items.BOWL), false);
        }
        return result;
    }
}
