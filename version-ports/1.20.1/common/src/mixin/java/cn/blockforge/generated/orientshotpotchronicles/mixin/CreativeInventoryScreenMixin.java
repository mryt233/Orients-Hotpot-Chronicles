package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.CreativeSubTabs;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 本模组分类页按左边的子类按钮（{@link CreativeSubTabs}）筛物品：切换分类页、物品列表重建之后各筛一次，并滚回顶部。
 * 作者：mryt233 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private float scrollOffs;

    CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = {"selectTab(Lnet/minecraft/world/item/CreativeModeTab;)V", "refreshCurrentTabContents(Ljava/util/Collection;)V"}, at = @At("TAIL"))
    private void orientsHotpot$filterSubTab(CallbackInfo ci) {
        if (CreativeSubTabs.ourTabSelected() && CreativeSubTabs.current != CreativeSubTabs.Sub.ALL) {
            CreativeSubTabs.filter(this.menu.items);
            this.scrollOffs = 0.0F;
            this.menu.scrollTo(0.0F);
        }
    }
}