package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在旧版私有槽位绘制方法之后补绘熟度和边框。作者：mryt233 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void hotpot$slot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        if ((Object) this instanceof HotPotScreen screen) screen.renderPotSlot(graphics, slot);
    }
}
