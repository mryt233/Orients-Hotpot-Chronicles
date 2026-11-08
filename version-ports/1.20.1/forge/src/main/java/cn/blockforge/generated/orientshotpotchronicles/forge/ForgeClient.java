package cn.blockforge.generated.orientshotpotchronicles.forge;

import cn.blockforge.orient.framework.client.PartOutlineDrawing;
import cn.blockforge.orient.framework.client.ClientNetwork;
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.generated.orientshotpotchronicles.client.*;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.network.PacketDistributor;

/** Forge 客户端接线；专用服不加载这个入口，构造时不触发内容注册。作者：mryt233 */
public final class ForgeClient {
    private static net.minecraft.client.gui.screens.Screen tabsScreen;
    private static CreativeSubTabs.Hooks tabs;

    public ForgeClient(IEventBus modBus) {
        // mryt233 改（0.2.2-alpha，清单 #128/#114）：Forge 端在模组菜单的「Config」按钮上打开游戏内设置界面。
        // 1.20.1 Forge 用 ModLoadingContext#registerExtensionPoint + ConfigScreenHandler.ConfigScreenFactory。
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
            net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                (mc, parent) -> new cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen(parent)));
        modBus.addListener(ForgeClient::setup);
        modBus.addListener(ForgeClient::renderers);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::tooltips);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::disconnect);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::outline);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::screenInit);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::screenRender);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::screenScroll);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::screenClose);
        if (!net.minecraftforge.fml.loading.FMLEnvironment.production && Boolean.getBoolean("hotpot.clientProbe")) {
            try {
                var tick = Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.ClientProbe").getMethod("tick");
                MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent event) -> {
                    if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
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

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            screens();
            items();
            // 发包统一走框架 NetworkApi，客户端侧也计入行为统计
            ClientNetwork.install(payload -> cn.blockforge.orient.framework.loader.Framework.platform()
                .network().sendToServer(payload));
            ChopsticksAimClient.install();
            GuideBookItem.clientOpen = () -> Minecraft.getInstance().setScreen(new GuideBookScreen());
            SauceStationBlock.openOverview = (pos, title) ->
                Minecraft.getInstance().setScreen(new SauceOverviewScreen(pos, title));
            for (var block : HotPotContent.SAUCE_STATIONS) {
                ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
            }
            HotPotForge.clientHooks(new cn.blockforge.orient.framework.loader.ClientHooks() {
                @Override
                public void onClientSetup(Runnable callback) {
                    callback.run();
                }
            });
        });
    }

    private static void screens() {
        net.minecraft.client.gui.screens.MenuScreens.register(HotPotContent.HOT_POT_SCREEN, HotPotScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(HotPotContent.STOVE_SCREEN, StoveScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(HotPotContent.SAUCE_STATION_SCREEN, SauceStationScreen::new);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HotPotContent.HOT_POT_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.STOVE_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.SAUCE_STATION_BE, SauceStationRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.CUTTING_BOARD_BE, CuttingBoardRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        event.registerEntityRenderer(HotPotContent.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    private static void items() {
        ClientItemRenderers.install(ForgeItemRenderers::register);
        SharedClientSetup.registerItems();
    }

    private static void tooltips(ItemTooltipEvent event) {
        ClientTooltips.append(event.getItemStack(), event.getToolTip());
    }

    private static void outline(net.minecraftforge.client.event.RenderHighlightEvent.Block event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        var pos = event.getTarget().getBlockPos();
        var camera = event.getCamera().getPosition();
        if (cn.blockforge.orient.framework.client.PartOutlineDrawing.render(mc.level.getBlockState(pos), pos, camera.x, camera.y, camera.z,
            event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()))) {
            event.setCanceled(true);
        }
    }

    private static void screenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creative) {
            tabsScreen = creative;
            tabs = CreativeSubTabs.init(creative, creative.width, creative.height, event::addListener);
        }
    }

    private static void screenRender(net.minecraftforge.client.event.ScreenEvent.Render.Pre event) {
        if (event.getScreen() == tabsScreen && tabs != null) {
            tabs.beforeRender().run();
        }
    }

    private static void screenScroll(net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() == tabsScreen && tabs != null
            && !tabs.scroll().allow(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    private static void screenClose(net.minecraftforge.client.event.ScreenEvent.Closing event) {
        if (event.getScreen() == tabsScreen) {
            tabs = null;
            tabsScreen = null;
        }
    }

    private static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        Config.clearClient();
    }
}
