package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.loader.PayloadHandler;
import cn.blockforge.orient.framework.game.ConfigBase;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraft.network.chat.Component;

import java.util.function.Function;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Fabric 客户端专用网络接线：只在客户端环境被 {@link FabricNetworkApi} 加载，服务端不会触达。
 * 作者：mryt233
 */
final class FabricClientNetworkHandlers {
    private FabricClientNetworkHandlers() {
    }

    static <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
                                                            PayloadHandler<T> handler) {
        RuntimeStats.ChannelStats stats = RuntimeStats.get().channel(type);
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            stats.handled(RuntimeStats.Direction.S2C);
            handler.handle(payload, () -> Minecraft.getInstance().player);
        });
    }

    /** 握手：核对不通过就带原因断开（处理器在客户端主线程执行）。 */
    static void registerHello(Function<FrameworkHello, Component> verify) {
        ClientPlayNetworking.registerGlobalReceiver(FrameworkHello.TYPE, (payload, context) -> {
            Component reason = verify.apply(payload);
            if (reason != null) {
                context.client().getConnection().getConnection().disconnect(reason);
            }
        });
    }

    static void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    /** 断线时输出客户端侧摘要，便于核对「单次发包 / 各处理一次」。 */
    static void installLifecycle() {
        // 配置门面：只有客户端主线程读客户端那份配置（前置检查 F3）
        ConfigBase.setClientThreadCheck(() -> Minecraft.getInstance().isSameThread());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            RuntimeStats.get().logSummary("client-disconnect"));
    }
}
