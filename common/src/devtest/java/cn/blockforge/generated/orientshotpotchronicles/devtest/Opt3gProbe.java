// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 第三批 3G：只在开发服务端运行的取证。全部走真实入口
 * （{@code ServerPlayerGameMode.useItemOn} / {@code destroyBlock} / {@code useItem}），
 * 不直接调业务方法冒充交互。这里只记录事实、不做断言：改前改后两份结果由外面逐字比对。
 * 作者：mryt233
 */
public final class Opt3gProbe {
    private Opt3gProbe() {
    }

    public static String run(ServerPlayer player, String action, BlockPos pos) {
        var level = player.serverLevel();
        JsonObject out = new JsonObject();
        try {
            switch (action) {
                // 锅打包成物品再放回：真挖锅（战利品表 copy_components）→ 捡起掉落的锅 → 真右键放回来。
                case "packpot" -> packPot(player, pos, out);
                // 条凳：真拿着条凳右键放一张，再右键它往上叠，再空手右键坐下。
                case "bench" -> bench(player, pos, out);
                // 砧板：放食材 → 菜刀切几刀。
                case "cut" -> cut(player, pos, out);
                // 砧板：放食材 → 擀面杖捣几刀。
                case "pound" -> pound(player, pos, out);
                // 饮料锅：瓶子倒进去 → 空杯舀出来（原版炼药锅的真实右键）。
                case "cauldron" -> cauldron(player, pos, out);
                case "held" -> held(player, pos, out);
                default -> throw new IllegalArgumentException(action);
            }
        } catch (RuntimeException | Error error) {
            out.addProperty("error", error.getClass().getName() + ": " + error.getMessage());
        }
        return out.toString();
    }

    /** 先记真实方块入口的结果，再走完整原版分发；假连接开菜单的异常只用于证明已进入空手分支。 */
    private static void held(ServerPlayer player, BlockPos pos, JsonObject out) {
        var level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var hit = hit(pos, 0.5, 0.9, 0.5);
        var raw = level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player,
            InteractionHand.MAIN_HAND, hit);
        out.addProperty("blockResult", raw.name());
        out.addProperty("before", contents(level, pos));
        try {
            var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
            out.addProperty("result", result.name());
            out.addProperty("emptyEntered", raw == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                && result.consumesAction());
        } catch (RuntimeException error) {
            boolean entered = java.util.Arrays.stream(error.getStackTrace()).anyMatch(frame ->
                frame.getMethodName().equals("useWithoutItem"));
            out.addProperty("emptyEntered", entered);
            out.addProperty("menuUnavailable", entered);
            if (!entered) throw error;
        }
        out.addProperty("after", contents(level, pos));
        out.addProperty("hand", hand(player, level));
    }

    private static void packPot(ServerPlayer player, BlockPos pos, JsonObject out) {
        var level = player.serverLevel();
        out.addProperty("before", contents(level, pos));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        Vec3 aim = Vec3.atCenterOf(pos);
        player.moveTo(aim.x, aim.y - player.getEyeHeight(), aim.z - 2, 0, 0);
        out.addProperty("removed", player.gameMode.destroyBlock(pos));
        ItemStack dropped = ItemStack.EMPTY;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4))) {
            if (entity.getItem().getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock() instanceof HotPotBlock) {
                dropped = entity.getItem().copy();
                entity.discard();
                break;
            }
        }
        out.addProperty("droppedItem", dropped.save(level.registryAccess()).toString());
        BlockPos target = pos.east(3);
        level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, dropped);
        player.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 2.5, 0, 0);
        out.addProperty("placeResult", String.valueOf(player.gameMode.useItemOn(player, level, dropped,
            InteractionHand.MAIN_HAND, new BlockHitResult(
                new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5),
                Direction.UP, target.below(), false))));
        out.addProperty("placed", level.getBlockState(target).toString());
        out.addProperty("after", contents(level, target));
        out.addProperty("handAfterPlace", hand(player, level));
    }

    private static void bench(ServerPlayer player, BlockPos pos, JsonObject out) {
        var level = player.serverLevel();
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        player.getInventory().clearContent();
        ItemStack benchItem = new ItemStack(HotPotContent.TALL_BENCHES.get(0));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, benchItem.copy());
        out.addProperty("placeResult", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos.below(), 0.5, 1.0, 0.5))));
        out.addProperty("placed", level.getBlockState(pos).toString());
        out.addProperty("handAfterPlace", hand(player, level));
        // 第二张：拿着条凳右键刚放好的那张，往上一格叠。
        player.setItemInHand(InteractionHand.MAIN_HAND, benchItem.copy());
        out.addProperty("stackResult", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.06, 0.5))));
        out.addProperty("stacked", level.getBlockState(pos.above()).toString());
        out.addProperty("handAfterStack", player.getMainHandItem().getCount());
        // 空手右键坐下：叠起来的凳子只坐最顶上那一张（中间层按原实现不让坐）。
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        out.addProperty("sitResult", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos.above(), 0.5, 0.9, 0.5))));
        out.addProperty("riding", player.getVehicle() != null ? "seat" : "none");
        if (player.getVehicle() != null) {
            player.getVehicle().discard();
            player.stopRiding();
        }
    }

    private static void cut(ServerPlayer player, BlockPos pos, JsonObject out) {
        var level = player.serverLevel();
        prepareCuttingBoard(player, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POTATO));
        out.addProperty("putResult", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.9, 0.5))));
        out.addProperty("put", boardTag(level, pos));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HotPotContent.KITCHEN_KNIFE));
        JsonArray steps = new JsonArray();
        for (int i = 0; i < 6; i++) {
            String result = String.valueOf(player.gameMode.useItemOn(player, level, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.9, 0.5)));
            steps.add(result + "|" + boardTag(level, pos) + "|dmg=" + player.getMainHandItem().getDamageValue()
                + "|drops=" + drops(level, pos));
        }
        out.add("cuts", steps);
    }

    private static void pound(ServerPlayer player, BlockPos pos, JsonObject out) {
        var level = player.serverLevel();
        prepareCuttingBoard(player, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF));
        out.addProperty("putResult", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.9, 0.5))));
        out.addProperty("put", boardTag(level, pos));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HotPotContent.ROLLING_PIN));
        JsonArray steps = new JsonArray();
        for (int i = 0; i < 4; i++) {
            String result = String.valueOf(player.gameMode.useItemOn(player, level, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.9, 0.5)));
            steps.add(result + "|" + boardTag(level, pos) + "|dmg=" + player.getMainHandItem().getDamageValue()
                + "|drops=" + drops(level, pos));
        }
        out.add("pounds", steps);
    }

    private static void cauldron(ServerPlayer player, BlockPos pos, JsonObject out) {
        var level = player.serverLevel();
        level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        player.getInventory().clearContent();
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HotPotContent.ORANGE_DRINK));
        out.addProperty("pour", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.9, 0.5))));
        out.addProperty("afterPour", level.getBlockState(pos).toString());
        out.addProperty("handAfterPour", hand(player, level));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HotPotContent.GLASS_CUP));
        out.addProperty("scoop", String.valueOf(player.gameMode.useItemOn(player, level,
            player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(pos, 0.5, 0.9, 0.5))));
        out.addProperty("afterScoop", level.getBlockState(pos).toString());
        out.addProperty("handAfterScoop", hand(player, level));
    }

    private static void prepareCuttingBoard(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, HotPotContent.CUTTING_BOARD.defaultBlockState(), 3);
        player.getInventory().clearContent();
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0, 0);
    }

    /** 手里的物品转成 SNBT；空栈不能 save，统一写 empty。 */
    private static String hand(ServerPlayer player, net.minecraft.world.level.Level level) {
        ItemStack stack = player.getMainHandItem();
        return stack.isEmpty() ? "empty" : stack.save(level.registryAccess()).toString();
    }

    private static BlockHitResult hit(BlockPos pos, double dx, double y, double dz) {
        return new BlockHitResult(new Vec3(pos.getX() + dx, pos.getY() + y, pos.getZ() + dz),
            Direction.UP, pos, false);
    }

    /** 锅 / 灶台那格的存档内容：只取和玩法有关的几个键。 */
    private static String contents(net.minecraft.world.level.Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        var tag = be == null ? new net.minecraft.nbt.CompoundTag() : be.saveWithoutMetadata(level.registryAccess());
        JsonObject out = new JsonObject();
        out.addProperty("Base", tag.contains("Base") ? tag.getInt("Base") : -99);
        out.addProperty("Spicy", tag.contains("Spicy") ? tag.getInt("Spicy") : -99);
        out.addProperty("Items", tag.getList("Items", 10).toString());
        out.addProperty("Doneness", java.util.Arrays.toString(tag.getIntArray("Doneness")));
        out.addProperty("tag", tag.toString());
        return out.toString();
    }

    private static String boardTag(net.minecraft.world.level.Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (!(be instanceof CuttingBoardBlockEntity board)) {
            return "no-board";
        }
        String held = board.item().isEmpty() ? "empty" : board.item().save(level.registryAccess()).toString();
        return held + "|cuts=" + board.cutCount() + "|pounds=" + board.pounds();
    }

    private static String drops(net.minecraft.world.level.Level level, BlockPos pos) {
        JsonObject out = new JsonObject();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8))) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString();
            out.addProperty(id, out.has(id) ? out.get(id).getAsInt() + entity.getItem().getCount()
                : entity.getItem().getCount());
        }
        return out.toString();
    }
}
