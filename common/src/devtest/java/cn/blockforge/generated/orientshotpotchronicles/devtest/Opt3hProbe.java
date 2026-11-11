// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 开发服务端补充取证；操作走原版交互 / 中键入口，观察完整物品组件和屏幕属性。 */
public final class Opt3hProbe {
    private Opt3hProbe() { }

    public static String run(ServerPlayer player, String action, BlockPos pos) {
        var level = player.serverLevel();
        JsonObject out = new JsonObject();
        if (action.equals("seed")) {
            level.random.setSeed(310031L);
            out.addProperty("seed", 310031L);
        } else if (action.equals("snapshot")) {
            var entity = level.getBlockEntity(pos);
            out.addProperty("block", level.getBlockState(pos).toString());
            if (entity != null) out.addProperty("tag", entity.saveWithoutMetadata(level.registryAccess()).toString());
            if (entity instanceof HotPotBlockEntity pot) {
                JsonArray properties = new JsonArray();
                for (int i = 0; i < HotPotBlockEntity.PROP_COUNT; i++) properties.add(pot.propertyAt(i));
                out.add("properties", properties);
            }
        } else if (action.equals("clone")) {
            var state = level.getBlockState(pos);
            out.addProperty("item", save(state.getBlock().getCloneItemStack(level, pos, state), player));
        } else if (action.equals("digpot")) {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
            player.moveTo(pos.getX() + .6, pos.getY() + .9 - player.getEyeHeight(), pos.getZ() - 2, 0, 0);
            out.addProperty("removed", player.gameMode.destroyBlock(pos));
            out.addProperty("blockAfter", level.getBlockState(pos).toString());
            JsonArray drops = new JsonArray();
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)))
                drops.add(save(entity.getItem(), player));
            out.add("drops", drops);
        } else if (action.equals("carry")) {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setShiftKeyDown(true);
            player.moveTo(pos.getX() + .6, pos.getY() + 1.9 - player.getEyeHeight(), pos.getZ() - 1, 0, 33.7F);
            try {
                out.addProperty("result", player.gameMode.useItemOn(player, level, player.getMainHandItem(),
                    InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(pos.getX()+.6, pos.getY()+.9, pos.getZ()+.85),
                        Direction.UP, pos, false)).name());
            } finally { player.setShiftKeyDown(false); }
            JsonArray inventory = new JsonArray();
            for (int i = 0; i < 36; i++) if (!player.getInventory().getItem(i).isEmpty())
                inventory.add(i + ":" + save(player.getInventory().getItem(i), player));
            out.add("inventory", inventory);
            // 把实际端起的物品选到主手，下一条放置命令继续用这件原物品。
            for (int i = 0; i < 36; i++) if (!player.getInventory().getItem(i).isEmpty()) {
                ItemStack held = player.getInventory().removeItemNoUpdate(i);
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                break;
            }
            out.addProperty("hand", save(player.getMainHandItem(), player));
        } else if (!action.equals("inventory")) {
            throw new IllegalArgumentException(action);
        }
        JsonArray inventory = new JsonArray();
        for (int i = 0; i < 36; i++) if (!player.getInventory().getItem(i).isEmpty())
            inventory.add(i + ":" + save(player.getInventory().getItem(i), player));
        out.add("inventoryAfter", inventory);
        out.addProperty("handAfter", save(player.getMainHandItem(), player));
        return out.toString();
    }

    private static String save(ItemStack stack, ServerPlayer player) {
        return stack.isEmpty() ? "empty" : stack.save(player.level().registryAccess()).toString();
    }
}
