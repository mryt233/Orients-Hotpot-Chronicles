package exp.harness;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 生产候选包的端到端测试助爪：进服 10 秒后发一次真实的瞄准包（走火锅自己注册的通道），
 * 再过 3 秒断开并退出客户端。只在验证环境安装，不进发布产物。作者：mryt233。
 */
public final class HarnessClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("hotpot-harness");
    private int ticks;
    private int aimTick = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                ticks = 0;
                aimTick = -1;
                return;
            }
            ticks++;
            if (ticks == 100) {
                LOGGER.info("[HARNESS] diag hasChunk={} screen={} dim={} connected={}",
                    client.level.getChunkSource().hasChunk(0, 0), client.screen,
                    client.level.dimension().location(), client.getConnection() != null);
            }
            // 锅所在区块可能比玩家晚到，持续重试到找到为止（mryt233 2026-10-30）
            if (aimTick < 0 && ticks >= 60 && ticks % 20 == 0) {
                BlockPos pos = new BlockPos(2, -59, 0);
                if (client.level.getBlockEntity(pos) instanceof HotPotBlockEntity pot) {
                    // 走真实客户端路径：内容模组的瞄准回调 → ClientNetwork → Runtime NetworkApi
                    HotPotInteraction.clientChopsticksAim.accept(pot, 4);
                    aimTick = ticks;
                    LOGGER.info("[HARNESS] aim invoked");
                } else if (ticks % 100 == 0) {
                    LOGGER.warn("[HARNESS] pot not found yet at {} state={} be={} player={}", pos,
                        client.level.getBlockState(pos), client.level.getBlockEntity(pos), client.player.position());
                }
            }
            boolean timeout = aimTick < 0 && ticks >= 2400;
            if ((aimTick >= 0 && ticks == aimTick + 60) || timeout) {
                LOGGER.info("[HARNESS] stopping client");
                client.level.disconnect();
                client.disconnect();
                client.stop();
            }
        });
    }
}
