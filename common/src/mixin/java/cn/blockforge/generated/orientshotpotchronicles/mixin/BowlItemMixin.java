package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableObjects;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版的碗点在方块顶面时像盘子一样摆上去（r99，用户要求）。碗是原版的普通物品，没有自己的 useOnBlock，
 * 所以在 Item.useOnBlock 开头接一下：方块自己的右键（箱子、锅……）照常先处理，轮到物品时才摆。
 * 作者：mryt233 */
@Mixin(Item.class)
abstract class BowlItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$placeBowl(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this == Items.BOWL) {
            UseOutcome outcome = TableObjects.placeOn(context, HotPotContent.BOWL_BLOCK, false);
            if (outcome != UseOutcome.PASS) {
                cir.setReturnValue(Use.item(outcome, context.getLevel().isClientSide));
            }
        }
    }
}
