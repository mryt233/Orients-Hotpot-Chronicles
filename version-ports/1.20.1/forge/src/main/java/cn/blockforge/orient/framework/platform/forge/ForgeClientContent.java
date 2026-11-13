// 作者：mryt233
package cn.blockforge.orient.framework.platform.forge;

import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.orient.framework.pure.DeferredRegistrations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** 各登记器通过原生事件落地；也支持 client(hooks) 晚于事件的初始化顺序。 */
final class ForgeClientContent implements ClientContent {
    private final DeferredRegistrations<EntityRenderersEvent.RegisterRenderers> renderers = new DeferredRegistrations<>();
    private final DeferredRegistrations<FMLClientSetupEvent> setup = new DeferredRegistrations<>();

    static void install(IEventBus bus) {
        ForgeClientContent content = new ForgeClientContent();
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> content.renderers.bind(e));
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> content.setup.bind(e)));
        ClientContent.install(content);
        ClientItemRenderers.install((item, renderer) ->
            ForgeClientExtensions.register(item, new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer custom;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (custom == null) {
                        var mc = Minecraft.getInstance();
                        custom = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                            @Override
                            public void renderByItem(ItemStack stack, ItemDisplayContext context,
                                                     PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                                renderer.render(stack, context, pose, buffers, light, overlay);
                            }
                        };
                    }
                    return custom;
                }
            }));
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(MenuType<M> type, ScreenFactory<M, S> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        setup.add(e -> MenuScreens.register(type, factory::create));
    }

    @Override
    public <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<T> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        renderers.add(e -> e.registerBlockEntityRenderer(type, factory));
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        renderers.add(e -> e.registerEntityRenderer(type, factory));
    }

    @Override
    public void tooltip(Tooltip callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent e) ->
            callback.append(e.getItemStack(), e.getEntity(), e.getFlags(), e.getToolTip()));
    }

    @Override
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        if (items.length == 0) throw new IllegalArgumentException("no armor items");
        Item[] copy = items.clone();
        for (Item item : copy) {
            if (!(item instanceof ArmorItem)) throw new IllegalArgumentException("armor item required: " + item);
        }
        IClientItemExtensions extension = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                           EquipmentSlot slot, HumanoidModel<?> original) {
                return Objects.requireNonNull(factory.model(entity, stack, slot, original));
            }
        };
        for (Item item : copy) {
            ForgeClientExtensions.register(item, extension);
        }
    }

    @Override
    public void onDisconnect(Runnable callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            if (e.getPlayer() != null && e.getConnection() != null) callback.run();
        });
    }

    @Override
    @SuppressWarnings("removal")
    public void cutout(Block... blocks) {
        Block[] copy = blocks.clone();
        setup.add(e -> {
            for (var block : copy) {
                ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
            }
        });
    }

    @Override
    public void screens(ScreenInit initializer) {
        Objects.requireNonNull(initializer);
        Map<Screen, ScreenHooks> hooks = new IdentityHashMap<>();
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            ScreenHooks value = initializer.init(e.getScreen(), e::addListener);
            if (value == null) hooks.remove(e.getScreen()); else hooks.put(e.getScreen(), value);
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre e) -> {
            ScreenHooks value = hooks.get(e.getScreen());
            if (value != null) value.beforeRender().run();
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> {
            ScreenHooks value = hooks.get(e.getScreen());
            if (value != null && !value.scroll().allow(e.getMouseX(), e.getMouseY(), e.getScrollDelta())) e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Closing e) -> hooks.remove(e.getScreen()));
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((RenderHighlightEvent.Block e) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var pos = e.getTarget().getBlockPos();
            var camera = e.getCamera().getPosition();
            if (callback.render(mc.level.getBlockState(pos), pos, camera.x, camera.y, camera.z,
                    e.getPoseStack(), e.getMultiBufferSource().getBuffer(RenderType.lines()))) {
                e.setCanceled(true);
            }
        });
    }
}
