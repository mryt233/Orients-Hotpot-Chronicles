// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 普通食物、筷子与锅界面的进食在原版统一消耗入口结算一次。
 *
 * <p>mryt233 改（2026-11-07）：26.x 的进食走物品自带的 {@code CONSUMABLE}，所以蘸料效果、
 * 汤底风味加成、变态辣火苗与全部进食成就都集中在这里结算，和 1.21.1 在
 * {@code LivingEntity.eat} 处注入的口径一致（结果相同，入口换了一个）。</p>
 * mryt233 改（2026-11-05，r136a）：在哪种汤底里煮出来的菜（COOKED_IN_SOUP 组件）吃下去时按汤底给风味加成，
 * 红汤再按辣度档位（SPICY_LEVEL 组件）加强；同时蘸了蜂蜜 / 醋的菜不再加负面「饥饿」。
 * 作者：mryt233
 */
@Mixin(Consumable.class)
abstract class FoodConsumeMixin {
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void orientsHotpot$consume(Level world, LivingEntity user, ItemStack stack,
                                       CallbackInfoReturnable<ItemStack> callback) {
        if (world.isClientSide() || !stack.has(DataComponents.FOOD)) { return; }
        List<String> dipped = stack.get(HotPotContent.DIPPED);   // mryt233 改（4E-1）：ID 列表
        if (dipped != null && !dipped.isEmpty()) { Seasoning.applyAll(dipped, user); }
        // mryt233 改（2026-11-05，r136a）：汤底风味加成。这道菜是在哪种汤里煮出来的，就补一份那种汤的
        // 状态效果；红汤再按它带来的辣度档位加强（0 微辣 ~ 3 变态辣）。
        String soupKey = stack.get(HotPotContent.COOKED_IN_SOUP);
        if (soupKey != null) {
            SoupDef soup = Soups.get(soupKey);   // mryt233 改（2026-11-11，4B-2）：问登记表
            if (soup != null) {
                int spicy = stack.getOrDefault(HotPotContent.SPICY_LEVEL, 0);
                // 同时蘸了解辣的东西（蜂蜜 / 醋）：负面效果不再叠上去
                boolean calmSpicy = dipped != null
                    && (SauceIds.containsDef(dipped, Seasoning.HONEY) || SauceIds.containsDef(dipped, Seasoning.VINEGAR));
                // 清汤、番茄汤本来就解冷锅反胃
                // mryt233 改（2026-11-10，r147）：「是不是清汤 / 番茄」改成汤底自己的属性 clearsNausea()
                if (soup.clearsNausea()) {
                    user.removeEffect(MobEffects.NAUSEA);
                }
                for (MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                    if (calmSpicy && effect.getEffect().is(MobEffects.HUNGER)) {
                        continue;   // 蘸了蜂蜜 / 醋，红汤的饥饿不再往上叠
                    }
                    user.addEffect(effect);
                }
                // 变态辣且没解辣：嘴上冒火苗，纯节目效果（前面刚给了抗火，不扣血）
                // mryt233 改（2026-11-06，r139）：穿着布围裙就不点嘴上这簇火——布围裙挡的是溅起来的滚油与明火。
                // mryt233 改（2026-11-10）：红汤判断改成 isSpicy() 属性；mryt233 改（第四批 4J）：改读 Config.spicyMouthFire*
                int mouthFireTicks = Config.spicyMouthFireTicks();
                if (soup.spicy()
                    && mouthFireTicks > 0 && spicy >= Config.spicyMouthFireFromLevel()
                    && !calmSpicy
                    && !ClothApronItem.wornBy(user)) {
                    user.setRemainingFireTicks(Math.max(user.getRemainingFireTicks(), mouthFireTicks));
                }
            }
        }
        // 成就（2026-10-25）：吃了什么、蘸没蘸、是不是坐在凳子上吃的
        if (user instanceof Player player) {
            HotPotCriterion.fire(player, "eat:" + BuiltInRegistries.ITEM.getId(stack.getItem()));
            // mryt233 改（2026-11-06，r139「西南之魂」）：吃下折耳根（整根、段、熟段都算）就解锁。
            if (stack.is(HotPotContent.ZHE_ER_GEN)
                || stack.is(HotPotContent.ZHE_ER_GEN_SLICE)
                || stack.is(HotPotContent.COOKED_ZHE_ER_GEN_SLICE)) {
                HotPotCriterion.fire(player, "eat_zhe_er_gen");
            }
            if (dipped != null && !dipped.isEmpty()) {
                HotPotCriterion.fire(player, "eat_dipped");
                // mryt233 改（2026-11-06，r139「调味宗师」）：这一口蘸到了 5 种以上调料才算。
                if (dipped.size() >= 5) {
                    HotPotCriterion.fire(player, "sauce_master_5");
                }
            }
            // 座位只取一次：getVehicle() 每帧都可能变，同一段判定里必须用同一个对象
            if (player.getVehicle() instanceof SeatEntity) {
                // mryt233 改（2026-11-10）：运行时仅保留条凳座位，高凳已归档。
                HotPotCriterion.fire(player, "eat_seated");
                HotPotCriterion.fire(player, "eat_seated_bench");
            }
        }
    }
}
