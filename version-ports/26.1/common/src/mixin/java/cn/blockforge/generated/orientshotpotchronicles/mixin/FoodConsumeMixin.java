// 作者：mryt233
// mryt233 改（2026-11-13）：26.1 原版 API；同步主工程蘸料列表、动态汤底与配置辣火规则。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity;
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
 * <p>26.x 的进食走物品自带的 {@code CONSUMABLE}，蘸料效果、汤底风味加成、变态辣火苗与全部进食成就
 * 集中在这里结算。</p>
 * 作者：mryt233
 */
@Mixin(Consumable.class)
abstract class FoodConsumeMixin {
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void orientsHotpot$consume(Level world, LivingEntity user, ItemStack stack,
                                       CallbackInfoReturnable<ItemStack> callback) {
        if (world.isClientSide() || !stack.has(DataComponents.FOOD)) {
            return;
        }
        List<String> dipped = stack.get(HotPotContent.DIPPED);
        if (dipped != null && !dipped.isEmpty()) {
            Seasoning.applyAll(dipped, user);
        }
        String soupKey = stack.get(HotPotContent.COOKED_IN_SOUP);
        if (soupKey != null) {
            SoupDef soup = Soups.get(soupKey);
            if (soup != null) {
                int spicy = stack.getOrDefault(HotPotContent.SPICY_LEVEL, 0);
                boolean calmSpicy = dipped != null
                    && (SauceIds.containsDef(dipped, Seasoning.HONEY)
                        || SauceIds.containsDef(dipped, Seasoning.VINEGAR));
                if (soup.clearsNausea()) {
                    user.removeEffect(MobEffects.NAUSEA);
                }
                for (MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                    if (calmSpicy && effect.getEffect().is(MobEffects.HUNGER)) {
                        continue;
                    }
                    user.addEffect(effect);
                }
                int mouthFireTicks = Config.spicyMouthFireTicks();
                if (soup.spicy()
                    && mouthFireTicks > 0 && spicy >= Config.spicyMouthFireFromLevel()
                    && !calmSpicy
                    && !ClothApronItem.wornBy(user)) {
                    user.setRemainingFireTicks(Math.max(user.getRemainingFireTicks(), mouthFireTicks));
                }
            }
        }
        if (user instanceof Player player) {
            HotPotCriterion.fire(player, "eat:" + BuiltInRegistries.ITEM.getId(stack.getItem()));
            if (stack.is(HotPotContent.ZHE_ER_GEN)
                || stack.is(HotPotContent.ZHE_ER_GEN_SLICE)
                || stack.is(HotPotContent.COOKED_ZHE_ER_GEN_SLICE)) {
                HotPotCriterion.fire(player, "eat_zhe_er_gen");
            }
            if (dipped != null && !dipped.isEmpty()) {
                HotPotCriterion.fire(player, "eat_dipped");
                if (dipped.size() >= 5) {
                    HotPotCriterion.fire(player, "sauce_master_5");
                }
            }
            if (player.getVehicle() instanceof SeatEntity) {
                HotPotCriterion.fire(player, "eat_seated");
                HotPotCriterion.fire(player, "eat_seated_bench");
            }
        }
    }
}
