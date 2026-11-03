// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** 从总览请求打开蘸料补货菜单，两平台共用服务端校验。作者：mryt233 */
public record OpenSauceRestockPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenSauceRestockPayload> ID = new Type<>(HotPot.id("open_sauce_restock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSauceRestockPayload> CODEC =
        BlockPos.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(OpenSauceRestockPayload::new, OpenSauceRestockPayload::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void handle(ServerPlayer player) {
        if (player.distanceToSqr(pos.getCenter()) > 64 || !player.level().hasChunkAt(pos)
            || !(player.level().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return;
        }
        HotpotScreens.openSauce(player, be);
    }
}
