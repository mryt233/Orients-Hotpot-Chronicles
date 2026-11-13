// 作者：mryt233
package cn.blockforge.orient.framework.platform.forge;

import cn.blockforge.orient.framework.loader.RuntimeEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import java.util.Objects;
import java.util.function.Consumer;

/** Forge 游戏事件适配。 */
final class ForgeRuntimeEvents implements RuntimeEvents {
    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void beforeBlockBreak(BlockBreak callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> {
            if (e.getLevel() instanceof Level world
                && callback.cancel(world, e.getPlayer(), e.getPos(), e.getState(), world.getBlockEntity(e.getPos()))) {
                e.setCanceled(true);
            }
        });
    }

    @Override
    public void onCommands(CommandRegistration callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
            callback.register(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) callback.accept(player);
        });
    }

    @Override
    public void onPlayerLeave(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) callback.accept(player);
        });
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void flammable(Block block, int encouragement, int flammability) {
        if (encouragement < 0 || flammability < 0) throw new IllegalArgumentException("negative fire value");
        Objects.requireNonNull(block);
        // 1.20.1 Forge 中 FireBlock#setFlammable 为私有方法；可燃性由方块覆写 IForgeBlock#getFlammability/getFireSpreadSpeed 控制。
    }
}
