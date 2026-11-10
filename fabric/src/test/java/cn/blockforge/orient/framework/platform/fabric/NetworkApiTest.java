// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.loader.NetworkApi;
import cn.blockforge.orient.framework.loader.ProtocolPolicy;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** 测真实握手 codec 和判定；绕过仅可在加载器启动期执行的注册构造器。 */
class NetworkApiTest {
    @Test void helloCodecRoundTripsEmptyAndMultipleRequiredChannels() {
        for (var channels : List.of(List.<String>of(), List.of("test:one", "test:two"))) {
            var message = new FrameworkHello(NetworkApi.PROTOCOL_VERSION, channels);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                FrameworkHello.CODEC.encode(buffer, message);
                assertEquals(message, FrameworkHello.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void helloDefensivelyCopiesChannelList() {
        var input = new java.util.ArrayList<>(List.of("test:one"));
        var message = new FrameworkHello(1, input); input.clear();
        assertEquals(List.of("test:one"), message.required());
        assertThrows(UnsupportedOperationException.class, () -> message.required().clear());
    }
    @Test void requiredAndOptionalUseActualRegistrationDecision() throws Exception {
        var api = isolated();
        var mark = FabricNetworkApi.class.getDeclaredMethod("markRegistered", ResourceLocation.class, String.class, ProtocolPolicy.class);
        mark.setAccessible(true);
        assertEquals(true, mark.invoke(api, ResourceLocation.parse("test:required"), "S2C", ProtocolPolicy.REQUIRED));
        assertEquals(true, mark.invoke(api, ResourceLocation.parse("test:optional"), "C2S", ProtocolPolicy.OPTIONAL));
        var required = FabricNetworkApi.class.getDeclaredField("required"); required.setAccessible(true);
        assertEquals(Set.of("test:required"), required.get(api));
        assertEquals(false, mark.invoke(api, ResourceLocation.parse("test:optional"), "C2S", ProtocolPolicy.REQUIRED));
        assertEquals(Set.of("test:required"), required.get(api));
    }
    @Test void missingRequiredIsRejectedAndOptionalAbsenceIsAccepted() throws Exception {
        var api = isolated();
        assertNull(api.verify(new FrameworkHello(1, List.of())));
        assertNotNull(api.verify(new FrameworkHello(1, List.of("test:required"))));
        var registered = FabricNetworkApi.class.getDeclaredField("registered"); registered.setAccessible(true);
        registered.set(api, Set.of("test:required|C2S"));
        assertNull(api.verify(new FrameworkHello(1, List.of("test:required"))));
        assertNotNull(api.verify(new FrameworkHello(2, List.of())));
    }

    private static FabricNetworkApi isolated() throws Exception {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        var unsafe = (sun.misc.Unsafe)field.get(null);
        var api = (FabricNetworkApi)unsafe.allocateInstance(FabricNetworkApi.class);
        for (String name : List.of("registered", "required")) {
            var target = FabricNetworkApi.class.getDeclaredField(name); target.setAccessible(true);
            target.set(api, java.util.concurrent.ConcurrentHashMap.newKeySet());
        }
        return api;
    }
}
