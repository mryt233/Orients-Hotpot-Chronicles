package cn.blockforge.generated.orientshotpotchronicles.hotpot.api;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.MenuProviderLike;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 火锅的界面入口（内容侧业务 API；Loader 不含这些类型）。
 * 内部组装 {@link MenuProviderLike}，通过通用 {@link cn.blockforge.orient.framework.loader.MenuOpener} 打开。
 * 作者：mryt233
 */
public final class HotpotScreens {
    private HotpotScreens() {
    }

    public static void openPot(ServerPlayer player, HotPotBlockEntity be) {
        open(player, title(HotPotBlockEntity.key("screen.title")),
            (syncId, inventory, p) -> new HotPotScreenHandler(syncId, inventory, be), be.getBlockPos());
    }

    public static void openFuel(ServerPlayer player, StoveBlockEntity be) {
        open(player, title(HotPotBlockEntity.key("screen.stove")),
            (syncId, inventory, p) -> new StoveScreenHandler(syncId, inventory, be), be.getBlockPos());
    }

    public static void openSauce(ServerPlayer player, SauceStationBlockEntity be) {
        open(player, be.getBlockState().getBlock().getName(),
            (syncId, inventory, p) -> new SauceStationScreenHandler(syncId, inventory, be), be.getBlockPos());
    }

    private static Component title(String key) {
        return Component.translatable(key);
    }

    private static void open(ServerPlayer player, Component title, MenuFactory factory, BlockPos pos) {
        Framework.platform().menus().open(player, new Provider(title, factory), pos);
    }

    @FunctionalInterface
    private interface MenuFactory {
        AbstractContainerMenu create(int syncId, Inventory inventory, Player player);
    }

    private record Provider(Component title, MenuFactory factory) implements MenuProviderLike {
        @Override
        public Component title() {
            return title;
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
            return factory.create(syncId, inventory, player);
        }
    }
}