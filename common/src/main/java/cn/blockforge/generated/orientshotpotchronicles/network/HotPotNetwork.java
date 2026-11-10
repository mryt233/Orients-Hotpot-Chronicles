// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 三个通道及配置收发的唯一登记处。 */
public final class HotPotNetwork {
    private HotPotNetwork() { }

    public static void register() {
        var network = Framework.platform().network();
        network.registerS2C(ConfigPayload.ID, ConfigPayload.CODEC,
            (payload, context) -> acceptClient(payload), ProtocolPolicy.OPTIONAL);
        network.registerC2S(ChopsticksAimPayload.ID, ChopsticksAimPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
        network.registerC2S(OpenSauceRestockPayload.ID, OpenSauceRestockPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
    }

    private static void acceptClient(ConfigPayload payload) {
        try {
            var parsed = JsonParser.parseString(payload.json());
            if (parsed.isJsonObject()) Config.acceptServer(parsed.getAsJsonObject());
            else HotPot.LOGGER.warn("[hotpot] 客户端收到的配置不是 JSON 对象");
        } catch (RuntimeException error) {
            HotPot.LOGGER.warn("[hotpot] 客户端收到的数值配置解析失败", error);
        }
    }

    public static void sendConfig(ServerPlayer player) {
        Framework.platform().network().sendToPlayer(player, new ConfigPayload(Config.serverSnapshot().toString()));
    }

    public static void broadcast(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(HotPotNetwork::sendConfig);
    }
}
