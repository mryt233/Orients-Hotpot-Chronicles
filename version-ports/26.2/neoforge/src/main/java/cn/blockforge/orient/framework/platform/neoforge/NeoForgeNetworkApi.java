// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.PayloadHandler;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeoForge 通用网络实现：通道先登记、在 RegisterPayloadHandlersEvent 窗口统一落地；
 * ProtocolPolicy.OPTIONAL 走 registrar.optional，重复通道只登记一次。作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 F1 / W2）：RegisterPayloadHandlersEvent 过了以后再登记直接报错
 * （以前悄悄进队列、永远不落地）；统计改用按通道缓存的计数。</p>
 */
public final class NeoForgeNetworkApi implements NetworkApi {
    private static final Logger LOGGER = LoggerFactory.getLogger("orient-framework");
    private static final int PROTOCOL_VERSION = 1;

    private record Channel<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                           StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                           PayloadHandler<T> handler,
                                                           boolean toClient,
                                                           ProtocolPolicy policy) {
    }

    private final List<Channel<?>> channels = new CopyOnWriteArrayList<>();
    private final Set<String> registered = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    @Override
    public <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                            PayloadHandler<T> clientHandler, ProtocolPolicy policy) {
        queue(new Channel<>(type, codec, clientHandler, true, policy));
    }

    @Override
    public <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
                                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                            PayloadHandler<T> serverHandler, ProtocolPolicy policy) {
        queue(new Channel<>(type, codec, serverHandler, false, policy));
    }

    private void queue(Channel<?> channel) {
        if (closed) {
            throw new IllegalStateException("payload registration window already closed: " + channel.type().id()
                + "（请在模组构造 / 注册阶段登记）");
        }
        // 同一个通道 ID 可以合法地同时用于 S2C 和 C2S，去重键必须带方向
        if (!registered.add(channel.type().id() + "|" + (channel.toClient() ? "S2C" : "C2S"))) {
            LOGGER.warn("[ORIENT-FW] duplicate channel registration ignored: {}", channel.type().id());
            return;
        }
        channels.add(channel);
    }

    /** 由框架 Runtime 在 RegisterPayloadHandlersEvent 中调用，只落地已登记通道。 */
    void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar base = event.registrar(Integer.toString(PROTOCOL_VERSION));
        closed = true;
        for (Channel<?> channel : channels) {
            registerOne(base, channel);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerOne(PayloadRegistrar base, Channel channel) {
        PayloadRegistrar registrar = channel.policy() == ProtocolPolicy.OPTIONAL ? base.optional() : base;
        RuntimeStats.ChannelStats stats = RuntimeStats.get().channel(channel.type());
        RuntimeStats.Direction direction = channel.toClient() ? RuntimeStats.Direction.S2C : RuntimeStats.Direction.C2S;
        if (channel.toClient()) {
            registrar.playToClient(channel.type(), channel.codec(), (payload, context) -> {
                stats.handled(direction);
                channel.handler().handle(payload, context::player);
            });
        } else {
            registrar.playToServer(channel.type(), channel.codec(), (payload, context) -> {
                stats.handled(direction);
                channel.handler().handle(payload, context::player);
            });
        }
        stats.registered(direction, channel.policy().name());
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        RuntimeStats.ChannelStats stats = RuntimeStats.get().channel(payload.type());
        if (!canSendTo(player, payload.type())) {
            stats.skipped(RuntimeStats.Direction.S2C);
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
        stats.sent(RuntimeStats.Direction.S2C);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (!FMLEnvironment.getDist().isClient()) {
            throw new IllegalStateException("sendToServer is client-only");
        }
        // mryt233 改（26.1）：客户端发包从 PacketDistributor 挪到了 ClientPacketDistributor
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        RuntimeStats.get().channel(payload.type()).sent(RuntimeStats.Direction.C2S);
    }

    @Override
    public boolean canSendTo(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }

    @Override
    public int protocolVersion() {
        return PROTOCOL_VERSION;
    }
}
