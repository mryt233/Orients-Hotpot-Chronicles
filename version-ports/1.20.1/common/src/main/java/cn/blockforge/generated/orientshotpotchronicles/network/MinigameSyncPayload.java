// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.compat.ByteBufCodecs;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;

import java.util.function.Consumer;

/** 服务端下发小游戏邀约、实时排名与结算结果；具体 HUD 与界面回调在客户端注入。作者：mryt233 */
public record MinigameSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<MinigameSyncPayload> ID = new Type<>(HotPot.id("minigame_sync"));
    public static final int MAX_JSON = 131072;
    public static final StreamCodec<RegistryFriendlyByteBuf, MinigameSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(MAX_JSON), MinigameSyncPayload::json, MinigameSyncPayload::new);

    public static Consumer<String> clientReceiver = payload -> { };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
