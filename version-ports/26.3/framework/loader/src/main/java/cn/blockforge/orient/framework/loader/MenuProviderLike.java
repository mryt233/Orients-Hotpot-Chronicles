package cn.blockforge.orient.framework.loader;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** 平台无关的菜单描述（标题 + 服务端菜单工厂）。作者：mryt233 */
public interface MenuProviderLike {
    Component title();

    AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player);
}