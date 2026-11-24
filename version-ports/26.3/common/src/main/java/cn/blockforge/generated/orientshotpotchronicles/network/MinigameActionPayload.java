// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMinigames;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** 客户端上行火锅小游戏动作包：开局、应战、敲击判定。作者：mryt233 */
public record MinigameActionPayload(int actionOrd, BlockPos potPos, int intArg, String strArg) implements CustomPacketPayload {
    public static final Type<MinigameActionPayload> ID = new Type<>(HotPot.id("minigame_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MinigameActionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MinigameActionPayload::actionOrd,
        BlockPos.STREAM_CODEC.cast(), MinigameActionPayload::potPos,
        ByteBufCodecs.VAR_INT, MinigameActionPayload::intArg,
        ByteBufCodecs.stringUtf8(1024), MinigameActionPayload::strArg,
        MinigameActionPayload::new);

    public enum Action {
        START_SOLO,
        SEND_INVITE,
        ACCEPT_INVITE,
        DECLINE_INVITE,
        START_INVITE_NOW,
        CANCEL,
        RUSH_PRESS,
        RHYTHM_HIT,
        RHYTHM_MISS,
        FORFEIT;

        public static Action byOrdinal(int ord) {
            Action[] values = values();
            return ord >= 0 && ord < values.length ? values[ord] : CANCEL;
        }
    }

    public static MinigameActionPayload of(Action action, BlockPos potPos, int intArg, String strArg) {
        return new MinigameActionPayload(
            action.ordinal(),
            potPos == null ? BlockPos.ZERO : potPos,
            intArg,
            strArg == null ? "" : strArg);
    }

    public void handle(ServerPlayer player) {
        HotPotMinigames.handleAction(player, Action.byOrdinal(actionOrd), potPos, intArg, strArg);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
