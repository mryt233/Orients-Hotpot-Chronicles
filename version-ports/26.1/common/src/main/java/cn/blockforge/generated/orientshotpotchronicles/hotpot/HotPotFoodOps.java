// 作者：mryt233；mryt233 改（2026-11-11）
// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.SLOTS;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.consumeHand;
import static cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity.key;

/** 下锅、取件与经验结算；原实体保留公开入口。 */
final class HotPotFoodOps {
    private HotPotFoodOps() { }

    static boolean insertHeld(HotPotBlockEntity be, Player player, ItemStack chopsticks, int slot) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !be.hasPot()) {
            return false;
        }
        ItemStack food = Chopsticks.held(chopsticks);
        if (!HotPotCooking.acceptable(be.getLevel(), food)) {
            player.sendOverlayMessage(Component.translatable(key("msg.cannot_cook")));
            return false;
        }
        if (slot < 0 || slot >= SLOTS) {
            player.sendOverlayMessage(Component.translatable(key("msg.cell_full")));
            return false;
        }
        if (!be.slots.ingredients().getItem(slot).isEmpty()) {
            player.sendOverlayMessage(Component.translatable(key("msg.slot_occupied")));
            return false;
        }
        be.slots.ingredients().setItem(slot, food.copyWithCount(1));
        Chopsticks.hold(chopsticks, ItemStack.EMPTY);
        be.onSlotStackChanged(slot);
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.35F);
        return true;
    }

    static boolean pourPlate(HotPotBlockEntity be, Player player, InteractionHand hand) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !be.hasPot()) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        // mryt233 改（4H）：统一走 TableObjects 门面判断可自动摆上桌且非整件摆放的容器
        PlateBlock plateBlock = TableObjects.placedFor(held);
        if (plateBlock == null || TableObjects.keepsSelf(held)) {
            return false;
        }
        ItemContainerContents contents = held.getOrDefault(
            DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> foods = NonNullList.withSize(plateBlock.size().capacity(), ItemStack.EMPTY);
        contents.copyInto(foods);
        if (foods.stream().allMatch(ItemStack::isEmpty)) {
            player.sendOverlayMessage(Component.translatable(key("msg.plate_empty")));
            return false;
        }
        int moved = 0;
        for (int i = 0; i < foods.size(); i++) {
            ItemStack food = foods.get(i);
            if (food.isEmpty() || !HotPotCooking.acceptable(be.getLevel(), food)) {
                continue;
            }
            int slot = be.randomEmptySlot();
            if (slot < 0) {
                break;
            }
            be.slots.ingredients().setItem(slot, food.copyWithCount(1));
            be.onSlotStackChanged(slot);
            foods.set(i, ItemStack.EMPTY);
            moved++;
        }
        if (moved == 0) {
            player.sendOverlayMessage(Component.translatable(key("msg.plate_nothing_to_pour")));
            return false;
        }
        ItemStack plate = held.getCount() > 1 ? held.split(1) : held;
        plate.set(DataComponents.CONTAINER,
            ItemContainerContents.fromItems(foods));
        PlateItem.syncPlateModel(plate);
        if (plate != held && !player.getInventory().add(plate)) {
            player.drop(plate, false);
        }
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.4F);
        return true;
    }

    static boolean insertIngredient(HotPotBlockEntity be, Player player, InteractionHand hand) {
        int slot = be.firstEmptySlot();
        if (slot < 0) {
            player.sendOverlayMessage(Component.translatable(key("msg.pot_full")));
            return false;
        }
        return be.insertIngredient(player, hand, slot);
    }

    static boolean insertIngredient(HotPotBlockEntity be, Player player, InteractionHand hand, int slot) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !be.hasPot()) {
            return false;
        }
        ItemStack handStack = player.getItemInHand(hand);
        if (!HotPotCooking.acceptable(be.getLevel(), handStack)) {
            return false;
        }
        if (slot < 0 || slot >= SLOTS) {
            player.sendOverlayMessage(Component.translatable(key("msg.cell_full")));
            return false;
        }
        if (!be.slots.ingredients().getItem(slot).isEmpty()) {
            player.sendOverlayMessage(Component.translatable(key("msg.slot_occupied")));
            return false;
        }
        be.slots.ingredients().setItem(slot, handStack.copyWithCount(1));
        consumeHand(player, hand, Items.AIR);
        be.onSlotStackChanged(slot);
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.ITEM_PICKUP,
            SoundSource.BLOCKS, 0.35F, 1.35F);
        return true;
    }

    static boolean collectCookedSlot(HotPotBlockEntity be, Player player, int slot) {
        // mryt233 改（2026-11-05，r136a）：捞起来的是熟虾 / 热豆腐块，记一条对应的成就
        ItemStack taking = slot >= 0 && slot < SLOTS ? be.slots.ingredients().getItem(slot) : ItemStack.EMPTY;
        int delivered = be.transferCookedSlot(player, slot);
        if (delivered > 0) {
            HotPotCriterion.fire(player, "pot_cooked");
            HotPotCookingRun.fireTakeCriteria(player, taking);
            be.finishCollection();
        }
        return delivered > 0;
    }

    static boolean pickWithChopsticks(HotPotBlockEntity be, Player player, ItemStack chopsticks, int slot) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || slot < 0 || slot >= SLOTS || !be.slots.cooked()[slot]
            || !Chopsticks.held(chopsticks).isEmpty()) {
            return false;
        }
        ItemStack stored = be.slots.ingredients().getItem(slot);
        if (stored.isEmpty()) {
            return false;
        }
        float slotXp = be.slots.xpValue()[slot];
        // mryt233 改（2026-11-05，r136a）：夹起来的是熟虾 / 热豆腐块，记一条对应的成就
        HotPotCookingRun.fireTakeCriteria(player, stored);
        Chopsticks.hold(chopsticks, stored.copyWithCount(1));
        if (stored.getCount() > 1) {
            ItemStack rest = stored.copyWithCount(stored.getCount() - 1);
            be.slots.ingredients().setItem(slot, rest);
            be.slots.tracked()[slot] = rest.copy();
        } else {
            be.slots.ingredients().setItem(slot, ItemStack.EMPTY);
            be.resetSlot(slot, ItemStack.EMPTY);
        }
        HotPotCookingRun.grantExperience(be, slotXp, 1);
        HotPotCriterion.fire(player, "pot_cooked");
        be.finishCollection();
        return true;
    }

    static boolean pickShown(HotPotBlockEntity be, Player player, ItemStack chopsticks, int bigCell) {
        int shown = be.shownIn(player, bigCell);
        if (shown >= 0) {
            if (!be.slots.cooked()[shown]) {
                player.sendOverlayMessage(Component.translatable(key("msg.not_cooked_yet")));
                return true;
            }
            return be.pickWithChopsticks(player, chopsticks, shown);
        }
        return be.pickWithChopsticks(player, chopsticks, be.firstCookedSlotInCell(bigCell));
    }

    static int shownIn(HotPotBlockEntity be, Player player, int bigCell) {
        int shown = ChopsticksAim.take(player, be.getBlockPos());
        return shown >= 0 && shown < SLOTS && be.firstSlotInCell(bigCell, slot -> slot == shown) == shown
            && !be.slots.ingredients().getItem(shown).isEmpty() ? shown : -1;
    }

    static boolean collectShown(HotPotBlockEntity be, Player player, int bigCell) {
        int shown = be.shownIn(player, bigCell);
        if (shown >= 0) {
            if (!be.slots.cooked()[shown]) {
                player.sendOverlayMessage(Component.translatable(key("msg.not_cooked_yet")));
                return true;
            }
            return be.collectCookedSlot(player, shown);
        }
        return be.collectCookedSlot(player, be.firstCookedSlotInCell(bigCell));
    }

    static int transferCookedSlot(HotPotBlockEntity be, Player player, int slot) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || slot < 0 || slot >= SLOTS || !be.slots.cooked()[slot]) {
            return 0;
        }
        ItemStack stored = be.slots.ingredients().getItem(slot);
        if (stored.isEmpty()) {
            be.resetSlot(slot, ItemStack.EMPTY);
            return 0;
        }

        float slotXp = be.slots.xpValue()[slot];
        // 不用 copyAndEmpty()：它会把库存里那个 ItemStack 对象自己清成 0 个。
        // 只要该对象被别处共享（旧存档、第三方配方缓存等），就会连带抹掉其它槽位。
        // 这里只接管一份拷贝，再显式把锅格置空。
        ItemStack moved = stored.copy();
        be.slots.ingredients().setItem(slot, ItemStack.EMPTY);
        be.resetSlot(slot, ItemStack.EMPTY);

        int total = moved.getCount();
        ItemStack remainder = moved.copy();
        player.getInventory().add(remainder);
        int delivered = total - remainder.getCount();
        if (!remainder.isEmpty()) {
            ItemEntity dropped = new ItemEntity(be.getLevel(),
                be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1.15, be.getBlockPos().getZ() + 0.5,
                remainder.copy(), 0.0, 0.16, 0.0);
            dropped.setDefaultPickUpDelay();
            dropped.setThrower(player);
            if (be.getLevel().addFreshEntity(dropped)) {
                delivered += remainder.getCount();
            } else {
                be.slots.ingredients().setItem(slot, remainder.copy());
                be.slots.tracked()[slot] = remainder.copy();
                be.slots.cooked()[slot] = true;
                be.slots.doneness()[slot] = HotPotCooking.FULL_DONE;
                be.slots.xpValue()[slot] = slotXp;
                be.slots.cookDur()[slot] = HotPotCooking.cookTicks(be.getLevel(), remainder);
            }
        }
        if (delivered > 0) {
            HotPotCookingRun.grantExperience(be, slotXp, delivered);
        }
        return delivered;
    }

    static void finishCollection(HotPotBlockEntity be) {
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.ITEM_PICKUP,
            SoundSource.BLOCKS, 0.35F, 1.2F);
        be.markDirtyAndSync();
    }

    static void grantTakeXp(HotPotBlockEntity be, Player player, int slot, int count) {
        HotPotCookingRun.grantTakeXp(be, player, slot, count);
    }
}
