package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.orient.framework.client.PartOutlineDrawing;
import cn.blockforge.orient.framework.client.ClientNetwork;
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

/** Fabric 客户端入口：仅平台接线，绘制/提示在共享客户端源码。作者：mryt233 */
public final class HotPotClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientItemRenderers.install((item, renderer) ->
            BuiltinItemRendererRegistry.INSTANCE.register(item, renderer::render));
        // 发包统一走框架 NetworkApi，客户端侧也计入行为统计
        ClientNetwork.install(payload -> cn.blockforge.orient.framework.loader.Framework.platform()
            .network().sendToServer(payload));
        ChopsticksAimClient.install();
        GuideBookItem.clientOpen = () -> Minecraft.getInstance().setScreen(new GuideBookScreen());
        SauceStationBlock.openOverview = (pos, title) ->
            Minecraft.getInstance().setScreen(new SauceOverviewScreen(pos, title));
        // 配置接收已并入框架 NetworkApi 的 S2C 客户端处理器，避免重复注册导致处理两次
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Config.clearClient());
        MenuScreens.register(HotPotContent.HOT_POT_SCREEN, HotPotScreen::new);
        MenuScreens.register(HotPotContent.STOVE_SCREEN, StoveScreen::new);
        MenuScreens.register(HotPotContent.SAUCE_STATION_SCREEN, SauceStationScreen::new);
        BlockEntityRendererRegistry.register(HotPotContent.HOT_POT_BE, HotPotBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.STOVE_BE, HotPotBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.SAUCE_STATION_BE, SauceStationRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.CUTTING_BOARD_BE, CuttingBoardRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        BlockEntityRendererRegistry.register(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        EntityRendererRegistry.register(HotPotContent.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
        // mryt233 改（2026-11-09，r146g）：布围裙用自己的穿戴模型（垂到膝盖、贴身），见 ApronModel
        net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register((pose, buffers, stack, entity, slot, light, parent) -> {
            net.minecraft.resources.ResourceLocation texture = ApronModel.texture(stack);
            if (texture != null) {
                net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.renderPart(
                    pose, buffers, light, stack, ApronModel.posedFrom(parent), texture);
            }
        }, HotPotContent.CLOTH_APRONS.toArray(new net.minecraft.world.level.ItemLike[0]));
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> ClientTooltips.append(stack, lines));
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
            net.minecraft.client.renderer.RenderType.cutout(),
            HotPotContent.SAUCE_STATIONS.toArray(new net.minecraft.world.level.block.Block[0]));
        FabricCreativeTabs.register();
        PartOutlineRenderer.register();
        SharedClientSetup.registerItems();
        cn.blockforge.generated.orientshotpotchronicles.GeneratedMod.clientHooks(new cn.blockforge.orient.framework.loader.ClientHooks() {
            @Override
            public void onClientSetup(Runnable callback) {
                callback.run();
            }
        });
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()
            && Boolean.getBoolean("hotpot.clientProbe")) {
            try {
                var tick = Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.ClientProbe").getMethod("tick");
                net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
                    try {
                        tick.invoke(null);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
