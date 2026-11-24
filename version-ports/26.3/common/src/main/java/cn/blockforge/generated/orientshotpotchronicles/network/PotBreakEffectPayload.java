// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;

/** 破坏前的灶台状态快照；只负责客户端表现，不携带扣物或修改世界的指令。 */
public record PotBreakEffectPayload(BlockPos pos, int stoveStateId) implements CustomPacketPayload {
    public static final Type<PotBreakEffectPayload> ID = new Type<>(HotPot.id("pot_break_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PotBreakEffectPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, PotBreakEffectPayload::pos,
        ByteBufCodecs.VAR_INT, PotBreakEffectPayload::stoveStateId, PotBreakEffectPayload::new);
    public static Consumer<PotBreakEffectPayload> clientReceiver = payload -> { };

    public PotBreakEffectPayload {
        pos = pos.immutable();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
