// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.client.ClientContent;
import cn.blockforge.orient.framework.pure.DeferredRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** NeoForge 通用客户端能力实现：26.2 物品模型由 DynamicItemModel 自行接管渲染器表；通过 DeferredRegistrations 支持 client(hooks) 晚于事件回调的顺序。作者：mryt233 */
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
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(
            MenuType<M> type, ScreenFactory<M, S> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);
        menus.add(e -> e.register(type, factory::create));
    }

    @Override
    public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<T> type, BlockEntityRendererProvider<T, S> factory) {
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
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) ->
            callback.append(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip()));
    }

    @Override
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        extensions.add(event -> event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack,
                                               EquipmentClientInfo.LayerType layerType,
                                               Model original) {
                EquipmentSlot slot = layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                    ? EquipmentSlot.LEGS
                    : EquipmentSlot.CHEST;
                HumanoidModel<?> base = original instanceof HumanoidModel<?> humanoid ? humanoid : null;
                HumanoidModel<?> custom = factory.model(stack, slot, base);
                return custom != null ? custom : original;
            }
        }, items));
    }

    @Override
    public void onDisconnect(Runnable callback) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> callback.run());
    }

    // mryt233 改：26.2 按每面贴图 alpha 自动确定 cutout，无需全局登记。
    @Override
    public void cutout(Block... blocks) {
        for (Block block : blocks) {
            Objects.requireNonNull(block, "block");
        }
    }

    @Override
    public void screens(ScreenInit initializer) {
        Map<Screen, ScreenHooks> active = new IdentityHashMap<>();
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            ScreenHooks hooks = initializer.init(event.getScreen(), event::addListener);
            if (hooks != null) {
                active.put(event.getScreen(), hooks);
            } else {
                active.remove(event.getScreen());
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event) -> {
            ScreenHooks hooks = active.get(event.getScreen());
            if (hooks != null && hooks.beforeRender() != null) {
                hooks.beforeRender().run();
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre event) -> {
            ScreenHooks hooks = active.get(event.getScreen());
            if (hooks != null && hooks.scroll() != null
                && !hooks.scroll().allow(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing event) -> active.remove(event.getScreen()));
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        NeoForge.EVENT_BUS.addListener((ExtractBlockOutlineRenderStateEvent event) -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            VoxelShape shape = callback.extract(event.getBlockState(), event.getBlockPos(), event.getLevel(), player);
            if (shape == null) return;
            event.setCanceled(true);
            event.getLevelRenderState().blockOutlineRenderState = new BlockOutlineRenderState(
                event.getBlockPos(), event.isInTranslucentPass(), event.isHighContrast(), shape, event.getCustomRenderers());
        });
    }
}
