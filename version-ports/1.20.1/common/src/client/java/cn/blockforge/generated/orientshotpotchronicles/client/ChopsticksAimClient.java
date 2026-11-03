package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotInteraction;
import cn.blockforge.generated.orientshotpotchronicles.network.ChopsticksAimPayload;
import cn.blockforge.orient.framework.client.ClientNetwork;
import net.minecraft.client.Minecraft;

/** 火锅业务：空筷子右键锅时报送看到的小格（客户端）。作者：mryt233 */
public final class ChopsticksAimClient {
    private ChopsticksAimClient() {
    }

    public static void install() {
        HotPotInteraction.clientChopsticksAim = (be, cell) -> {
            float partial = Minecraft.getInstance().getFrameTime();
            int slot = HotPotBlockEntityRenderer.shownSlot(be, cell, partial);
            if (slot >= 0) {
                ClientNetwork.send(new ChopsticksAimPayload(be.getBlockPos(), slot));
            }
        };
    }
}
