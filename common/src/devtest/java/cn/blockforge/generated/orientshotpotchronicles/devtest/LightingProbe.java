package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/** 隔离开发世界的光照/形状取证，不进入交付包，不改变游戏方块。作者：mryt233 */
public final class LightingProbe {
    private static JsonObject result = new JsonObject();
    private LightingProbe() { }

    public static void action(Minecraft mc, JsonObject request) {
        if (mc.level == null) throw new IllegalStateException("光照取证需要隔离世界");
        var args = request.getAsJsonArray("extra");
        String action = args.get(0).getAsString();
        if (action.equals("commands")) {
            var server = mc.getSingleplayerServer();
            if (server == null) throw new IllegalStateException("只允许隔离集成世界命令");
            var uuid = mc.player.getUUID();
            var commands = args.deepCopy();
            server.execute(() -> {
                var source = server.getPlayerList().getPlayer(uuid).createCommandSourceStack().withPermission(4);
                for (int i = 1; i < commands.size(); i++) server.getCommands().performPrefixedCommand(source, commands.get(i).getAsString());
            });
            return;
        }
        if (action.equals("ao")) {
            // 只改本次隔离客户端用于 A/B 对照；不是修复方案。
            mc.options.ambientOcclusion().set(args.get(1).getAsBoolean());
            mc.levelRenderer.allChanged();
            return;
        }
        if (action.equals("mill")) {
            var server = mc.getSingleplayerServer();
            if (server == null) throw new IllegalStateException("只允许隔离集成世界石磨测试");
            var uuid = mc.player.getUUID();
            BlockPos millPos = new BlockPos(args.get(1).getAsInt(), args.get(2).getAsInt(), args.get(3).getAsInt());
            JsonObject millRes = new JsonObject();
            server.execute(() -> {
                try {
                    var sPlayer = server.getPlayerList().getPlayer(uuid);
                    var sLevel = sPlayer.serverLevel();
                    sLevel.setBlockAndUpdate(millPos, HotPotContent.STONE_MILL.defaultBlockState().setValue(StoneMillBlock.FACING, Direction.NORTH));
                    var be = (StoneMillBlockEntity) sLevel.getBlockEntity(millPos);
                    boolean addedWater = be.tryAddWater(sPlayer, InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
                    boolean insertedSoy = be.tryInsertInput(sPlayer, InteractionHand.MAIN_HAND, new ItemStack(HotPotContent.SOYBEAN, 2));
                    JsonArray steps = new JsonArray();
                    for (int step = 1; step <= 4; step++) {
                        boolean ground = be.tryGrind(sPlayer, Direction.NORTH);
                        JsonObject s = new JsonObject();
                        s.addProperty("step", step);
                        s.addProperty("ok", ground);
                        s.addProperty("turns", be.turns());
                        s.addProperty("input_count", be.input().getCount());
                        s.addProperty("trough", be.troughResult().isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(be.troughResult().getItem()).toString());
                        steps.add(s);
                    }
                    ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                    sPlayer.setItemInHand(InteractionHand.MAIN_HAND, emptyBucket);
                    boolean collected = be.tryCollectTrough(sPlayer, InteractionHand.MAIN_HAND, emptyBucket);
                    millRes.addProperty("added_water", addedWater);
                    millRes.addProperty("inserted_soy", insertedSoy);
                    millRes.add("grind_steps", steps);
                    millRes.addProperty("collected", collected);
                    millRes.addProperty("trough_after_collect", be.troughResult().isEmpty());
                    millRes.addProperty("pass", addedWater && insertedSoy && collected && be.troughResult().isEmpty() && be.input().isEmpty());
                } catch (Throwable t) {
                    millRes.addProperty("error", t.toString());
                    millRes.addProperty("pass", false);
                }
                result = new JsonObject();
                result.add("mill", millRes);
            });
            return;
        }
        result = new JsonObject();
        result.addProperty("ao", mc.options.ambientOcclusion().get());
        result.addProperty("hit", String.valueOf(mc.hitResult));
        JsonArray rows = new JsonArray();
        if (action.equals("audit")) {
            BlockPos air = new BlockPos(190, 12, 190);
            for (var block : BuiltInRegistries.BLOCK) {
                var id = BuiltInRegistries.BLOCK.getKey(block);
                if (id.getNamespace().equals("orients_hotpot_chronicles")) {
                    JsonObject row = describe(mc, block.defaultBlockState(), air, false);
                    row.addProperty("registered_states", block.getStateDefinition().getPossibleStates().size());
                    rows.add(row);
                }
            }
        } else if (action.equals("scan")) {
            if ((args.size() - 1) % 3 != 0 || args.size() > 151) throw new IllegalArgumentException("光照坐标列表无效");
            for (int i = 1; i < args.size(); i += 3) {
                BlockPos pos = new BlockPos(args.get(i).getAsInt(), args.get(i+1).getAsInt(), args.get(i+2).getAsInt());
                rows.add(describe(mc, mc.level.getBlockState(pos), pos, true));
            }
        } else throw new IllegalArgumentException("未知光照动作");
        result.add("blocks", rows);
    }

    private static JsonObject describe(Minecraft mc, BlockState state, BlockPos pos, boolean model) {
        JsonObject row = new JsonObject();
        row.addProperty("state", state.toString());
        row.addProperty("class", state.getBlock().getClass().getSimpleName());
        row.addProperty("pos", pos.toShortString());
        try {
            row.addProperty("emission", state.getLightEmission());
            row.addProperty("sky", mc.level.getBrightness(LightLayer.SKY, pos));
            row.addProperty("block", mc.level.getBrightness(LightLayer.BLOCK, pos));
            row.addProperty("shade", state.getShadeBrightness(mc.level, pos));
            row.addProperty("light_block", state.getLightBlock(mc.level, pos));
            row.addProperty("full_collision", state.isCollisionShapeFullBlock(mc.level, pos));
            row.addProperty("view_blocking", state.isViewBlocking(mc.level, pos));
            row.addProperty("solid_render", state.isSolidRender(mc.level, pos));
            row.addProperty("can_occlude", state.canOcclude());
            row.addProperty("skylight_down", state.propagatesSkylightDown(mc.level, pos));
            row.addProperty("shape_light", state.useShapeForLightOcclusion());
            var shape = state.getShape(mc.level, pos);
            row.addProperty("shape", shape.toString());
            if (model) {
                JsonArray neighbors = new JsonArray();
                for (Direction direction : Direction.values()) {
                    var next = pos.relative(direction);
                    JsonObject neighbor = new JsonObject();
                    neighbor.addProperty("face", direction.getSerializedName());
                    neighbor.addProperty("state", mc.level.getBlockState(next).toString());
                    neighbor.addProperty("sky", mc.level.getBrightness(LightLayer.SKY, next));
                    neighbor.addProperty("block", mc.level.getBrightness(LightLayer.BLOCK, next));
                    neighbors.add(neighbor);
                }
                row.add("neighbors", neighbors);
                var baked = mc.getBlockRenderer().getBlockModel(state);
                row.addProperty("model_ao", baked.useAmbientOcclusion());
                JsonArray quads = new JsonArray();
                for (int face = 0; face < 7; face++) {
                    for (var quad : baked.getQuads(state, face == 6 ? null : Direction.values()[face], RandomSource.create(42))) {
                        JsonObject q = new JsonObject();
                        q.addProperty("cull", face == 6 ? "none" : Direction.values()[face].getSerializedName());
                        q.addProperty("face", quad.getDirection().getSerializedName());
                        q.addProperty("shade", quad.isShade());
                        q.addProperty("sprite", quad.getSprite().contents().name().toString());
                        JsonArray points = new JsonArray();
                        int[] vertices = quad.getVertices();
                        for (int i = 0; i < 4; i++) {
                            JsonArray xyz = new JsonArray();
                            for (int j = 0; j < 3; j++) xyz.add(Float.intBitsToFloat(vertices[i * (vertices.length / 4) + j]));
                            points.add(xyz);
                        }
                        q.add("points", points);
                        quads.add(q);
                    }
                }
                row.add("quads", quads);
            }
        } catch (RuntimeException failure) {
            row.addProperty("probe_error", failure.toString());
        }
        return row;
    }

    public static JsonObject result() { return result.deepCopy(); }
}
