package cn.blockforge.generated.orientshotpotchronicles.neoforge;

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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge 客户端接线；专用服不加载这个入口，构造时不触发内容注册。作者：mryt233 */
@Mod(value = HotPot.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
    private static net.minecraft.client.gui.screens.Screen tabsScreen;
    private static CreativeSubTabs.Hooks tabs;

    /**
     * mryt233 改（2026-11-05，r135e）：多接收一个 {@link ModContainer}，用来注册 NeoForge 原生配置界面扩展点——
     * 主菜单 Mods -> 选中本模组 -> 「Config」按钮就能直接打开 {@link HotPotConfigScreen}（不依赖 Mod Menu）。
     */
    public NeoForgeClient(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        if (container != null) {
            container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mc, parent) -> new HotPotConfigScreen(parent));
        }
        modBus.addListener(NeoForgeClient::setup);
        modBus.addListener(NeoForgeClient::screens);
        modBus.addListener(NeoForgeClient::renderers);
        modBus.addListener(NeoForgeClient::items);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::tooltips);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::disconnect);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::outline);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenInit);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenRender);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenScroll);
        NeoForge.EVENT_BUS.addListener(NeoForgeClient::screenClose);
        if (!net.neoforged.fml.loading.FMLEnvironment.production && Boolean.getBoolean("hotpot.clientProbe")) {
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
            for (var block : HotPotContent.SAUCE_STATIONS) {
                ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
            }
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
        event.registerBlockEntityRenderer(HotPotContent.BENCH_BE, BenchLegsRenderer::new);
        event.registerEntityRenderer(HotPotContent.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    private static void items(RegisterClientExtensionsEvent event) {
        ClientItemRenderers.install((item, renderer) -> event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer custom;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (custom == null) {
                    Minecraft mc = Minecraft.getInstance();
                    custom = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                                 MultiBufferSource buffers, int light, int overlay) {
                            renderer.render(stack, context, pose, buffers, light, overlay);
                        }
                    };
                }
                return custom;
            }
        }, item));
        SharedClientSetup.registerItems();
    }

    private static void tooltips(ItemTooltipEvent event) {
        ClientTooltips.append(event.getItemStack(), event.getToolTip());
    }

    private static void outline(net.neoforged.neoforge.client.event.RenderHighlightEvent.Block event) {
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
