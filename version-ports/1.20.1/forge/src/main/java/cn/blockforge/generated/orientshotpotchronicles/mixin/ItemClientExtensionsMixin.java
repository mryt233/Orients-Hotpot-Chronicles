package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.forge.ForgeItemRenderers;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** mryt233 改：通过旧版客户端扩展接入自定义绘制，包含原版碗。 */
@Mixin(Item.class)
public abstract class ItemClientExtensionsMixin {
    @Inject(method = "getRenderPropertiesInternal", at = @At("HEAD"), cancellable = true, remap = false)
    private void hotpot$extensions(CallbackInfoReturnable<Object> callback) {
        Object extension = ForgeItemRenderers.extension((Item) (Object) this);
        if (extension != null) callback.setReturnValue(extension);
    }
}
