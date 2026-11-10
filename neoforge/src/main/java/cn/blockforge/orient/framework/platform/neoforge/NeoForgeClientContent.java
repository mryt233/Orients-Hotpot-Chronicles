// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.pure.DeferredRegistrations;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import java.util.Objects;

/** 各登记器通过原生事件落地；也支持 client(hooks) 晚于事件的初始化顺序。 */
final class NeoForgeClientContent implements ClientContent {
    private final DeferredRegistrations<RegisterMenuScreensEvent> menus = new DeferredRegistrations<>();
    private final DeferredRegistrations<EntityRenderersEvent.RegisterRenderers> renderers = new DeferredRegistrations<>();
    private final DeferredRegistrations<RegisterClientExtensionsEvent> extensions = new DeferredRegistrations<>();

    static void install(IEventBus bus) {
        NeoForgeClientContent content = new NeoForgeClientContent();
        bus.addListener((RegisterMenuScreensEvent e) -> content.menus.bind(e));
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> content.renderers.bind(e));
        bus.addListener((RegisterClientExtensionsEvent e) -> content.extensions.bind(e));
        ClientContent.install(content);
        // mryt233 改（2026-11-10）：复用扩展事件队列，公共登记不依赖原生事件先后。
        ClientItemRenderers.install((item, renderer) ->
            content.extensions.add(e -> e.registerItem(new IClientItemExtensions() {
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
            }, item)));
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(MenuType<M> type, ScreenFactory<M, S> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        menus.add(e -> e.register(type, factory::create));
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
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) ->
            callback.append(e.getItemStack(), e.getContext(), e.getFlags(), e.getToolTip()));
    }

    @Override
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        if (items.length == 0) throw new IllegalArgumentException("no armor items");
        Item[] copy = items.clone();
        for (Item item : copy) {
            if (!(item instanceof ArmorItem)) throw new IllegalArgumentException("armor item required: " + item);
        }
        extensions.add(e -> e.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                           EquipmentSlot slot, HumanoidModel<?> original) {
                return Objects.requireNonNull(factory.model(entity, stack, slot, original));
            }
        }, copy));
    }

    @Override
    public void onDisconnect(Runnable callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            // 原生事件在进入世界前也会用空连接触发，只转发真正的玩家断开。
            if (e.getPlayer() != null && e.getConnection() != null) callback.run();
        });
    }
    // mryt233 改（2026-11-10）：子标签和选中框的原生差异统一在平台转换。
    @Override
    public void cutout(Block... blocks) {
        for (var block : blocks) ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout());
    }

    @Override
    public void screens(ScreenInit initializer) {
        Objects.requireNonNull(initializer);
        Map<Screen, ScreenHooks> hooks = new IdentityHashMap<>();
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            ScreenHooks value = initializer.init(e.getScreen(), e::addListener);
            if (value == null) hooks.remove(e.getScreen()); else hooks.put(e.getScreen(), value);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre e) -> {
            ScreenHooks value = hooks.get(e.getScreen());
            if (value != null) value.beforeRender().run();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> {
            ScreenHooks value = hooks.get(e.getScreen());
            if (value != null && !value.scroll().allow(e.getMouseX(), e.getMouseY(), e.getScrollDeltaY())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing e) -> hooks.remove(e.getScreen()));
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((RenderHighlightEvent.Block e) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var pos = e.getTarget().getBlockPos();
            var camera = e.getCamera().getPosition();
            if (callback.render(mc.level.getBlockState(pos), pos, camera.x, camera.y, camera.z, e.getPoseStack(),
                    e.getMultiBufferSource().getBuffer(RenderType.lines()))) e.setCanceled(true);
        });
    }
}
