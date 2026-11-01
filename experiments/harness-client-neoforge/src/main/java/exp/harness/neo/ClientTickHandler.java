package exp.harness.neo;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端 tick：找到锅后走真实瞄准回调 → ClientNetwork → Runtime NetworkApi，然后退出。作者：mryt233。 */
final class ClientTickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("hotpot-harness");
    private static int ticks;
    private static int aimTick = -1;

    private ClientTickHandler() {
    }

    static void install() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            ticks = 0;
            aimTick = -1;
            return;
        }
        ticks++;
        if (aimTick < 0 && ticks >= 60 && ticks % 20 == 0) {
            BlockPos pos = new BlockPos(2, -59, 0);
            if (client.level.getBlockEntity(pos) instanceof HotPotBlockEntity pot) {
                HotPotInteraction.clientChopsticksAim.accept(pot, 4);
                aimTick = ticks;
                LOGGER.info("[HARNESS] aim invoked");
            } else if (ticks % 100 == 0) {
                LOGGER.warn("[HARNESS] pot not found yet at {} player={}", pos, client.player.position());
            }
        }
        boolean timeout = aimTick < 0 && ticks >= 600;
        if ((aimTick >= 0 && ticks == aimTick + 60) || timeout) {
            LOGGER.info("[HARNESS] stopping client");
            client.level.disconnect();
            client.disconnect();
            client.stop();
        }
    }
}
