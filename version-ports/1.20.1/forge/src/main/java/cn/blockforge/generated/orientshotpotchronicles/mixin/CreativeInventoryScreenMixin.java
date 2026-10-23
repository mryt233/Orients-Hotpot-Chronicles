package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.CreativeSubTabs;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 本模组分类页按左边的子类按钮（{@link CreativeSubTabs}）筛物品：切换分类页、物品列表重建之后各筛一次，并滚回顶部。
 * 作者：Claude
 */
@Mixin(CreativeInventoryScreen.class)
abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {
    @Shadow
    private float scrollPosition;

    CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = {"setSelectedTab", "refreshSelectedTab"}, at = @At("TAIL"))
    private void orientsHotpot$filterSubTab(CallbackInfo ci) {
        if (CreativeSubTabs.ourTabSelected() && CreativeSubTabs.current != CreativeSubTabs.Sub.ALL) {
            CreativeSubTabs.filter(this.handler.itemList);
            this.scrollPosition = 0.0F;
            this.handler.scrollItems(0.0F);
        }
    }
}
