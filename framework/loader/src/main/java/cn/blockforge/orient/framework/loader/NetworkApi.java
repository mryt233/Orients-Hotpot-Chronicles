package cn.blockforge.orient.framework.loader;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** 网络契约：C2S/S2C 统一 ProtocolPolicy；协商不等于授权。作者：mryt233 */
public interface NetworkApi {
    int PROTOCOL_VERSION = 1;
    <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
                                                     StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                     PayloadHandler<T> clientHandler, ProtocolPolicy policy);

    <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
                                                     StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                     PayloadHandler<T> serverHandler, ProtocolPolicy policy);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToServer(CustomPacketPayload payload);

    /** 未协商（或对端不支持）时返回 false；发送前必须检查。 */
    boolean canSendTo(ServerPlayer player, CustomPacketPayload.Type<?> type);

    /** 本端协议版本，握手期交换。 */
    int protocolVersion();
}