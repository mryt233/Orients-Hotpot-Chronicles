// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.RuntimeEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import java.util.Objects;
import java.util.function.Consumer;

/** NeoForge 游戏事件适配。 */
final class NeoForgeRuntimeEvents implements RuntimeEvents {
    // mryt233 改（2026-11-10）：仅适配原生事件，不承载内容业务。
    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartingEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void beforeBlockBreak(BlockBreak callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) -> {
            if (e.getLevel() instanceof net.minecraft.world.level.Level world
                && callback.cancel(world, e.getPlayer(), e.getPos(), e.getState(), world.getBlockEntity(e.getPos()))) {
                e.setCanceled(true);
            }
        });
    }

    @Override
    public void onCommands(CommandRegistration callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
            callback.register(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) callback.accept(player);
        });
    }

    @Override
    public void onPlayerLeave(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) callback.accept(player);
        });
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void flammable(Block block, int encouragement, int flammability) {
        if (encouragement < 0 || flammability < 0) throw new IllegalArgumentException("negative fire value");
        ((FireBlock) Blocks.FIRE).setFlammable(Objects.requireNonNull(block), encouragement, flammability);
    }
}
