// ；仅客户端接入 Forge 的原版碗渲染扩展。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.ForgeItemRenderers;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class BowlClientExtensionsMixin {
    @Inject(method = "getRenderPropertiesInternal", at = @At("HEAD"), cancellable = true, remap = false)
    private void orientsHotpot$bowlExtension(CallbackInfoReturnable<Object> callback) {
        if ((Object) this == Items.BOWL) callback.setReturnValue(ForgeItemRenderers.extension());
    }
}
