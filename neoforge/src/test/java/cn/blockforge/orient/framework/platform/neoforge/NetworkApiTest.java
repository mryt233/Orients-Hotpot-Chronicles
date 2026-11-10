// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NetworkApiTest {
    private record Payload(int value) implements CustomPacketPayload {
        static final Type<Payload> TYPE = new Type<>(ResourceLocation.parse("test:codec"));
        static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = new StreamCodec<>() {
            public Payload decode(RegistryFriendlyByteBuf buffer) { return new Payload(buffer.readVarInt()); }
            public void encode(RegistryFriendlyByteBuf buffer, Payload value) { buffer.writeVarInt(value.value()); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    @Test void registeredCodecRoundTripsBothDirectionsAndKeepsPolicy() throws Exception {
        var api = new NeoForgeNetworkApi();
        api.registerS2C(Payload.TYPE, Payload.CODEC, (payload, context) -> {}, ProtocolPolicy.REQUIRED);
        api.registerC2S(Payload.TYPE, Payload.CODEC, (payload, context) -> {}, ProtocolPolicy.OPTIONAL);
        var field = NeoForgeNetworkApi.class.getDeclaredField("channels"); field.setAccessible(true);
        var channels = (List<?>)field.get(api);
        assertEquals(2, channels.size());
        for (int i = 0; i < channels.size(); i++) {
            var channel = channels.get(i);
            var policy = channel.getClass().getDeclaredMethod("policy"); policy.setAccessible(true);
            assertEquals(i == 0 ? ProtocolPolicy.REQUIRED : ProtocolPolicy.OPTIONAL, policy.invoke(channel));
            var codecMethod = channel.getClass().getDeclaredMethod("codec"); codecMethod.setAccessible(true);
            @SuppressWarnings("unchecked") var codec = (StreamCodec<RegistryFriendlyByteBuf,Payload>)codecMethod.invoke(channel);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                codec.encode(buffer, new Payload(-173));
                assertEquals(new Payload(-173), codec.decode(buffer)); assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void closedWindowRejectsBothPolicies() throws Exception {
        var api = new NeoForgeNetworkApi();
        var field = NeoForgeNetworkApi.class.getDeclaredField("closed"); field.setAccessible(true); field.set(api, true);
        for (var policy : ProtocolPolicy.values()) {
            assertThrows(IllegalStateException.class, () -> api.registerS2C(Payload.TYPE, Payload.CODEC, null, policy));
            assertThrows(IllegalStateException.class, () -> api.registerC2S(Payload.TYPE, Payload.CODEC, null, policy));
        }
    }
}
