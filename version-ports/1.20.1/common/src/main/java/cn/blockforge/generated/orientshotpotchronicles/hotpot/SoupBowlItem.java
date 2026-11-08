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
 * 营养和效果都能在配置文件 soup_bowl.&lt;汤底&gt; 里改（mryt233 改 2026-11-05）：
 * 默认回 4 点饥饿值，高汤、三鲜汤饱和度高一些。风味效果见 {@link SoupBase#dishBonusEffects(int)}
 * （mryt233 改 2026-11-06，清单 #125：清汤速度 + 饱和、高汤抗性 + 伤害吸收且不再缓慢、菌汤生命恢复、
 * 红汤力量 + 速度且特辣起加抗火与饥饿、番茄汤急迫、三鲜汤水下呼吸 + 幸运）；清汤、番茄汤喝下还解反胃。
 * 喝完还一只空碗（同原版蘑菇煲）。
 * mryt233 改（2026-11-05，清单 #116）：六种汤碗都按 SoupBase.dishBonusEffects(辣度) 结算，
 * 红汤碗还带着盛出时的辣度档位，3 级特辣喝完嘴上冒火（2 秒无伤火苗）。
 * <p>mryt233 改（0.2.2-alpha，1.20.1 适配）：
 * ① 饱和度用 {@code saturationMod(float)}、饱食可吃用 {@code alwaysEat()}；
 * ② 辣度与汤底标记走物品 NBT（ItemData / ItemDataKey），不是 1.21 的 DataComponentType；
 * ③ 1.20.1 没有 {@code LivingEntity#eat(Level, ItemStack, FoodProperties)} 这个三参重载，
 * 营养改成覆写 {@link #getFoodProperties()} 现读配置——和 {@link ConfiguredFoodItem} 同一套做法，
 * 改完配置 /orientshotpot reload 立即生效；
 * ④ 喝完留下的空碗仍由 {@link #finishUsingItem} 自己换（1.20.1 没有 {@code usingConvertsTo}）。
 * 作者：mryt233 */
public class SoupBowlItem extends Item {
    private final SoupBase soup;

    public SoupBowlItem(SoupBase soup) {
        super(new Item.Properties().stacksTo(1).food(foodOf(soup)));
        this.soup = soup;
    }

    /** 注册时的数值只作默认值和客户端显示用；真正的结算在 {@link #getFoodProperties()}。 */
    private static FoodProperties foodOf(SoupBase soup) {
        return new FoodProperties.Builder()
            .nutrition(SoupBase.DEFAULT_BOWL_HUNGER)
            .saturationMod(soup == SoupBase.RICH_STOCK || soup == SoupBase.THREE_FRESH ? 0.9F : 0.6F)
            .alwaysEat()   // 饱了也能喝（同本模组其他食物，mryt233 改 2026-11-01）
            .build();
    }

    /**
     * mryt233 改（2026-11-05，清单 #110）：喝的那一刻按配置现算营养（饥饿值 + 饱和度系数），
     * 所以改完 config/orients_hotpot_chronicles.json 执行 /orientshotpot reload 就能立即生效。
     * 饱了也能喝这一项跟注册时保持一致。
     */
    @Override
    public FoodProperties getFoodProperties() {
        return new FoodProperties.Builder()
            .nutrition(soup.bowlHunger())
            .saturationMod(soup.bowlSaturation())
            .alwaysEat()
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

    /**
     * mryt233 改（2026-11-07，清单 #116）：辣度必须在吃之前读。
     * <p>finishUsingItem 内部会把这碗吃到 0 个（物品栈变成空 / 换成空碗），此后再读辣度只能拿到兜底 0，
     * 红汤的等级加成、抗火，以及 3 级特辣喝完嘴上冒火都会静默失效。
     * 与 LivingEntityEatMixin 在 eat 的 HEAD 处读标记保持同一口径。</p>
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        // mryt233 改（2026-11-07）：先把辣度取成快照，再交给原版结算
        Integer storedSpicy = cn.blockforge.orient.framework.compat.ItemData.get(stack, HotPotContent.SPICY_LEVEL);
        int spicy = Math.max(0, Math.min(3, storedSpicy == null ? 0 : storedSpicy));
        // mryt233 改（2026-11-05）：营养走 getFoodProperties() 现读配置，这里交给原版结算
        ItemStack result = super.finishUsingItem(stack, world, user);
        if (!world.isClientSide) {
            if (soup == SoupBase.CLEAR || soup == SoupBase.TOMATO) {
                user.removeEffect(MobEffects.CONFUSION);
            }
            // mryt233 改（2026-11-05，清单 #125）：六种汤碗统一按 SoupBase.dishBonusEffects(辣度) 结算，
            // 红汤碗的辣度是盛出来时装在物品 NBT 里的（0 档 = 微辣，和以前一样）。
            for (net.minecraft.world.effect.MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                user.addEffect(effect);
            }
            // 3 级特辣：嘴上冒 2 秒火苗，纯节目效果（前面刚加的抗火罩着，不会扣血）
            // mryt233 改（2026-11-06，清单 #127）：穿着布围裙时不再点火苗（防溅油装备挡滚油与明火）。
            if (soup == SoupBase.SPICY && spicy >= 3 && !ClothApronItem.wornBy(user)) {
                user.setRemainingFireTicks(Math.max(user.getRemainingFireTicks(), 40));
            }
        }
        return user instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().instabuild
            ? result : new ItemStack(Items.BOWL);
    }
}
