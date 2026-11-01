package cn.blockforge.generated.orientshotpotchronicles.devtest;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 假玩家的「空连接」。
 *
 * <p>Fabric 的 {@code FakePlayer} 默认不在玩家列表里，{@code list} 看不到、{@code @a} 选不到，
 * 所以 {@code /recipe give}、{@code /give}、{@code /advancement} 这类「先找玩家」的命令一律失败。
 * 要让假玩家真正登录，{@link net.minecraft.server.players.PlayerList#placeNewPlayer} 需要一个连接；
 * 这个类就是那个连接：所有发包都丢掉，状态切换也不碰 netty 的 channel（原版实现会在
 * {@code setupInboundProtocol} 里操作 channel，没有真实连接就会 NPE）。</p>
 *
 * <p>只在开发环境使用，打 jar 时整个 devtest 包被排除（见 build.gradle）。</p>
 * 作者：mryt233 */
class DevFakeConnection extends Connection {
    private static final SocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 0);

    DevFakeConnection() {
        super(PacketFlow.SERVERBOUND);
    }

    @Override
    public void send(Packet<?> packet) {
        // 假玩家不需要收包
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener callbacks) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener callbacks, boolean flush) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> state, T packetListener) {
        // 原版这里会 syncUninterruptibly(channel.writeAndFlush(...))，假连接没有 channel
    }

    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> state) {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener listener) {
    }

    @Override
    public void flushChannel() {
    }

    @Override
    public void tick() {
    }

    @Override
    public void runOnceConnected(java.util.function.Consumer<Connection> task) {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isMemoryConnection() {
        return true;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return ADDRESS;
    }

    @Override
    public String getLoggableAddress(boolean logIps) {
        return "[dev-fake]";
    }

    @Override
    public void disconnect(Component reason) {
    }

    @Override
    public void disconnect(DisconnectionDetails info) {
    }
}
