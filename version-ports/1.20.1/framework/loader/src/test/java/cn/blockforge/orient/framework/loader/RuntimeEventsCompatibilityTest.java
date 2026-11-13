// 作者：mryt233
package cn.blockforge.orient.framework.loader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeEventsCompatibilityTest {
    @Test
    void existingEventsImplementationDoesNotNeedNewMethods() {
        RuntimeEvents original = new RuntimeEvents() {
            public void onCommands(CommandRegistration callback) { }
            public void onPlayerJoin(Consumer<ServerPlayer> callback) { }
            public void onPlayerLeave(Consumer<ServerPlayer> callback) { }
            public void onServerStopping(Consumer<MinecraftServer> callback) { }
            public void flammable(Block block, int encouragement, int flammability) { }
        };
        for (Runnable call : List.<Runnable>of(
                () -> original.onServerStarting(server -> fail()),
                () -> original.onServerStopped(server -> fail()),
                () -> original.beforeBlockBreak((level, player, pos, state, entity) -> false))) {
            assertTrue(assertThrows(UnsupportedOperationException.class, call::run).getMessage().contains("0.1.6"));
        }
    }
}
