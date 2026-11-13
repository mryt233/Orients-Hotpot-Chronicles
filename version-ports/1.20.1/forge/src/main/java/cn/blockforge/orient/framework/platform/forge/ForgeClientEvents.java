package cn.blockforge.orient.framework.platform.forge;

// mryt233 改（2026-11-10）：第三批 3C 平台接线。

import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Forge 客户端专用接线：只在客户端环境被 {@link ForgeFrameworkPlatform} 加载。
 * 断线时输出客户端侧摘要，便于核对「单次发包 / 各处理一次」。作者：mryt233
 */
public final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    public static void installLifecycle(IEventBus modBus) {
        ForgeClientContent.install(modBus);
        // 配置门面：只有客户端主线程读客户端那份配置（前置检查 F3，mryt233 改 2026-11-01）
        cn.blockforge.orient.framework.game.ConfigBase.setClientThreadCheck(
            () -> net.minecraft.client.Minecraft.getInstance().isSameThread());
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
            RuntimeStats.get().logSummary("client-disconnect"));
    }
}
