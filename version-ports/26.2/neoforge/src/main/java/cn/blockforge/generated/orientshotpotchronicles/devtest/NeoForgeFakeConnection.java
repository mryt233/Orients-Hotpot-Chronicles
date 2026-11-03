package cn.blockforge.generated.orientshotpotchronicles.devtest;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;

/** NeoForge 读取连接通道属性，需要一个不连外网的嵌入式通道。作者：mryt233 */
final class NeoForgeFakeConnection extends DevFakeConnection {
    private final EmbeddedChannel channel = new EmbeddedChannel();

    @Override
    public Channel channel() {
        return channel;
    }
}
