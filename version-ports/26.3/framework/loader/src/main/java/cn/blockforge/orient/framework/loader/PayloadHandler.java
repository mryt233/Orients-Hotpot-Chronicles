package cn.blockforge.orient.framework.loader;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 平台无关的 payload 处理器（主线程执行由平台保证）。作者：mryt233 */
@FunctionalInterface
public interface PayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, Context context);

    /** 平台在调用时注入的最少上下文。 */
    interface Context {
        Object player();   // ServerPlayer 或客户端玩家；平台实现负责具体类型
    }
}