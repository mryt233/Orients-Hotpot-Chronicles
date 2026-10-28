//  移植：0.1.4-alpha 冻结底本，保留原作者署名。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.minecraft.entity.player.PlayerEntity;
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
public final class ChopsticksAimPayload {
    public static final net.minecraft.util.Identifier ID = GeneratedMod.id("chopsticks_aim");
    private ChopsticksAimPayload() {}

    /** 各玩家最近报的那一下：哪口锅、哪一小格、服务端收到时的世界时间。 */
    private static final Map<UUID, Aim> PENDING = new ConcurrentHashMap<>();

    private record Aim(BlockPos pos, int slot, long time) {
    }

    // ：1.20.1通道先读完buffer，再在服务端主线程记住准星。
    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(ID,
            (server, player, handler, buf, responseSender) -> {
                BlockPos pos = buf.readBlockPos();
                int slot = buf.readVarInt();
                server.execute(() -> remember(player, pos, slot));
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
