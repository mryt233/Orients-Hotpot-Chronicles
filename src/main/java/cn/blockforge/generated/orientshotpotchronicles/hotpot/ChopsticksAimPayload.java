package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 空筷子直接右键锅时，客户端告诉服务端「我看到浮在这一格上的是哪一小格的菜」（用户 2026-10-25：
 * 看到的是牛肉，夹起来却是同一格的别的东西）。一格里几样菜轮流浮上来，轮到哪样取决于客户端的
 * 时间和火力，服务端算不准，所以由客户端报。这个包和原版的右键包走同一条连接、按顺序到，
 * 服务端处理右键时用 {@link #take} 取出来。
 *
 */
public record ChopsticksAimPayload(BlockPos pos, int slot) implements CustomPayload {
    public static final CustomPayload.Id<ChopsticksAimPayload> ID = new CustomPayload.Id<>(GeneratedMod.id("chopsticks_aim"));
    public static final PacketCodec<RegistryByteBuf, ChopsticksAimPayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, ChopsticksAimPayload::pos,
        PacketCodecs.VAR_INT, ChopsticksAimPayload::slot,
        ChopsticksAimPayload::new);

    /** 各玩家最近报的那一下：哪口锅、哪一小格、服务端收到时的世界时间。 */
    private static final Map<UUID, Aim> PENDING = new ConcurrentHashMap<>();

    private record Aim(BlockPos pos, int slot, long time) {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            remember(context.player(), payload.pos(), payload.slot());
        });
    }

    /** 记下这名玩家报的那一下（收包时调；开发环境的 /hpt aim 也调它模拟客户端）。 */
    public static void remember(PlayerEntity player, BlockPos pos, int slot) {
        PENDING.put(player.getUuid(), new Aim(pos, slot, player.getWorld().getTime()));
    }

    /** 这名玩家刚刚对这口锅报的小格；没报、报的是别的锅或者过时了（超过 10 刻）返回 -1。用一次就清掉。 */
    public static int take(PlayerEntity player, BlockPos pos) {
        Aim aim = PENDING.remove(player.getUuid());
        if (aim == null || !aim.pos().equals(pos) || player.getWorld().getTime() - aim.time() > 10) {
            return -1;
        }
        return aim.slot();
    }
}
