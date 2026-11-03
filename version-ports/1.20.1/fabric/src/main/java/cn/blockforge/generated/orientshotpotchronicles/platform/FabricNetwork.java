package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.network.ChopsticksAimPayload;
import cn.blockforge.generated.orientshotpotchronicles.network.ConfigPayload;
import cn.blockforge.generated.orientshotpotchronicles.network.OpenSauceRestockPayload;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 火锅的通道登记：通过框架 {@link NetworkApi} 统一注册（方向 / 策略 / 行为统计由 Runtime 负责）。
 * 数据、codec 与请求校验仍在 common。作者：mryt233
 */
public final class FabricNetwork {
    private FabricNetwork() {
    }

    public static void register() {
        NetworkApi network = Framework.platform().network();
        network.registerS2C(ConfigPayload.ID, ConfigPayload.CODEC,
            (payload, context) -> acceptClient(payload), ProtocolPolicy.OPTIONAL);
        network.registerC2S(ChopsticksAimPayload.ID, ChopsticksAimPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
        network.registerC2S(OpenSauceRestockPayload.ID, OpenSauceRestockPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendConfig(handler.player));
    }

    private static void acceptClient(ConfigPayload payload) {
        try {
            var parsed = JsonParser.parseString(payload.json());
            if (parsed.isJsonObject()) {
                Config.acceptServer(parsed.getAsJsonObject());
            } else {
                HotPot.LOGGER.warn("[hotpot] 客户端收到的配置不是 JSON 对象");
            }
        } catch (RuntimeException e) {
            HotPot.LOGGER.warn("[hotpot] 客户端收到的数值配置解析失败", e);
        }
    }

    private static void sendConfig(ServerPlayer player) {
        Framework.platform().network().sendToPlayer(player,
            new ConfigPayload(Config.serverSnapshot().toString()));
    }

    public static void broadcast(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(FabricNetwork::sendConfig);
    }
}
