package cn.blockforge.orient.framework.platform.forge;

import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;
import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.PayloadHandler;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import cn.blockforge.orient.framework.loader.RuntimeStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** 每个原有包 ID 使用独立 SimpleChannel，协议不匹配拒绝连接。作者：mryt233 */
public final class ForgeNetworkApi implements NetworkApi {
    private static final int VERSION = PROTOCOL_VERSION;
    private record Registration<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> handler,
            boolean toClient, ProtocolPolicy policy) { }
    private record ClientPacket(CustomPacketPayload payload) { }
    private record ServerPacket(CustomPacketPayload payload) { }
    private final List<Registration<?>> registrations = new ArrayList<>();
    private final Set<String> registered = new HashSet<>();
    private final Map<ResourceLocation, SimpleChannel> channels = new ConcurrentHashMap<>();
    private boolean closed;

    @Override public <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> handler, ProtocolPolicy policy) {
        queue(new Registration<>(type, codec, handler, true, policy));
    }
    @Override public <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> handler, ProtocolPolicy policy) {
        queue(new Registration<>(type, codec, handler, false, policy));
    }
    private void queue(Registration<?> registration) {
        if (closed) throw new IllegalStateException("payload registration window already closed: " + registration.type().id());
        if (!registered.add(registration.type().id() + "|" + registration.toClient())) return;
        registrations.add(registration);
    }
    void finishRegistration() {
        if (closed) return;
        closed = true;
        Map<ResourceLocation, Boolean> required = new LinkedHashMap<>();
        for (Registration<?> r : registrations) required.merge(r.type().id(), r.policy() == ProtocolPolicy.REQUIRED, Boolean::logicalOr);
        required.forEach((id, mandatory) -> channels.put(id, NetworkRegistry.newSimpleChannel(id,
            () -> Integer.toString(VERSION), version -> accepted(version, mandatory), version -> accepted(version, mandatory))));
        for (Registration<?> r : registrations) registerOne(r);
    }
    private static boolean accepted(String remote, boolean required) {
        return Integer.toString(VERSION).equals(remote) || (!required &&
            (NetworkRegistry.ABSENT.equals(remote) || NetworkRegistry.ACCEPTVANILLA.equals(remote)));
    }
    private <T extends CustomPacketPayload> void registerOne(Registration<T> r) {
        SimpleChannel channel = channels.get(r.type().id());
        RuntimeStats.Direction direction = r.toClient() ? RuntimeStats.Direction.S2C : RuntimeStats.Direction.C2S;
        RuntimeStats.get().channel(r.type()).registered(direction, r.policy().name());
        if (r.toClient()) channel.registerMessage(0, ClientPacket.class,
            (packet, buffer) -> encode(r, packet.payload(), buffer),
            buffer -> new ClientPacket(r.codec().decode(new RegistryFriendlyByteBuf(buffer))),
            (packet, context) -> handle(r, packet.payload(), context), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        else channel.registerMessage(1, ServerPacket.class,
            (packet, buffer) -> encode(r, packet.payload(), buffer),
            buffer -> new ServerPacket(r.codec().decode(new RegistryFriendlyByteBuf(buffer))),
            (packet, context) -> handle(r, packet.payload(), context), Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void encode(Registration<T> r, CustomPacketPayload payload, FriendlyByteBuf buffer) {
        r.codec().encode(new RegistryFriendlyByteBuf(buffer), (T) payload);
    }
    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void handle(Registration<T> r, CustomPacketPayload payload,
                                                               Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            RuntimeStats.get().channel(r.type()).handled(r.toClient() ? RuntimeStats.Direction.S2C : RuntimeStats.Direction.C2S);
            if (r.handler() != null) r.handler().handle((T) payload, () -> r.toClient()
                ? ForgeClientNetwork.player() : context.getSender());
        });
    }
    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        RuntimeStats.ChannelStats stats = RuntimeStats.get().channel(payload.type());
        if (!canSendTo(player, payload.type())) { stats.skipped(RuntimeStats.Direction.S2C); return; }
        channels.get(payload.type().id()).send(PacketDistributor.PLAYER.with(() -> player), new ClientPacket(payload));
        stats.sent(RuntimeStats.Direction.S2C);
    }
    @Override public void sendToServer(CustomPacketPayload payload) {
        if (!FMLEnvironment.dist.isClient()) throw new IllegalStateException("sendToServer is client-only");
        SimpleChannel channel = channels.get(payload.type().id());
        if (channel == null) throw new IllegalStateException("Unregistered payload " + payload.type().id());
        channel.sendToServer(new ServerPacket(payload));
        RuntimeStats.get().channel(payload.type()).sent(RuntimeStats.Direction.C2S);
    }
    @Override public boolean canSendTo(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        SimpleChannel channel = channels.get(type.id());
        return channel != null && player.connection != null && channel.isRemotePresent(player.connection.connection);
    }
    @Override public int protocolVersion() { return VERSION; }
}
