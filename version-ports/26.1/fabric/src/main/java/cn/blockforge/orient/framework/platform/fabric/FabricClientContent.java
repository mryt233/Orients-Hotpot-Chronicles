// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.client.ClientContent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Objects;

/** Fabric 通用客户端能力实现：26.1 物品模型由 DynamicItemModel 自行接管渲染器表。作者：mryt233 */
final class FabricClientContent implements ClientContent {
    static void install() {
        ClientContent.install(new FabricClientContent());
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(
            MenuType<M> type, ScreenFactory<M, S> factory) {
        MenuScreens.register(type, factory::create);
    }

    @Override
    public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<T> type, BlockEntityRendererProvider<T, S> factory) {
        BlockEntityRendererRegistry.register(type, factory);
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory) {
        EntityRendererRegistry.register(type, factory);
    }

    @Override
    public void tooltip(Tooltip callback) {
        ItemTooltipCallback.EVENT.register(callback::append);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void armorModels(ArmorModel factory, Item... items) {
        ArmorRenderer.register((matrices, submitNodeCollector, stack, state, slot, light, contextModel) -> {
            HumanoidModel model = factory.model(stack, slot, contextModel);
            if (model == null) return;
            Identifier texture = armorTexture(stack, slot);
            if (texture == null) return;
            model.setupAnim(state);
            submitNodeCollector.submitModel(model, state, matrices,
                RenderTypes.armorCutoutNoCull(texture), light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
        }, items);
    }

    private static Identifier armorTexture(ItemStack stack, EquipmentSlot slot) {
        Equippable eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq == null) return null;
        Identifier asset = eq.assetId().map(ResourceKey::identifier).orElse(null);
        if (asset == null) return null;
        String layer = slot == EquipmentSlot.LEGS ? "humanoid_leggings/" : "humanoid/";
        return Identifier.fromNamespaceAndPath(asset.getNamespace(),
            "textures/entity/equipment/" + layer + asset.getPath() + ".png");
    }

    @Override
    public void onDisconnect(Runnable callback) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
    }

    // mryt233 改：26.1 按每面贴图 alpha 自动确定 cutout，无需全局登记。
    @Override
    public void cutout(Block... blocks) {
        for (Block block : blocks) {
            Objects.requireNonNull(block, "block");
        }
    }

    @Override
    public void screens(ScreenInit initializer) {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            var widgets = Screens.getWidgets(screen);
            ScreenHooks hooks = initializer.init(screen, widgets::add);
            if (hooks == null) return;
            if (hooks.beforeRender() != null) {
                ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) ->
                    hooks.beforeRender().run());
            }
            if (hooks.scroll() != null) {
                ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, h, v) ->
                    hooks.scroll().allow(x, y, v));
            }
        });
    }

    @Override
    public void blockOutline(BlockOutline callback) {
        LevelRenderEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hitResult) -> {
            if (!(hitResult instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return;
            ClientLevel level = context.level();
            Player player = Minecraft.getInstance().player;
            if (level == null || player == null) return;
            BlockPos pos = bhr.getBlockPos();
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = callback.extract(state, pos, level, player);
            if (shape == null) return;
            BlockOutlineRenderState prev = context.levelState().blockOutlineRenderState;
            boolean translucent = prev != null && prev.isTranslucent();
            boolean highContrast = prev != null && prev.highContrast();
            context.levelState().blockOutlineRenderState = prev != null
                ? new BlockOutlineRenderState(pos, translucent, highContrast, shape,
                    prev.collisionShape(), prev.occlusionShape(), prev.interactionShape())
                : new BlockOutlineRenderState(pos, false, false, shape);
        });
    }
}
