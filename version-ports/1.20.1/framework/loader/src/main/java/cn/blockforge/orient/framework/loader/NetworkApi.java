package cn.blockforge.orient.framework.loader;

import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** 网络契约：C2S/S2C 统一 ProtocolPolicy；协商不等于授权。作者：mryt233 */
public interface NetworkApi {
    /** 跨端共享的框架握手协议版本（避免依赖具体平台实现类）。 */
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