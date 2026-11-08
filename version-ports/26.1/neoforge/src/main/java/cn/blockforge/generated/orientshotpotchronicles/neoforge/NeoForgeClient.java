package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.orient.framework.client.PartOutlineDrawing;
import cn.blockforge.orient.framework.client.ClientNetwork;
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.generated.orientshotpotchronicles.client.*;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** NeoForge 客户端接线；专用服不加载这个入口，构造时不触发内容注册。作者：mryt233 */
@Mod(value = HotPot.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
    private static net.minecraft.client.gui.screens.Screen tabsScreen;
    private static CreativeSubTabs.Hooks tabs;

    public NeoForgeClient(IEventBus modBus) {
        modBus.addListener(NeoForgeClient::setup);
        modBus.addListener(NeoForgeClient::screens);
        modBus.addListener(NeoForgeClient::renderers);
        // mryt233 改（26.1）：手持 / 物品栏里的自定义物品画法改成物品模型类型（items/*.json 引用），
        // 和 Fabric 同一套 DynamicItemModel / SoupItemProperty；BEWLR 在 26.1 已经没有了。
        DynamicItemModel.install();
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) ->
            event.register(HotPot.id("dynamic"), DynamicItemModel.Unbaked.CODEC));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) ->
            event.register(HotPot.id("soup"), SoupItemProperty.CODEC));
        // mryt233 改（26.1，移植同步清单 #128）：NeoForge 的模组列表「Config」按钮直接打开设置界面。
        net.neoforged.fml.ModContainer container =
            net.neoforged.fml.ModList.get().getModContainerById(HotPot.MOD_ID).orElseThrow();
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
            (mc, parent) -> new HotPotConfigScreen(parent));
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::tooltips);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::disconnect);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::outline);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenInit);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenRender);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenScroll);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenClose);
        if (!net.neoforged.fml.loading.FMLEnvironment.isProduction() && Boolean.getBoolean("hotpot.clientProbe")) {
            try {
                var tick = Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.ClientProbe").getMethod("tick");
                NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> {
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
            // 发包统一走框架 NetworkApi，客户端侧也计入行为统计
            ClientNetwork.install(payload -> cn.blockforge.orient.framework.loader.Framework.platform()
                .network().sendToServer(payload));
            ChopsticksAimClient.install();
            GuideBookItem.clientOpen = () -> Minecraft.getInstance().setScreen(new GuideBookScreen());
            SauceStationBlock.openOverview = (pos, title) ->
                Minecraft.getInstance().setScreen(new SauceOverviewScreen(pos, title));
            // mryt233 改（26.1）：按贴图透明度自动选 cutout，不再登记渲染层（同 Fabric）。
            SharedClientSetup.registerItems();
            HotPotNeoForge.clientHooks(new cn.blockforge.orient.framework.loader.ClientHooks() {
                @Override
                public void onClientSetup(Runnable callback) {
                    callback.run();
                }
            });
        });
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(HotPotContent.HOT_POT_SCREEN, HotPotScreen::new);
        event.register(HotPotContent.STOVE_SCREEN, StoveScreen::new);
        event.register(HotPotContent.SAUCE_STATION_SCREEN, SauceStationScreen::new);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HotPotContent.HOT_POT_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.STOVE_BE, HotPotBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.SAUCE_STATION_BE, SauceStationRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.CUTTING_BOARD_BE, CuttingBoardRenderer::new);
        event.registerBlockEntityRenderer(HotPotContent.PLATE_HOLDER_BE, PlateHolderRenderer::new);
        event.registerEntityRenderer(HotPotContent.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    private static void tooltips(ItemTooltipEvent event) {
        ClientTooltips.append(event.getItemStack(), event.getToolTip());
    }

    /**
     * 部件描边（mryt233 改，26.1）：原版先发提取事件、再按方块形状建描边状态。
     * 这里和 Fabric 的 AFTER_BLOCK_OUTLINE_EXTRACTION 效果一样：瞄准的是可分部件的方块时，
     * 取消原版这一次提取，换成「部件形状」的描边状态，线条仍由原版管线画。
     */
    private static void outline(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent event) {
        var shape = PartOutlineDrawing.extract(event.getBlockState(), event.getBlockPos(), event.getLevel(),
            Minecraft.getInstance().player);
        if (shape == null) {
            return;
        }
        event.setCanceled(true);
        event.getLevelRenderState().blockOutlineRenderState = new net.minecraft.client.renderer.state.level.BlockOutlineRenderState(
            event.getBlockPos(), event.isInTranslucentPass(), event.isHighContrast(), shape, event.getCustomRenderers());
    }

    private static void screenInit(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creative) {
            tabsScreen = creative;
            tabs = CreativeSubTabs.init(creative, creative.width, creative.height, event::addListener);
        }
    }

    private static void screenRender(net.neoforged.neoforge.client.event.ScreenEvent.Render.Pre event) {
        if (event.getScreen() == tabsScreen && tabs != null) {
            tabs.beforeRender().run();
        }
    }

    private static void screenScroll(net.neoforged.neoforge.client.event.ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() == tabsScreen && tabs != null
            && !tabs.scroll().allow(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private static void screenClose(net.neoforged.neoforge.client.event.ScreenEvent.Closing event) {
        if (event.getScreen() == tabsScreen) {
            tabs = null;
            tabsScreen = null;
        }
    }

    private static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        Config.clearClient();
    }
}
