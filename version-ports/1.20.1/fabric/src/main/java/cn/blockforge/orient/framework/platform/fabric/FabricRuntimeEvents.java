// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.loader.RuntimeEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Fabric 游戏事件适配。 */
final class FabricRuntimeEvents implements RuntimeEvents {
    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        ServerLifecycleEvents.SERVER_STARTING.register(callback::accept);
    }

    @Override
    public void onServerStopped(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        ServerLifecycleEvents.SERVER_STOPPED.register(callback::accept);
    }

    @Override
    public void beforeBlockBreak(BlockBreak callback) {
        Objects.requireNonNull(callback);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) ->
            !callback.cancel(world, player, pos, state, entity));
    }

    @Override
    public void onChunkLoad(BiConsumer<ServerLevel, LevelChunk> callback) {
        Objects.requireNonNull(callback);
        record Pending(ServerLevel level, ChunkPos pos) {
        }
        Queue<Pending> pending = new ConcurrentLinkedQueue<>();
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (level != null && chunk != null) {
                pending.add(new Pending(level, chunk.getPos()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Pending entry;
            while ((entry = pending.poll()) != null) {
                if (entry.level().getServer() != server) {
                    continue;
                }
                LevelChunk loaded = entry.level().getChunkSource().getChunkNow(entry.pos().x, entry.pos().z);
                if (loaded != null) {
                    callback.accept(entry.level(), loaded);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
            pending.removeIf(entry -> entry.level().getServer() == server));
    }

    @Override
    public void onCommands(CommandRegistration callback) {
        Objects.requireNonNull(callback);
        CommandRegistrationCallback.EVENT.register(callback::register);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> callback.accept(handler.player));
    }

    @Override
    public void onPlayerLeave(Consumer<ServerPlayer> callback) {
        Objects.requireNonNull(callback);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> callback.accept(handler.player));
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        ServerLifecycleEvents.SERVER_STOPPING.register(callback::accept);
    }

    @Override
    public void onServerTick(Consumer<MinecraftServer> callback) {
        Objects.requireNonNull(callback);
        ServerTickEvents.END_SERVER_TICK.register(callback::accept);
    }

    @Override
    public void flammable(Block block, int encouragement, int flammability) {
        if (encouragement < 0 || flammability < 0) throw new IllegalArgumentException("negative fire value");
        FlammableBlockRegistry.getDefaultInstance().add(Objects.requireNonNull(block), encouragement, flammability);
    }
}
