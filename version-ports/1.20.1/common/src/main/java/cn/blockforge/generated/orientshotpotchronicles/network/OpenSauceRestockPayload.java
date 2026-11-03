package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens;
import net.minecraft.core.BlockPos;
import cn.blockforge.orient.framework.compat.RegistryFriendlyByteBuf;
import cn.blockforge.orient.framework.compat.StreamCodec;
import cn.blockforge.orient.framework.compat.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** 从总览请求打开蘸料补货菜单，两平台共用服务端校验。作者：mryt233 */
public record OpenSauceRestockPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenSauceRestockPayload> ID = new Type<>(HotPot.id("open_sauce_restock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSauceRestockPayload> CODEC =
        cn.blockforge.orient.framework.compat.BlockPosCodecs.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(OpenSauceRestockPayload::new, OpenSauceRestockPayload::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void handle(ServerPlayer player) {
        if (player.distanceToSqr(pos.getCenter()) > 64 || !player.serverLevel().hasChunkAt(pos)
            || !(player.level().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return;
        }
        HotpotScreens.openSauce(player, be);
    }
}
