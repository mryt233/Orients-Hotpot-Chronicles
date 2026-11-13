// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.client.renderer.entity.NoopRenderer;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.client.ClientNetwork;
import cn.blockforge.orient.framework.client.PartOutlineDrawing;
import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** 仅客户端源码使用 ClientContent；所有火锅客户端内容在此登记一次。 */
public final class HotPotClientContent {
    private HotPotClientContent() { }
    public static void install() {
        // mryt233 改（2026-11-11）：旧无世界入口的客户端回退，不持有世界。
        HotPotRecipes.clientLevel(() -> Minecraft.getInstance().level);
        ClientContent client = ClientContent.get();
        ClientNetwork.install(payload -> Framework.platform().network().sendToServer(payload));
        ChopsticksAimClient.install();
        GuideBookItem.clientOpen = () -> Minecraft.getInstance().setScreen(new GuideBookScreen());
        SauceStationBlock.openOverview = (pos, title) -> Minecraft.getInstance().setScreen(new SauceOverviewScreen(pos, title));
        client.onDisconnect(Config::clearClient);
        client.menu(HotPotContent.HOT_POT_SCREEN, HotPotScreen::new);
        client.menu(HotPotContent.STOVE_SCREEN, StoveScreen::new);
        client.menu(HotPotContent.SAUCE_STATION_SCREEN, SauceStationScreen::new);
        client.blockEntityRenderer(HotPotContent.HOT_POT_BE, HotPotBlockEntityRenderer::new);
        client.blockEntityRenderer(HotPotContent.STOVE_BE, HotPotBlockEntityRenderer::new);
        client.blockEntityRenderer(HotPotContent.SAUCE_STATION_BE, SauceStationRenderer::new);
        client.blockEntityRenderer(HotPotContent.CUTTING_BOARD_BE, CuttingBoardRenderer::new);
        client.blockEntityRenderer(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        client.blockEntityRenderer(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        client.entityRenderer(HotPotContent.SEAT, NoopRenderer::new);
        client.armorModels((entity, stack, slot, original) -> slot == EquipmentSlot.CHEST ? ApronModel.get() : null, HotPotContent.CLOTH_APRONS.toArray(Item[]::new));
        client.tooltip((stack, context, flag, lines) -> ClientTooltips.append(stack, lines));
        client.cutout(HotPotContent.SAUCE_STATIONS.toArray(Block[]::new));
        client.screens((screen, add) -> {
            if (!(screen instanceof CreativeModeInventoryScreen creative)) return null;
            var hooks = CreativeSubTabs.init(creative, creative.width, creative.height, add);
            return new ClientContent.ScreenHooks(hooks.beforeRender(), hooks.scroll()::allow);
        });
        client.blockOutline(PartOutlineDrawing::render);
        SharedClientSetup.registerItems();
    }
}
