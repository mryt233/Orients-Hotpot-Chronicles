// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.compat.BlockPosCodecs;
import cn.blockforge.orient.framework.compat.ByteBufCodecs;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

/** 破坏前的灶台状态快照；只负责客户端表现，不携带扣物或修改世界的指令（1.20.1 适配）。 */
public record PotBreakEffectPayload(BlockPos pos, int stoveStateId) implements CustomPacketPayload {
    public static final Type<PotBreakEffectPayload> ID = new Type<>(HotPot.id("pot_break_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PotBreakEffectPayload> CODEC = StreamCodec.composite(
        BlockPosCodecs.STREAM_CODEC.cast(), PotBreakEffectPayload::pos,
        ByteBufCodecs.VAR_INT, PotBreakEffectPayload::stoveStateId, PotBreakEffectPayload::new);
    public static Consumer<PotBreakEffectPayload> clientReceiver = payload -> { };

    public PotBreakEffectPayload {
        pos = pos.immutable();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
