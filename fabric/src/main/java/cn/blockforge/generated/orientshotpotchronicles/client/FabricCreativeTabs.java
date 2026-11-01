package cn.blockforge.generated.orientshotpotchronicles.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/** Fabric 创造物品栏事件接线，布局与状态共用。作者：mryt233 */
public final class FabricCreativeTabs {
    private FabricCreativeTabs() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof CreativeModeInventoryScreen creative) {
                var hooks = CreativeSubTabs.init(creative, width, height, widget -> Screens.getButtons(screen).add(widget));
                ScreenEvents.beforeRender(screen).register((s, graphics, x, y, delta) -> hooks.beforeRender().run());
                ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) ->
                    hooks.scroll().allow(x, y, vertical));
            }
        });
    }
}
