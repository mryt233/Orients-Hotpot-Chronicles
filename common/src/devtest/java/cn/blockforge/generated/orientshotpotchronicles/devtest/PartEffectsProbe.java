// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 仅隔离开发客户端使用，记录真正生成的地形粒子和服务端掉落；不进交付 JAR。 */
public final class PartEffectsProbe {
    private static final List<JsonObject> samples = new ArrayList<>();
    private static final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final AtomicReference<JsonObject> serverResult = new AtomicReference<>(new JsonObject());
    private static String error = "";
    private static boolean recording;

    private PartEffectsProbe() { }

    public static void tick() {
        if (!recording) return;
        try {
            Object engine = Minecraft.getInstance().particleEngine;
            Map<?, ?> active = (Map<?, ?>) field(engine, "particles");
            for (Object group : active.values()) for (Object particle : (Iterable<?>) group) record(particle);
            for (Object particle : (Iterable<?>) field(engine, "particlesToAdd")) record(particle);
        } catch (ReflectiveOperationException failure) {
            error = failure.toString();
            recording = false;
        }
    }

    private static void record(Object particle) throws ReflectiveOperationException {
        if (!(particle instanceof TerrainParticle) || seen.contains(particle) || samples.size() >= 512) return;
        // 队列合入时 age=0；记录首帧，而不是用下落后的坐标冒充生成位置。
        if (((Number) field(particle, "age")).intValue() != 0) return;
        seen.add(particle);
        JsonObject sample = new JsonObject();
        for (String axis : List.of("x", "y", "z")) sample.addProperty(axis, ((Number) field(particle, axis)).doubleValue());
        sample.addProperty("sprite", ((TextureAtlasSprite) field(particle, "sprite")).contents().name().toString());
        samples.add(sample);
    }

    public static void action(JsonObject request) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            throw new IllegalStateException("粒子取证只允许隔离的集成服世界");
        }
        var args = request.getAsJsonArray("extra");
        String action = args.get(0).getAsString();
        if (action.equals("clear")) {
            samples.clear();
            seen.clear();
            error = "";
            recording = true;
            serverResult.set(new JsonObject());
            return;
        }
        if (action.equals("reload")) {
            mc.reloadResourcePacks();
            return;
        }
        if (action.equals("snapshot")) return;
        BlockPos pos = new BlockPos(args.get(1).getAsInt(), args.get(2).getAsInt(), args.get(3).getAsInt());
        if (action.equals("hit")) {
            Vec3 point = new Vec3(args.get(4).getAsDouble(), args.get(5).getAsDouble(), args.get(6).getAsDouble());
            Direction face = Direction.valueOf(args.get(7).getAsString());
            var previous = mc.hitResult;
            try {
                mc.hitResult = new BlockHitResult(point, face, pos, false);
                mc.particleEngine.crack(pos, face);
                tick();
            } finally {
                mc.hitResult = previous;
            }
        } else if (action.equals("terrain")) {
            var particle = new TerrainParticle(mc.level, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5,
                0, 0, 0, mc.level.getBlockState(pos), pos);
            try {
                // NeoForge 的原生 provider 会再走 ModelData 更新；Fabric 没有此扩展。
                particle.getClass().getMethod("updateSprite", net.minecraft.world.level.block.state.BlockState.class, BlockPos.class)
                    .invoke(particle, mc.level.getBlockState(pos), pos);
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
            mc.particleEngine.add(particle);
            tick();
        } else if (action.equals("break")) {
            var server = mc.getSingleplayerServer();
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                JsonObject result = new JsonObject();
                try {
                    var player = server.getPlayerList().getPlayer(uuid);
                    var level = player.serverLevel();
                    if (!(level.getBlockEntity(pos) instanceof StoveBlockEntity stove)) {
                        throw new IllegalStateException("目标不是灶台方块实体");
                    }
                    var before = stove.toPotItem(stove.getBlockState().getValue(StoveBlock.POT).material());
                    int portions = 0;
                    for (int slot = 0; slot < stove.getContainerSize(); slot++) portions += stove.getItem(slot).getCount();
                    result.addProperty("input_portions", portions);
                    result.addProperty("before_contents", before.save(level.registryAccess()).toString());
                    stove.breakPot(player);
                    result.addProperty("pot_removed", !level.getBlockState(pos).getValue(StoveBlock.POT).hasPot());
                    result.addProperty("stove_retained", StoveBlock.isStove(level.getBlockState(pos)));
                    result.addProperty("fuel_count", stove.fuelInventory().getItem(0).getCount());
                    JsonArray items = new JsonArray();
                    for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
                        JsonObject item = new JsonObject();
                        item.addProperty("item", BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString());
                        item.addProperty("count", entity.getItem().getCount());
                        item.addProperty("y", entity.getY());
                        item.addProperty("contents", entity.getItem().save(level.registryAccess()).toString());
                        item.addProperty("same_components", net.minecraft.world.item.ItemStack.isSameItemSameComponents(before, entity.getItem()));
                        items.add(item);
                    }
                    result.add("items", items);
                } catch (Throwable failure) {
                    result.addProperty("error", failure.toString());
                }
                serverResult.set(result);
            });
        } else {
            throw new IllegalArgumentException("未知粒子取证动作：" + action);
        }
    }

    public static JsonObject status() {
        JsonObject result = new JsonObject();
        result.addProperty("error", error);
        result.addProperty("recording", recording);
        JsonArray particles = new JsonArray();
        for (JsonObject sample : samples) particles.add(sample.deepCopy());
        result.add("samples", particles);
        result.add("server", serverResult.get().deepCopy());
        return result;
    }

    private static Object field(Object instance, String name) throws ReflectiveOperationException {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
}
