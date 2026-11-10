// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

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
}
