// 作者：Codex；按 Claude 已定的 1.20.1 网络接口迁移。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class OpenSauceRestockPayload {
    public static final Identifier ID = GeneratedMod.id("open_sauce_restock");
    private OpenSauceRestockPayload() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> {
                if (player.squaredDistanceTo(pos.toCenterPos()) > 64
                    || !(player.getWorld().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) return;
                player.openHandledScreen(be);
            });
        });
    }
}
