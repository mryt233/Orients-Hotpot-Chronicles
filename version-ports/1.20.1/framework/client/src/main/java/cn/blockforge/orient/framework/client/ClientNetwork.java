package cn.blockforge.orient.framework.client;

import cn.blockforge.orient.framework.compat.CustomPacketPayload;

import java.util.function.Consumer;

/**
 * 客户端发包桥：只负责发送原版 payload，具体内容在内容模块。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4）：统一走 install() / send()；旧字段只为兼容保留。
 */
public final class ClientNetwork {
    /** 已弃用：旧内容包（Runtime 0.1.1）直接读写这个字段，保留二进制兼容，0.2.0 改为私有；新代码用 install / send。 */
    @Deprecated(since = "0.1.2", forRemoval = true)
    public static volatile Consumer<CustomPacketPayload> sender = payload -> {
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
