package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.network.ChopsticksAimPayload;
import cn.blockforge.generated.orientshotpotchronicles.network.ConfigPayload;
import cn.blockforge.generated.orientshotpotchronicles.network.OpenSauceRestockPayload;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 火锅的通道登记：通过框架 {@link NetworkApi} 统一注册，落在 Runtime 的
 * RegisterPayloadHandlersEvent 窗口里（方向 / 策略 / 行为统计由 Runtime 负责）。
 * 数据、codec 与请求校验仍在 common。作者：mryt233
 */
public final class NeoForgeNetwork {
    private NeoForgeNetwork() {
    }

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.OnDatapackSyncEvent event) -> event.getRelevantPlayers().forEach(NeoForgeNetwork::sendConfig));
        NetworkApi network = Framework.platform().network();
        network.registerS2C(ConfigPayload.ID, ConfigPayload.CODEC,
            NeoForgeNetwork::acceptClient, ProtocolPolicy.OPTIONAL);
        network.registerC2S(ChopsticksAimPayload.ID, ChopsticksAimPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
        network.registerC2S(OpenSauceRestockPayload.ID, OpenSauceRestockPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
    }

    private static void acceptClient(ConfigPayload payload,
                                     cn.blockforge.orient.framework.loader.PayloadHandler.Context context) {
        try {
            var parsed = JsonParser.parseString(payload.json());
            if (parsed.isJsonObject()) {
                Config.acceptServer(parsed.getAsJsonObject());
                cn.blockforge.generated.orientshotpotchronicles.hotpot.RecipeSync.accept(payload.recipes());
            } else {
                HotPot.LOGGER.warn("[hotpot] 客户端收到的配置不是 JSON 对象");
            }
        } catch (RuntimeException e) {
            HotPot.LOGGER.warn("[hotpot] 客户端收到的配置解析失败", e);
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 配置与配方由 OnDatapackSyncEvent 在登录时统一下发。
        }
    }

    private static void sendConfig(ServerPlayer player) {
        // 假玩家/未协商该通道的连接不能接收自定义包，不伪造协商状态。
        Framework.platform().network().sendToPlayer(player,
            new ConfigPayload(Config.serverSnapshot().toString(), cn.blockforge.generated.orientshotpotchronicles.hotpot.RecipeSync.snapshot(player.level().getServer())));
    }

    public static void broadcast(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(NeoForgeNetwork::sendConfig);
    }
}
