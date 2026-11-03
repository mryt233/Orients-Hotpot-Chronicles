package cn.blockforge.orient.framework.compat;

import net.minecraft.resources.ResourceLocation;

/** 载荷标识的 1.20.1 兼容契约。作者：mryt233 */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) { }
}
