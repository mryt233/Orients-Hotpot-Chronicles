package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.game.ConfigBase;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 客户端专用接线：只在客户端环境被 {@link NeoForgeFrameworkPlatform} 加载。
 * 断线时输出客户端侧摘要，便于核对「单次发包 / 各处理一次」。作者：mryt233
 */
public final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {
    }

    public static void installLifecycle(IEventBus modBus) {
        NeoForgeClientContent.install(modBus);
        installLifecycle();
    }

    public static void installLifecycle() {
        // 配置门面：只有客户端主线程读客户端那份配置（前置检查 F3，mryt233 改 2026-11-01）
        ConfigBase.setClientThreadCheck(
            () -> Minecraft.getInstance().isSameThread());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
            RuntimeStats.get().logSummary("client-disconnect"));
    }
}
