// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRules;
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import java.nio.charset.StandardCharsets;

/** 单个有界规则快照通道，不修改原配置包协议。 */
public record SyncRackRulesPayload(String json) implements CustomPacketPayload {
    public static final Type<SyncRackRulesPayload> ID = new Type<>(HotPot.id("sync_prep_rack_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SyncRackRulesPayload> CODEC = StreamCodec.of(
        (buf, p) -> {
            var bytes = p.json.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > RackRules.MAX_WIRE_BYTES) throw new IllegalArgumentException("rack packet too large");
            buf.writeByteArray(bytes);
        },
        buf -> new SyncRackRulesPayload(new String(buf.readByteArray(RackRules.MAX_WIRE_BYTES), StandardCharsets.UTF_8)));

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
