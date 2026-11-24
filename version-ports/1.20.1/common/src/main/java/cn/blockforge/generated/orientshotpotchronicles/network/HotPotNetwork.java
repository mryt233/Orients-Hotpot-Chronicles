// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.network;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTracks;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 网络通道及配置、谱面、小游戏收发的唯一登记处。 */
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
        network.registerS2C(SyncTracksPayload.ID, SyncTracksPayload.CODEC,
            (payload, context) -> HotPotTracks.acceptClientJson(payload.json()), ProtocolPolicy.OPTIONAL);
        network.registerC2S(MinigameActionPayload.ID, MinigameActionPayload.CODEC,
            (payload, context) -> payload.handle((ServerPlayer) context.player()), ProtocolPolicy.REQUIRED);
        network.registerS2C(MinigameSyncPayload.ID, MinigameSyncPayload.CODEC,
            (payload, context) -> MinigameSyncPayload.clientReceiver.accept(payload.json()), ProtocolPolicy.OPTIONAL);
        // mryt233 改（2026-11-19）：表现通道可选，旧端按玩家回退原版事件。
        network.registerS2C(PotBreakEffectPayload.ID, PotBreakEffectPayload.CODEC,
            (payload, context) -> PotBreakEffectPayload.clientReceiver.accept(payload), ProtocolPolicy.OPTIONAL);
    }

    /** mryt233 改（2026-11-19）：一次效果只发给本维度区块观察者，状态在拆锅前捕获。 */
    public static void potBroken(net.minecraft.server.level.ServerLevel level,
                                 net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.level.block.state.BlockState potState) {
        var network = Framework.platform().network();
        var payload = new PotBreakEffectPayload(pos, net.minecraft.world.level.block.Block.getId(state));
        var legacyPos = net.minecraft.core.BlockPos.containing(
            cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock.potDropPoint(pos, state).add(0, -.25, 0));
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(pos), false)) {
            if (network.canSendTo(player, PotBreakEffectPayload.ID)) {
                network.sendToPlayer(player, payload);
            } else {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelEventPacket(
                    net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK,
                    legacyPos, net.minecraft.world.level.block.Block.getId(potState), false));
            }
        }
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

    public static void sendTracks(ServerPlayer player) {
        if (player != null) {
            Framework.platform().network().sendToPlayer(player, new SyncTracksPayload(HotPotTracks.toNetworkJson()));
        }
    }

    public static void broadcastTracks(MinecraftServer server) {
        if (server != null) {
            String json = HotPotTracks.toNetworkJson();
            SyncTracksPayload payload = new SyncTracksPayload(json);
            var net = Framework.platform().network();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                net.sendToPlayer(p, payload);
            }
        }
    }

    public static void sendMinigameSync(ServerPlayer player, String json) {
        if (player != null && json != null) {
            Framework.platform().network().sendToPlayer(player, new MinigameSyncPayload(json));
        }
    }
}
