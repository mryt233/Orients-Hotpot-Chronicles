// 作者：mryt233
package cn.blockforge.orient.framework.loader;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import java.util.function.Consumer;

/** 内容模块可在 runtime(hooks) 中通过 hooks.events() 登记。 */
public interface RuntimeEvents {
    @FunctionalInterface
    interface CommandRegistration {
        void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
                      Commands.CommandSelection selection);
    }

    // mryt233 改（2026-11-10）：保留默认方法，已有平台实现仍可加载。
    default void onServerStarting(Consumer<MinecraftServer> callback) {
        throw new UnsupportedOperationException("server starting requires Runtime 0.1.6");
    }

    default void onServerStopped(Consumer<MinecraftServer> callback) {
        throw new UnsupportedOperationException("server stopped requires Runtime 0.1.6");
    }

    @FunctionalInterface
    interface BlockBreak {
        /** 返回 true 取消原版破坏；回调在方块移除之前执行。 */
        boolean cancel(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                       net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                       net.minecraft.world.level.block.entity.BlockEntity entity);
    }

    default void beforeBlockBreak(BlockBreak callback) {
        throw new UnsupportedOperationException("block breaking requires Runtime 0.1.6");
    }
    /** mryt233 改（2026-11-16）：服务端区块加载完成回调，供旧存档方块与实体无损迁移使用。 */
    default void onChunkLoad(java.util.function.BiConsumer<net.minecraft.server.level.ServerLevel,
            net.minecraft.world.level.chunk.LevelChunk> callback) {
        throw new UnsupportedOperationException("chunk load requires Runtime 0.2.1");
    }
    /** mryt233 改（2026-11-16）：服务端刻末回调，供会话定时结算与限时邀约超时使用。 */
    default void onServerTick(Consumer<MinecraftServer> callback) {
        throw new UnsupportedOperationException("server tick requires Runtime 0.2.1");
    }

    void onCommands(CommandRegistration callback);

    void onPlayerJoin(Consumer<ServerPlayer> callback);

    void onPlayerLeave(Consumer<ServerPlayer> callback);

    void onServerStopping(Consumer<MinecraftServer> callback);

    /** 两个参数依次为引燃难度与燃烧速度，语义与原版 FireBlock 一致。 */
    void flammable(Block block, int encouragement, int flammability);
}
