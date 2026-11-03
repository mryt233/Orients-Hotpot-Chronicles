// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.orient.framework.loader;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网络统计：同一通道每次取到同一个计数对象；摘要行格式不变（行为断言脚本按它解析）。
 * 作者：mryt233（2026-11-01，前置检查 W2 / W8）
 */
class RuntimeStatsTest {
    @Test
    void channelStatsAreCachedAndSummarized() {
        CustomPacketPayload.Type<CustomPacketPayload> type =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("test_mod", "ping"));
        RuntimeStats stats = RuntimeStats.get();
        RuntimeStats.ChannelStats channel = stats.channel(type);
        assertSame(channel, stats.channel(type));
        channel.registered(RuntimeStats.Direction.S2C, "OPTIONAL");
        channel.sent(RuntimeStats.Direction.S2C);
        channel.sent(RuntimeStats.Direction.S2C);
        channel.skipped(RuntimeStats.Direction.S2C);
        String expected = " channel=test_mod:ping direction=S2C registered=1 policy=OPTIONAL sent=2 handled=0 skipped=1";
        assertTrue(stats.summary().stream().anyMatch(line -> line.startsWith("net process=") && line.endsWith(expected)),
            String.join("\n", stats.summary()));
        // 没有任何计数的方向不输出
        assertTrue(stats.summary().stream().noneMatch(line -> line.contains("channel=test_mod:ping direction=C2S")));
    }
}
