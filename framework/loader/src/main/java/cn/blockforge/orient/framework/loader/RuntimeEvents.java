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

    void onCommands(CommandRegistration callback);
    void onPlayerJoin(Consumer<ServerPlayer> callback);
    void onPlayerLeave(Consumer<ServerPlayer> callback);
    void onServerStopping(Consumer<MinecraftServer> callback);

    /** 两个参数依次为引燃难度与燃烧速度，语义与原版 FireBlock 一致。 */
    void flammable(Block block, int encouragement, int flammability);
}
