// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.compat.ByteBufCodecs;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;

/** 服务端下发 hotpot_tracks 音游谱面列表快照。作者：mryt233 */
public record SyncTracksPayload(String json) implements CustomPacketPayload {
    public static final Type<SyncTracksPayload> ID = new Type<>(HotPot.id("sync_tracks"));
    public static final int MAX_JSON = 524288;
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTracksPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(MAX_JSON), SyncTracksPayload::json, SyncTracksPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
