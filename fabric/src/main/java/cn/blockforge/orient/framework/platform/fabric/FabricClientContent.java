// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.orient.framework.client.ClientItemRenderers;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

import cn.blockforge.orient.framework.client.ClientContent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Objects;

/** 只在客户端环境加载；菜单、渲染器立即登记。 */
final class FabricClientContent implements ClientContent {
    static void install() {
        ClientContent.install(new FabricClientContent());
        // mryt233 改（2026-11-10）：动态物品平台适配由框架安装，内容只登记绘制回调。
        ClientItemRenderers.install((item, renderer) ->
            BuiltinItemRendererRegistry.INSTANCE.register(item, renderer::render));
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(MenuType<M> type, ScreenFactory<M, S> factory) {
        MenuScreens.register(Objects.requireNonNull(type), Objects.requireNonNull(factory)::create);
    }

    @Override
    public <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<T> factory) {
        BlockEntityRendererRegistry.register(Objects.requireNonNull(type), Objects.requireNonNull(factory));
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory) {
        EntityRendererRegistry.register(Objects.requireNonNull(type), Objects.requireNonNull(factory));
    }

    @Override
    public void tooltip(Tooltip callback) {
        Objects.requireNonNull(callback);
        ItemTooltipCallback.EVENT.register(callback::append);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void armorModels(ArmorModel factory, Item... items) {
        Objects.requireNonNull(factory);
        if (items.length == 0) throw new IllegalArgumentException("no armor items");
        for (Item item : items) {
            if (!(item instanceof ArmorItem)) throw new IllegalArgumentException("armor item required: " + item);
        }
        ArmorRenderer.register((pose, buffers, stack, entity, slot, light, parent) -> {
            HumanoidModel model = Objects.requireNonNull(factory.model(entity, stack, slot, parent));
            parent.copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> { model.head.visible = true; model.hat.visible = true; }
                case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
                case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
                case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
                default -> { return; }
            }
            ArmorItem item = (ArmorItem) stack.getItem();
            for (var layer : item.getMaterial().value().layers()) {
                ArmorRenderer.renderPart(pose, buffers, light, stack, model, layer.texture(slot == EquipmentSlot.LEGS));
            }
        }, items);
    }

    @Override
    public void onDisconnect(Runnable callback) {
        Objects.requireNonNull(callback);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
    }
    // mryt233 改（2026-11-10）：只把客户端原生事件转换为公共回调。
    @Override
    public void cutout(Block... blocks) {
        BlockRenderLayerMap.INSTANCE.putBlocks(
            RenderType.cutout(), blocks);
    }

    @Override
    public void screens(ScreenInit initializer) {
        Objects.requireNonNull(initializer);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenHooks hooks = initializer.init(screen, widget ->
                Screens.getButtons(screen).add(widget));
            if (hooks == null) return;
            ScreenEvents.beforeRender(screen).register((s, graphics, x, y, delta) -> hooks.beforeRender().run());
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> hooks.scroll().allow(x, y, vertical));
        });
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        Objects.requireNonNull(callback);
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) ->
            !callback.render(outline.blockState(), outline.blockPos(), outline.cameraX(), outline.cameraY(), outline.cameraZ(), context.matrixStack(), outline.vertexConsumer()));
    }
}
