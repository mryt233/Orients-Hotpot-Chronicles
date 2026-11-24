// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotKeybinds;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 全量客户端收口探针：读取按键绑定、光影与优化模组状态，以及客户端/服务端方块实体对照。作者：mryt233 */
public final class FullCheckProbe {
    private static final String[] INSPECT_MODS = {
        "sodium", "reeses-sodium-options", "iris", "lithium", "ferritecore",
        "entityculling", "c2me", "jade", "modmenu", "placeholder-api", "modernfix"
    };

    private static volatile JsonObject lastResult = new JsonObject();

    private FullCheckProbe() {
    }

    public static JsonObject status(Minecraft mc) {
        JsonObject out = new JsonObject();
        out.add("keybinds", inspectKeybinds(mc));
        out.add("iris", inspectIris());
        JsonObject mods = new JsonObject();
        for (String modId : INSPECT_MODS) {
            mods.addProperty(modId, HotPotCompat.isModLoaded(modId));
        }
        out.add("mods", mods);
        out.add("last", lastResult.deepCopy());
        return out;
    }

    public static void action(Minecraft mc, JsonObject request) {
        JsonArray extra = request.getAsJsonArray("extra");
        if (extra == null || extra.isEmpty()) {
            throw new IllegalArgumentException("fullcheck action requires extra[0]");
        }
        String sub = extra.get(0).getAsString();
        switch (sub) {
            case "reload_packs" -> {
                mc.reloadResourcePacks();
                JsonObject res = new JsonObject();
                res.addProperty("action", "reload_packs");
                res.addProperty("done", true);
                res.addProperty("triggered", true);
                lastResult = res;
            }
            case "exec_server" -> execServer(mc, extra);
            case "inspect_blocks" -> inspectBlocks(mc, extra);
            case "interact_block" -> interactBlock(mc, extra);
            case "mine_hit" -> mineHit(mc, extra);
            case "destroy_block" -> destroyBlock(mc, extra);
            default -> throw new IllegalArgumentException("Unsupported fullcheck subcommand: " + sub);
        }
    }

    private static void execServer(Minecraft mc, JsonArray extra) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("exec_server requires an integrated singleplayer server");
        }
        List<String> commands = new ArrayList<>();
        for (int i = 1; i < extra.size(); i++) {
            commands.add(extra.get(i).getAsString());
        }
        JsonObject pending = new JsonObject();
        pending.addProperty("action", "exec_server");
        pending.addProperty("done", false);
        lastResult = pending;

        server.execute(() -> {
            JsonObject done = new JsonObject();
            done.addProperty("action", "exec_server");
            JsonArray ran = new JsonArray();
            for (String cmd : commands) {
                JsonObject entry = new JsonObject();
                entry.addProperty("cmd", cmd);
                try {
                    String raw = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                    int code = server.getCommands().getDispatcher().execute(
                        raw,
                        server.createCommandSourceStack().withSuppressedOutput().withPermission(4)
                    );
                    entry.addProperty("result", code);
                    entry.addProperty("error", "");
                } catch (Exception ex) {
                    entry.addProperty("result", -1);
                    entry.addProperty("error", String.valueOf(ex.getMessage()));
                }
                ran.add(entry);
            }
            done.add("commands", ran);
            done.addProperty("done", true);
            lastResult = done;
        });
    }

    private static void inspectBlocks(Minecraft mc, JsonArray extra) {
        if (mc.level == null) {
            throw new IllegalStateException("inspect_blocks requires client level");
        }
        MinecraftServer server = mc.getSingleplayerServer();
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 1; i + 2 < extra.size(); i += 3) {
            positions.add(new BlockPos(
                extra.get(i).getAsInt(),
                extra.get(i + 1).getAsInt(),
                extra.get(i + 2).getAsInt()
            ));
        }
        Map<String, JsonObject> clientSnapshots = new LinkedHashMap<>();
        for (BlockPos pos : positions) {
            BlockState state = mc.level.getBlockState(pos);
            BlockEntity be = mc.level.getBlockEntity(pos);
            JsonObject entry = new JsonObject();
            entry.addProperty("pos", pos.getX() + "," + pos.getY() + "," + pos.getZ());
            entry.addProperty("client_block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            entry.addProperty("client_state", state.toString());
            entry.addProperty("client_be", be == null ? "none" : be.getClass().getSimpleName());
            entry.addProperty("client_be_nbt",
                be == null ? "" : be.saveWithFullMetadata(mc.level.registryAccess()).toString());
            clientSnapshots.put(entry.get("pos").getAsString(), entry);
        }

        if (server == null) {
            JsonObject done = new JsonObject();
            done.addProperty("action", "inspect_blocks");
            JsonArray arr = new JsonArray();
            clientSnapshots.values().forEach(arr::add);
            done.add("blocks", arr);
            done.addProperty("done", true);
            lastResult = done;
            return;
        }

        JsonObject pending = new JsonObject();
        pending.addProperty("action", "inspect_blocks");
        pending.addProperty("done", false);
        lastResult = pending;

        server.execute(() -> {
            ServerLevel level = server.overworld();
            JsonObject done = new JsonObject();
            done.addProperty("action", "inspect_blocks");
            JsonArray arr = new JsonArray();
            for (BlockPos pos : positions) {
                String key = pos.getX() + "," + pos.getY() + "," + pos.getZ();
                JsonObject entry = clientSnapshots.get(key).deepCopy();
                BlockState sState = level.getBlockState(pos);
                BlockEntity sBe = level.getBlockEntity(pos);
                String sStateStr = sState.toString();
                entry.addProperty("server_block", BuiltInRegistries.BLOCK.getKey(sState.getBlock()).toString());
                entry.addProperty("server_state", sStateStr);
                entry.addProperty("server_be", sBe == null ? "none" : sBe.getClass().getSimpleName());
                entry.addProperty("server_be_nbt",
                    sBe == null ? "" : sBe.saveWithFullMetadata(level.registryAccess()).toString());
                entry.addProperty("client_server_match", entry.get("client_state").getAsString().equals(sStateStr));
                arr.add(entry);
            }
            done.add("blocks", arr);
            done.addProperty("done", true);
            lastResult = done;
        });
    }

    private static void interactBlock(Minecraft mc, JsonArray extra) {
        if (mc.player == null || mc.gameMode == null) {
            throw new IllegalStateException("interact_block requires player and gameMode");
        }
        int x = extra.get(1).getAsInt();
        int y = extra.get(2).getAsInt();
        int z = extra.get(3).getAsInt();
        String sneakMode = extra.size() > 4 ? extra.get(4).getAsString() : "false";
        boolean keepShift = "keep".equalsIgnoreCase(sneakMode);
        boolean sneak = keepShift ? mc.player.isShiftKeyDown() : Boolean.parseBoolean(sneakMode);
        double hitY = extra.size() > 5 ? extra.get(5).getAsDouble() : 0.5;
        String faceName = extra.size() > 6 ? extra.get(6).getAsString().toLowerCase(Locale.ROOT) : "up";
        Direction dir = parseDir(faceName);
        BlockPos pos = new BlockPos(x, y, z);
        double hx = x + 0.5 + dir.getStepX() * 0.5;
        double hy = y + hitY;
        double hz = z + 0.5 + dir.getStepZ() * 0.5;

        boolean prevShift = mc.player.isShiftKeyDown();
        InteractionResult result;
        try {
            if (!keepShift) {
                mc.player.setShiftKeyDown(sneak);
                mc.options.keyShift.setDown(sneak);
            }
            result = mc.gameMode.useItemOn(
                mc.player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(hx, hy, hz), dir, pos, false)
            );
        } finally {
            if (!keepShift) {
                mc.player.setShiftKeyDown(prevShift);
                mc.options.keyShift.setDown(false);
            }
        }

        JsonObject done = new JsonObject();
        done.addProperty("action", "interact_block");
        done.addProperty("pos", x + "," + y + "," + z);
        done.addProperty("sneak", sneak);
        done.addProperty("face", dir.getName());
        done.addProperty("result", result.toString());
        done.addProperty("done", true);
        lastResult = done;
    }

    private static void mineHit(Minecraft mc, JsonArray extra) {
        if (mc.player == null || mc.gameMode == null || mc.level == null) {
            throw new IllegalStateException("mine_hit requires player, level and gameMode");
        }
        int x = extra.get(1).getAsInt();
        int y = extra.get(2).getAsInt();
        int z = extra.get(3).getAsInt();
        double hitY = extra.size() > 4 ? extra.get(4).getAsDouble() : 0.8;
        String faceName = extra.size() > 5 ? extra.get(5).getAsString().toLowerCase(Locale.ROOT) : "north";
        Direction dir = parseDir(faceName);
        BlockPos pos = new BlockPos(x, y, z);
        double hx = x + 0.5 + dir.getStepX() * 0.5;
        double hy = y + hitY;
        double hz = z + 0.5 + dir.getStepZ() * 0.5;

        String particlesBefore = mc.particleEngine.countParticles();
        mc.hitResult = new BlockHitResult(new Vec3(hx, hy, hz), dir, pos, false);
        mc.gameMode.startDestroyBlock(pos, dir);
        mc.gameMode.continueDestroyBlock(pos, dir);
        mc.gameMode.stopDestroyBlock();
        String particlesAfter = mc.particleEngine.countParticles();

        JsonObject done = new JsonObject();
        done.addProperty("action", "mine_hit");
        done.addProperty("pos", x + "," + y + "," + z);
        done.addProperty("hit_y", hy);
        done.addProperty("face", dir.getName());
        done.addProperty("particles_before", particlesBefore);
        done.addProperty("particles_after", particlesAfter);
        done.addProperty("done", true);
        lastResult = done;
    }

    private static Direction parseDir(String faceName) {
        return switch (faceName) {
            case "down" -> Direction.DOWN;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "east" -> Direction.EAST;
            default -> Direction.UP;
        };
    }

    private static void destroyBlock(Minecraft mc, JsonArray extra) {
        if (mc.player == null || mc.gameMode == null) {
            throw new IllegalStateException("destroy_block requires player and gameMode");
        }
        int x = extra.get(1).getAsInt();
        int y = extra.get(2).getAsInt();
        int z = extra.get(3).getAsInt();
        String particlesBefore = mc.particleEngine.countParticles();
        boolean destroyed = mc.gameMode.destroyBlock(new BlockPos(x, y, z));
        if (mc.level != null && !mc.level.getBlockState(new BlockPos(x, y, z)).isAir()) {
            mc.particleEngine.destroy(new BlockPos(x, y, z), mc.level.getBlockState(new BlockPos(x, y, z)));
        }
        String particlesAfter = mc.particleEngine.countParticles();
        JsonObject done = new JsonObject();
        done.addProperty("action", "destroy_block");
        done.addProperty("pos", x + "," + y + "," + z);
        done.addProperty("destroyed", destroyed);
        done.addProperty("particles_before", particlesBefore);
        done.addProperty("particles_after", particlesAfter);
        done.addProperty("done", true);
        lastResult = done;
    }

    private static JsonObject inspectKeybinds(Minecraft mc) {
        JsonObject out = new JsonObject();
        KeyMapping[] mappings = mc.options != null ? mc.options.keyMappings : new KeyMapping[0];
        JsonArray names = new JsonArray();
        int identityMatches = 0;
        for (KeyMapping expected : HotPotKeybinds.all()) {
            for (KeyMapping registered : mappings) {
                if (registered == expected) {
                    identityMatches++;
                    names.add(registered.getName());
                    break;
                }
            }
        }
        out.addProperty("expected_count", HotPotKeybinds.all().length);
        out.addProperty("registered_count", identityMatches);
        out.addProperty("all_present", identityMatches == HotPotKeybinds.all().length);
        out.add("registered_names", names);
        return out;
    }

    private static JsonObject inspectIris() {
        JsonObject out = new JsonObject();
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            boolean inUse = (boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
            Object config = apiClass.getMethod("getConfig").invoke(api);
            boolean enabled = (boolean) config.getClass().getMethod("areShadersEnabled").invoke(config);

            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            String packName = String.valueOf(irisClass.getMethod("getCurrentPackName").invoke(null));
            boolean incompatible = (boolean) irisClass.getMethod("loadedIncompatiblePack").invoke(null);

            out.addProperty("loaded", true);
            out.addProperty("shader_pack_in_use", inUse);
            out.addProperty("shaders_enabled", enabled);
            out.addProperty("current_pack_name", packName);
            out.addProperty("incompatible_pack", incompatible);
            out.addProperty("error", "");
        } catch (ClassNotFoundException ignored) {
            out.addProperty("loaded", false);
            out.addProperty("shader_pack_in_use", false);
            out.addProperty("shaders_enabled", false);
            out.addProperty("current_pack_name", "");
            out.addProperty("incompatible_pack", false);
            out.addProperty("error", "");
        } catch (Exception ex) {
            out.addProperty("loaded", true);
            out.addProperty("shader_pack_in_use", false);
            out.addProperty("shaders_enabled", false);
            out.addProperty("current_pack_name", "");
            out.addProperty("incompatible_pack", false);
            out.addProperty("error", String.valueOf(ex.getMessage()));
        }
        return out;
    }
}
