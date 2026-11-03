// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 普通食物、筷子与锅界面的进食在原版统一消耗入口结算一次。 */
@Mixin(Consumable.class)
abstract class FoodConsumeMixin {
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void orientsHotpot$consume(Level world, LivingEntity user, ItemStack stack,
                                       CallbackInfoReturnable<ItemStack> callback) {
        if (world.isClientSide() || !stack.has(DataComponents.FOOD)) { return; }
        Integer mask = stack.get(HotPotContent.DIPPED);
        if (mask != null && mask != 0) { Seasoning.applyAll(mask, user); }
        if (user instanceof Player player) {
            HotPotCriterion.fire(player, "eat:" + BuiltInRegistries.ITEM.getId(stack.getItem()));
            if (mask != null && mask != 0) { HotPotCriterion.fire(player, "eat_dipped"); }
            if (player.getVehicle() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity) {
                HotPotCriterion.fire(player, "eat_seated");
            }
        }
    }
}
