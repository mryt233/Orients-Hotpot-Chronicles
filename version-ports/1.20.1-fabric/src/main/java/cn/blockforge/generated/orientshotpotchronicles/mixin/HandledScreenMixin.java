package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.x 移植版专用：1.21 的锅界面直接重写 drawSlot 给格子加边框和熟度标记；1.20.1 的 drawSlot 是 private，
 * 改成在原版画完每个格子后调 {@link HotPotScreen#afterDrawSlot}，效果一样。
 *
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void orients_hotpot_chronicles$afterDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        if ((Object) this instanceof HotPotScreen screen) {
            screen.afterDrawSlot(context, slot);
        }
    }
}
