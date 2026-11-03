package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蘸过蘸料的菜（物品组件 dipped）不管怎么吃下去——筷子对空气吃、锅界面里吃、放回盘子再拿手吃——
 * 都会得到蘸料的效果（用户：buff 先弄到食物上，吃了后获得）。原版所有吃东西都走 LivingEntity.eatFood。
 * 作者：mryt233 */
@Mixin(LivingEntity.class)
abstract class LivingEntityEatMixin {
    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void orientsHotpot$sauceEffects(Level world, ItemStack stack,
                                           CallbackInfoReturnable<ItemStack> cir) {
        Integer mask = cn.blockforge.orient.framework.compat.ItemData.get(stack, HotPotContent.DIPPED);
        if (!world.isClientSide && mask != null && mask != 0) {
            Seasoning.applyAll(mask, (LivingEntity) (Object) this);
        }
        // 成就（2026-10-25）：吃了什么、蘸没蘸、是不是坐在凳子上吃的
        if (!world.isClientSide && (Object) this instanceof net.minecraft.world.entity.player.Player player) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player,
                "eat:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
            if (mask != null && mask != 0) {
                cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "eat_dipped");
            }
            if (player.getVehicle() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity) {
                cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "eat_seated");
            }
        }
    }

    /**
     * 用筷子吃东西时，吃的碎屑按筷子上那样菜来画（2026-10-23 用户报：吃丸子碎屑是紫黑的「未知方块」——
     * 筷子夹着菜的物品模型是代码画的 builtin/entity，没有碎屑贴图）。新增。
     */
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "spawnItemParticles", at = @At("HEAD"), argsOnly = true)
    private ItemStack orientsHotpot$chopsticksCrumbs(ItemStack stack) {
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            ItemStack held = cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks.held(stack);
            if (!held.isEmpty()) {
                return held;
            }
        }
        return stack;
    }
}
