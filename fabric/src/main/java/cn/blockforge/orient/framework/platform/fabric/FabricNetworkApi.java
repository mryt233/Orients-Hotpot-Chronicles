package cn.blockforge.orient.framework.platform.fabric;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.fabricmc.api.EnvType;
import net.minecraft.network.chat.Component;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.PayloadHandler;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import cn.blockforge.orient.framework.loader.RuntimeStats.Direction;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric 通用网络实现：C2S/S2C 统一注册，重复通道只登记一次并记入行为统计。
 * 客户端专用代码隔离在 {@link FabricClientNetworkHandlers}，服务端不会加载客户端类。作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 F2 / W2）：{@link ProtocolPolicy#REQUIRED} 以前在 Fabric 上没生效，
 * 现在进服时用 {@link FrameworkHello} 握手校验协议版本和必需通道；统计改用按通道缓存的计数。</p>
 */
public final class FabricNetworkApi implements NetworkApi {
    private static final Logger LOGGER = LoggerFactory.getLogger("orient-framework");

    private final Set<String> registered = ConcurrentHashMap.newKeySet();
    /** 必需通道 id（两个方向都算），握手时发给客户端核对。 */
    private final Set<String> required = ConcurrentHashMap.newKeySet();

    FabricNetworkApi() {
        PayloadTypeRegistry.playS2C().register(FrameworkHello.TYPE, FrameworkHello.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> hello(handler.player));
        if (isClient()) {
            FabricClientNetworkHandlers.registerHello(this::verify);
        }
    }

    /** 服务端：玩家进服时发握手；客户端不认识握手（没装前置或旧前置）就跳过，不踢人。 */
    private void hello(ServerPlayer player) {
        if (required.isEmpty()) {
            return;
        }
        if (!ServerPlayNetworking.canSend(player, FrameworkHello.TYPE)) {
            LOGGER.debug("[ORIENT-FW] {} cannot receive the framework handshake; skipping channel check",
                player.getGameProfile().getName());
            return;
        }
        List<String> list = new ArrayList<>(required);
        list.sort(null);
        ServerPlayNetworking.send(player, new FrameworkHello(PROTOCOL_VERSION, list));
    }

    /** 客户端：核对握手，返回断开原因；全部满足返回 null。 */
    Component verify(FrameworkHello hello) {
        if (hello.version() != PROTOCOL_VERSION) {
            return FrameworkHello.protocolMismatch(hello.version(), PROTOCOL_VERSION);
        }
        List<String> missing = new ArrayList<>();
        for (String id : hello.required()) {
            if (!registered.contains(id + "|S2C") && !registered.contains(id + "|C2S")) {
                missing.add(id);
            }
        }
        return missing.isEmpty() ? null : FrameworkHello.missingChannels(String.join(", ", missing));
    }

    private static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                            PayloadHandler<T> clientHandler, ProtocolPolicy policy) {
        if (!markRegistered(type.id(), "S2C", policy)) {
            return;
        }
        PayloadTypeRegistry.playS2C().register(type, codec);
        RuntimeStats.get().channel(type).registered(Direction.S2C, policy.name());
        if (clientHandler != null && isClient()) {
            FabricClientNetworkHandlers.registerS2C(type, clientHandler);
        }
    }

    @Override
    public <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                            PayloadHandler<T> serverHandler, ProtocolPolicy policy) {
        if (!markRegistered(type.id(), "C2S", policy)) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(type, codec);
        RuntimeStats.ChannelStats stats = RuntimeStats.get().channel(type);
        stats.registered(Direction.C2S, policy.name());
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            stats.handled(Direction.C2S);
            serverHandler.handle(payload, context::player);
        });
    }

    private boolean markRegistered(ResourceLocation id, String direction, ProtocolPolicy policy) {
        // 同一个通道 ID 可以合法地同时用于 S2C 和 C2S（两个注册表分开），去重键必须带方向
        if (!registered.add(id + "|" + direction)) {
            LOGGER.warn("[ORIENT-FW] duplicate channel registration ignored: {} {}", direction, id);
            return false;
        }
        if (policy == ProtocolPolicy.REQUIRED) {
            required.add(id.toString());
        }
        return true;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        RuntimeStats.ChannelStats stats = RuntimeStats.get().channel(payload.type());
        if (!canSendTo(player, payload.type())) {
            stats.skipped(Direction.S2C);
            return;
        }
        ServerPlayNetworking.send(player, payload);
        stats.sent(Direction.S2C);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (!isClient()) {
            throw new IllegalStateException("sendToServer is client-only");
        }
        FabricClientNetworkHandlers.send(payload);
        RuntimeStats.get().channel(payload.type()).sent(Direction.C2S);
    }

    @Override
    public boolean canSendTo(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }

    @Override
    public int protocolVersion() {
        return PROTOCOL_VERSION;
    }
}
