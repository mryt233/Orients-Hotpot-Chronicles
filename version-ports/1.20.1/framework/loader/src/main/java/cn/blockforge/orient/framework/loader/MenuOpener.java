package cn.blockforge.orient.framework.loader;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** 通用开界面能力；不含任何具体界面/内容类型。作者：mryt233 */
@FunctionalInterface
public interface MenuOpener {
    void open(ServerPlayer player, MenuProviderLike menu, BlockPos pos);
}