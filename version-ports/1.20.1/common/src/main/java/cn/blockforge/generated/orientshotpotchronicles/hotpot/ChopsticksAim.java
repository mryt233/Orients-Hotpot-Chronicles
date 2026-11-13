package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「客户端报的是哪一小格」的服务端状态（从网络包里拆出来）。
 * <p>
 * 一格里几样菜轮流浮上来，轮到哪样取决于客户端的时间和火力，服务端算不准，所以由客户端报，
 * 服务端在处理右键时取出来用一次。这里只管存和取，不关心怎么传过来的——
 * Fabric 那边由 {@code ChopsticksAimPayload} 收包后调 {@link #remember}。
 * 这样锅的核心逻辑不再依赖任何网络/加载器代码。
 * 作者：mryt233 */
public final class ChopsticksAim {
    /** 各玩家最近报的那一下：哪口锅、哪一小格、报的时候的世界时间。 */
    private record Aim(BlockPos pos, int slot, long time) {
    }

    private static final Map<UUID, Aim> PENDING = new ConcurrentHashMap<>();

    private ChopsticksAim() {
    }

    /** 记下这名玩家报的那一下（收包时调；开发环境的 /hpt aim 也调它模拟客户端）。 */
    public static void remember(Player player, BlockPos pos, int slot) {
        long now = player.level().getGameTime();
        // 报了没用上的记录（玩家下线、右键被别的东西接走）以前一直留着；记录多了顺手清掉过时的（代码检查 W8，mryt233 2026-11-01）
        if (PENDING.size() > 32) {
            PENDING.values().removeIf(aim -> now - aim.time() > STALE_TICKS || aim.time() > now);
        }
        PENDING.put(player.getUUID(), new Aim(pos, slot, now));
    }

    /** 记录多久内有效（刻）：客户端报「这一格浮上来的是第几样」到服务端处理之间要容得下网络抖动。
     *  mryt233 改（2026-11-07）：从 10 刻放宽到 20 刻，网络抖动 / 主线程忙时会误判成没报过。 */
    private static final long AIM_TTL = 20L;
    /** 这么久没被用掉的记录算垃圾，清掉（remember 里顺手清理用）。 */
    private static final long STALE_TICKS = 200L;

    /**
     * 这名玩家刚刚对这口锅报的小格；没报、报的是别的锅或者过时了返回 -1。
     * mryt233 改（2026-11-07）：只有「正好是这口锅」才消费掉记录；别的锅的记录留着，下一口锅还要用
     * （以前先 remove 再比对，会把上一口锅的记录一起删掉，两端对不齐）。作者：mryt233
     */
    public static int take(Player player, BlockPos pos) {
        UUID id = player.getUUID();
        Aim aim = PENDING.get(id);
        if (aim == null) {
            return -1;
        }
        long now = player.level().getGameTime();
        if (now - aim.time() > AIM_TTL || aim.time() > now) {
            PENDING.remove(id, aim);   // 过时的清掉
            return -1;
        }
        if (!aim.pos().equals(pos)) {
            return -1;                 // 报的是别的锅：留着，下一口锅还要用
        }
        PENDING.remove(id, aim);
        return aim.slot();
    }
}
