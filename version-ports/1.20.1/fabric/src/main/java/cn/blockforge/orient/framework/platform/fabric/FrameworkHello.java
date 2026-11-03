package cn.blockforge.orient.framework.platform.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import cn.blockforge.orient.framework.compat.ByteBufCodecs;
import cn.blockforge.orient.framework.compat.StreamCodec;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Fabric 进服握手（前置检查 F2）：服务端把协议版本和「必需通道」清单发给客户端，客户端对不上就断开。
 * NeoForge 自己会按 registrar 版本和必需 payload 拒绝连接；Fabric 没有这一步，以前缺了必需通道也能进服、功能静默失效。
 * 客户端没有这个通道（不装前置，或 0.1.1 及更早的前置）时服务端跳过检查，不把老客户端踢掉。
 * 作者：mryt233（2026-11-01）
 */
record FrameworkHello(int version, List<String> required) implements CustomPacketPayload {
    static final Type<FrameworkHello> TYPE = new Type<>(new ResourceLocation("orient_framework", "hello"));
    static final StreamCodec<FriendlyByteBuf, FrameworkHello> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, FrameworkHello::version,
        ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(1024)), FrameworkHello::required,
        FrameworkHello::new);

    FrameworkHello {
        required = List.copyOf(required);
    }

    @Override
    public Type<FrameworkHello> type() {
        return TYPE;
    }

    /** 协议版本对不上时的断开原因（键在内容包语言文件里，没有翻译时显示英文）。 */
    static Component protocolMismatch(int server, int client) {
        return Component.translatableWithFallback("orient_framework.disconnect.protocol",
            "Orient Framework network protocol mismatch (server %s, client %s). Install the same mod versions as the server.",
            server, client);
    }

    /** 缺必需通道时的断开原因。 */
    static Component missingChannels(String channels) {
        return Component.translatableWithFallback("orient_framework.disconnect.missing_channels",
            "This server requires network channels your client does not have: %s. Install the same mod versions as the server.",
            channels);
    }
}
