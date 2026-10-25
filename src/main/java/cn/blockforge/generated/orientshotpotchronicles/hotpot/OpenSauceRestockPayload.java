package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * 蘸料台总览界面上点「补充蘸料台」（2026-10-23 用户要求）：客户端告诉服务端「给我打开这个蘸料台的补货界面」。
 * 服务端核对一下人还在旁边、那里确实是蘸料台，再打开（补货界面是有格子的容器界面，只能由服务端打开）。
 * 作者：
 */
public record OpenSauceRestockPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<OpenSauceRestockPayload> ID = new CustomPayload.Id<>(GeneratedMod.id("open_sauce_restock"));
    public static final PacketCodec<RegistryByteBuf, OpenSauceRestockPayload> CODEC =
        BlockPos.PACKET_CODEC.<RegistryByteBuf>cast().xmap(OpenSauceRestockPayload::new, OpenSauceRestockPayload::pos);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            var player = context.player();
            BlockPos pos = payload.pos();
            if (player.squaredDistanceTo(pos.toCenterPos()) > 64
                || !(player.getWorld().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
                return;
            }
            player.openHandledScreen(be);
        });
    }
}
