// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蘸过蘸料的菜（物品组件 dipped）不管怎么吃下去——筷子对空气吃、锅界面里吃、放回盘子再拿手吃——
 * 都会得到蘸料的效果（用户：buff 先弄到食物上，吃了后获得）。原版所有吃东西都走 LivingEntity.eatFood。
 * 作者：Claude
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityEatMixin {
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void orientsHotpot$sauceEffects(World world, ItemStack stack,
                                           CallbackInfoReturnable<ItemStack> cir) {
        Integer mask = cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.dipped(stack);
        if (!world.isClient && mask != null && mask != 0) {
            Seasoning.applyAll(mask, (LivingEntity) (Object) this);
        }
    }
}
