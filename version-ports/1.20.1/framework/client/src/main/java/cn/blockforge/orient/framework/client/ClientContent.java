// 作者：mryt233
package cn.blockforge.orient.framework.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 客户端能力入口；仅 client 层引用此类，可在 ContentModule.client(hooks) 内取得。 */
public interface ClientContent {
    static ClientContent get() {
        ClientContent instance = Installed.instance;
        if (instance == null) throw new IllegalStateException("client content platform not installed");
        return instance;
    }

    static void install(ClientContent implementation) {
        Objects.requireNonNull(implementation);
        synchronized (Installed.class) {
            if (Installed.instance != null && Installed.instance != implementation) {
                throw new IllegalStateException("client content platform already installed");
            }
            Installed.instance = implementation;
        }
    }

    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
        S create(M menu, Inventory inventory, Component title);
    }

    /** 1.20.1 没有 Item.TooltipContext，保留统一四参签名（Fabric 传 null，Forge 传 player）。 */
    @FunctionalInterface
    interface Tooltip {
        void append(ItemStack stack, Object context, TooltipFlag flag, List<Component> lines);
    }

    @FunctionalInterface
    interface ArmorModel {
        HumanoidModel<?> model(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original);
    }

    <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void menu(MenuType<M> type, ScreenFactory<M, S> factory);

    <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<T> type, BlockEntityRendererProvider<T> factory);

    <T extends Entity> void entityRenderer(EntityType<T> type, EntityRendererProvider<T> factory);

    void tooltip(Tooltip callback);

    /** 模型使用物品原有 ArmorMaterial 的贴图层；平台负责复制原版姿势与槽位可见性。 */
    void armorModels(ArmorModel factory, Item... items);

    void onDisconnect(Runnable callback);

    // mryt233 改（2026-11-10）：客户端公共接线所需的窄事件能力，旧平台保留二进制兼容。
    default void cutout(Block... blocks) {
        throw new UnsupportedOperationException("cutout requires Runtime 0.1.6");
    }

    @FunctionalInterface
    interface Scroll { boolean allow(double x, double y, double delta); }

    record ScreenHooks(Runnable beforeRender, Scroll scroll) { }

    @FunctionalInterface
    interface ScreenInit {
        ScreenHooks init(Screen screen, Consumer<AbstractWidget> add);
    }

    default void screens(ScreenInit initializer) {
        throw new UnsupportedOperationException("screen events require Runtime 0.1.6");
    }

    @FunctionalInterface
    interface BlockOutline {
        boolean render(BlockState state, BlockPos pos,
                       double cameraX, double cameraY, double cameraZ,
                       PoseStack pose, VertexConsumer vertices);
    }

    default void blockOutline(BlockOutline callback) {
        throw new UnsupportedOperationException("block outline requires Runtime 0.1.6");
    }

    final class Installed {
        private static volatile ClientContent instance;
        private Installed() { }
    }
}
