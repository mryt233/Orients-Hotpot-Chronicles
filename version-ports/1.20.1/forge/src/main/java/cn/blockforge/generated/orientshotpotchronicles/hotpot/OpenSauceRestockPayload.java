package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** 消息仅从客户端发往服务器；位置格式、距离和方块校验与Fabric底本一致。 */
public record OpenSauceRestockPayload(BlockPos pos) {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        GeneratedMod.id("open_sauce_restock"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OpenSauceRestockPayload.class,
            (message, buf) -> buf.writeBlockPos(message.pos()),
            buf -> new OpenSauceRestockPayload(buf.readBlockPos()),
            OpenSauceRestockPayload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static void handle(OpenSauceRestockPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            BlockPos pos = message.pos();
            if (player == null || player.squaredDistanceTo(pos.toCenterPos()) > 64
                || !(player.getWorld().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) return;
            NetworkHooks.openScreen(player, be, pos);
        });
        context.setPacketHandled(true);
    }

    public static void send(BlockPos pos) {
        CHANNEL.sendToServer(new OpenSauceRestockPayload(pos));
    }
}
