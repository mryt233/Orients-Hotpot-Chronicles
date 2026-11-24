package cn.blockforge.orient.framework.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

/**
 * 客户端发包桥：只负责发送原版 payload，具体内容在内容模块。作者：mryt233
 * mryt233 改（2026-11-11）：Runtime 0.2.0 收紧旧字段，只通过 install() / send() 接线。
 */
public final class ClientNetwork {
    /** mryt233 改（2026-11-11）：平台发送入口仅供内部持有。 */
    private static volatile Consumer<CustomPacketPayload> sender = payload -> {
        throw new IllegalStateException("Client network platform not installed");
    };

    private ClientNetwork() {
    }

    public static void install(Consumer<CustomPacketPayload> send) {
        sender = send;
    }

    public static void send(CustomPacketPayload payload) {
        sender.accept(payload);
    }
}
