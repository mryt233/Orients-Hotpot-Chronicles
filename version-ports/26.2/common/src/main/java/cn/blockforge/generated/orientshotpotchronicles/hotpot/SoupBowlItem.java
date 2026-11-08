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
 * 营养和效果都能在配置文件 soup_bowl.&lt;汤底&gt; 里改（mryt233 改 2026-11-05）：
 * 默认回 4 点饥饿值，高汤、三鲜汤饱和度高一些。风味效果见 {@link SoupBase#dishBonusEffects(int)}
 * （mryt233 改 2026-11-06，r139：清汤速度 + 饱和、高汤抗性 + 伤害吸收且不再缓慢、菌汤生命恢复、
 * 红汤力量 + 速度且特辣起加抗火与饥饿、番茄汤急迫、三鲜汤水下呼吸 + 幸运）；清汤、番茄汤喝下还解反胃。
 * 喝完还一只空碗（同原版蘑菇煲）。
 * mryt233 改（2026-11-05，r136a）：六种汤碗都按 SoupBase.dishBonusEffects(辣度) 结算，
 * 红汤碗还带着盛出时的辣度档位（SPICY_LEVEL 组件），3 级特辣喝完嘴上冒火（2 秒无伤火苗）。
 * 作者：mryt233 */
public class SoupBowlItem extends Item {
    private final SoupBase soup;

    public SoupBowlItem(SoupBase soup, Item.Properties properties) {
        super(properties.stacksTo(1).food(foodOf(soup)).usingConvertsTo(Items.BOWL));
        this.soup = soup;
    }

    /** 注册时的数值只作默认值和客户端显示用；真正的结算在 {@link #eating}（照 ConfiguredFoodItem 的思路）。 */
    private static FoodProperties foodOf(SoupBase soup) {
        return new FoodProperties.Builder()
            .nutrition(SoupBase.DEFAULT_BOWL_HUNGER)
            .saturationModifier(soup == SoupBase.RICH_STOCK || soup == SoupBase.THREE_FRESH ? 0.9F : 0.6F)
            .alwaysEdible()   // 饱了也能喝（同本模组其他食物，mryt233 改 2026-11-01）
            .build();
    }

    /**
     * mryt233 改（2026-11-05）：喝的那一刻按配置重建营养组件（饥饿值 + 饱和度系数），
     * 所以改完 config/orients_hotpot_chronicles.json 执行 /orientshotpot reload 就能立即生效。
     * 留下空碗、饱了也能喝这两项跟注册时保持一致。
     */
    private FoodProperties eating() {
        // saturationModifier 收的是系数（原版内部按 2×营养换算成饱和点数），和配置里 food.<物品>.saturation 含义一致
        return new FoodProperties.Builder()
            .nutrition(soup.bowlHunger())
            .saturationModifier(soup.bowlSaturation())
            // 26.x 的 FoodProperties.Builder 没有 usingConvertsTo：留空碗由物品属性上的
            // Item.Properties.usingConvertsTo(Items.BOWL) 负责（mryt233 改，2026-11-08）
            .alwaysEdible()
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
     * mryt233 改（2026-11-07）：辣度必须在吃之前读。
     * <p>user.eat(...) 内部会 stack.consume(1, user)，把这一碗吃到 0 个；空栈的 getComponents()
     * 返回 DataComponentMap.EMPTY，此后再读 SPICY_LEVEL 只能拿到兜底的 0，红汤的等级加成、抗火，
     * 以及 3 级特辣喝完嘴上冒火都会静默失效。与 LivingEntityEatMixin 在 eat 的 HEAD 处读组件保持同一口径。</p>
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        // mryt233 改（2026-11-07）：先把辣度取成快照，再交给原版结算；吃过之后再读必然读到 0 档
        int spicy = Math.max(0, Math.min(3, stack.getOrDefault(HotPotContent.SPICY_LEVEL, 0)));
        // mryt233 改（26.x）：营养按配置现算——临时换上 eating() 的 FOOD 组件再走原版结算，结算完换回去，
        // 做法和 ConfiguredFoodItem 一致（26.x 没有 user.eat(world, stack, food) 这个重载）。
        FoodProperties original = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        stack.set(net.minecraft.core.component.DataComponents.FOOD, eating());
        ItemStack result;
        try {
            result = super.finishUsingItem(stack, world, user);
        } finally {
            stack.set(net.minecraft.core.component.DataComponents.FOOD, original);
        }
        if (!world.isClientSide()) {
            if (soup == SoupBase.CLEAR || soup == SoupBase.TOMATO) {
                user.removeEffect(MobEffects.NAUSEA);
            }
            // mryt233 改（2026-11-05，r136a）：六种汤碗统一按 SoupBase.dishBonusEffects(辣度) 结算，
            // 红汤碗的辣度是盛出来时装在 SPICY_LEVEL 组件里的（0 档 = 微辣，和以前一样）。
            for (net.minecraft.world.effect.MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                user.addEffect(effect);
            }
            // 3 级特辣：嘴上冒 2 秒火苗，纯节目效果（前面刚加的抗火罩着，不会扣血）
            // mryt233 改（2026-11-06，r139）：穿着布围裙时不再点火苗（防溅油装备挡滚油与明火）。
            if (soup == SoupBase.SPICY && spicy >= 3 && !ClothApronItem.wornBy(user)) {
                user.setRemainingFireTicks(Math.max(user.getRemainingFireTicks(), 40));
            }
        }
        return result;
    }
}
