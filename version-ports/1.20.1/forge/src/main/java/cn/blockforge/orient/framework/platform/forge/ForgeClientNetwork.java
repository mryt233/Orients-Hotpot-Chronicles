package cn.blockforge.orient.framework.platform.forge;

/** 独立客户端类，专用服务端不加载 Minecraft 客户端类型。作者：mryt233 */
final class ForgeClientNetwork {
    private ForgeClientNetwork() { }
    static net.minecraft.world.entity.player.Player player() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }
}
