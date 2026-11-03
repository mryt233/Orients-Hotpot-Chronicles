// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopsticksAim;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** 客户端报送浮起的食材槽位；两平台共用数据与校验。作者：mryt233 */
public record ChopsticksAimPayload(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<ChopsticksAimPayload> ID = new Type<>(HotPot.id("chopsticks_aim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChopsticksAimPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC.cast(), ChopsticksAimPayload::pos,
        ByteBufCodecs.VAR_INT, ChopsticksAimPayload::slot, ChopsticksAimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void handle(ServerPlayer player) {
        if (slot < 0 || slot >= 81 || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64
            || !player.level().hasChunkAt(pos)
            || !(player.level().getBlockEntity(pos) instanceof HotPotBlockEntity)) {
            return;
        }
        ChopsticksAim.remember(player, pos, slot);
    }
}
