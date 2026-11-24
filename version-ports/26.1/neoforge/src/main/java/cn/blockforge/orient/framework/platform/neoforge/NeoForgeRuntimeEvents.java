// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.RuntimeEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** NeoForge 通用运行事件与方块属性实现。作者：mryt233 */
final class NeoForgeRuntimeEvents implements RuntimeEvents {
    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void beforeBlockBreak(BlockBreak callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent e) -> {
            if (e.getLevel() instanceof Level level && e.getPlayer() != null) {
                if (callback.cancel(level, e.getPlayer(), e.getPos(), e.getState(), level.getBlockEntity(e.getPos()))) {
                    e.setCanceled(true);
                }
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
            if (e.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void onPlayerLeave(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                callback.accept(player);
            }
        });
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> callback.accept(e.getServer()));
    }

    @Override
    public void onChunkLoad(BiConsumer<ServerLevel, LevelChunk> callback) {
        Objects.requireNonNull(callback);
        record Pending(ServerLevel level, ChunkPos pos) {
        }
        Queue<Pending> pending = new ConcurrentLinkedQueue<>();
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (e.getLevel() instanceof ServerLevel serverLevel && e.getChunk() instanceof LevelChunk chunk) {
                pending.add(new Pending(serverLevel, chunk.getPos()));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            MinecraftServer server = e.getServer();
            Pending entry;
            while ((entry = pending.poll()) != null) {
                if (entry.level().getServer() != server) {
                    continue;
                }
                LevelChunk loaded = entry.level().getChunkSource().getChunkNow(entry.pos().x(), entry.pos().z());
                if (loaded != null) {
                    callback.accept(entry.level(), loaded);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) ->
            pending.removeIf(entry -> entry.level().getServer() == e.getServer()));
    }

    @Override
    public void onServerTick(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> callback.accept(e.getServer()));
    }

    @Override
    public void flammable(Block block, int encouragement, int flammability) {
        if (encouragement < 0 || flammability < 0) throw new IllegalArgumentException("negative fire value");
        ((FireBlock) Blocks.FIRE).setFlammable(Objects.requireNonNull(block), encouragement, flammability);
    }
}
