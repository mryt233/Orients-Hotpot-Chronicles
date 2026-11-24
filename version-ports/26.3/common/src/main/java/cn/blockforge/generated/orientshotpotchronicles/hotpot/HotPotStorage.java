// mryt233 改：26.2 存档入口；第三批 3G 存读档改走适配小包，键名、结构与兜底值不变。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreWriter;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoredItem;
import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 锅的存档读写（从 {@link HotPotBlockEntity} 抽出来）：
 * 汤底、81 格食材、熟度、经验。方块存档和「装着东西的锅」物品共用这一份格式。
 * 作者：mryt233
 */
public final class HotPotStorage {
    /** 存档里每边汤底的字段名（第 0 边沿用老存档的 Base）。 */
    static final String[] BASE_KEYS = {"Base", "BaseB", "BaseC", "BaseD"};
    /** 存档里每边红汤辣度档位的字段名，第 0 边沿用老式短名 Spicy。 */
    static final String[] SPICY_KEYS = {"Spicy", "SpicyB", "SpicyC", "SpicyD"};

    private HotPotStorage() {
    }

    /** 写锅里的东西（整锅打包物品组件用）。 */
    static void write(HotPotBlockEntity be, CompoundTag nbt, HolderLookup.Provider registries) {
        // 旧键仍可读到下个大版本，但新写出的内容不保留旧序号。
        for (String key : BASE_KEYS) {
            nbt.remove(key);
        }
        BeIo.write(nbt, registries, w -> writeContents(be, w));
    }

    /** 写锅里的东西（方块实体 ValueOutput 存档用）。 */
    static void write(HotPotBlockEntity be, ValueOutput nbt) {
        BeIo.write(nbt, w -> writeContents(be, w));
    }

    private static void writeContents(HotPotBlockEntity be, StoreWriter w) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int side = 0; side < be.bases.length; side++) {
            SoupIds.write(w, side, be.bases[side]);
            w.putInt(SPICY_KEYS[side], be.spicyLevels[side]);
        }
        w.putIntArray("Doneness", be.slots.doneness().clone());
        byte[] cookedBytes = new byte[slots];
        for (int i = 0; i < slots; i++) {
            cookedBytes[i] = (byte) (be.slots.cooked()[i] ? 1 : 0);
        }
        w.putByteArray("Cooked", cookedBytes);
        w.putIntArray("Overcook", be.slots.overcook().clone());
        int[] xp = new int[slots];
        for (int i = 0; i < slots; i++) {
            xp[i] = Math.round(be.slots.xpValue()[i] * 100F);
        }
        w.putIntArray("SlotXp", xp);
        w.putFloat("PendingXp", be.slots.pendingXp());
        NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++) {
            list.set(i, be.slots.ingredients().getItem(i));
        }
        w.putItems(list);
    }

    /** 读锅里的东西（整锅打包物品组件用）；读完顺手修复方块状态。 */
    static void read(HotPotBlockEntity be, CompoundTag nbt, HolderLookup.Provider registries) {
        BeIo.read(nbt, registries, r -> readContents(be, r));
        finishRead(be);
    }

    /** 读锅里的东西（方块实体 ValueInput 读档用）；读完顺手修复方块状态。 */
    static void read(HotPotBlockEntity be, ValueInput nbt) {
        BeIo.read(nbt, r -> readContents(be, r));
        finishRead(be);
    }

    private static void readContents(HotPotBlockEntity be, StoreReader r) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int side = 0; side < be.bases.length; side++) {
            be.bases[side] = SoupIds.read(r, side);
            int spicy = r.getInt(SPICY_KEYS[side], 0);
            be.spicyLevels[side] = Math.max(0, Math.min(3, spicy));
        }
        Arrays.fill(be.slots.doneness(), 0);
        int[] loaded = r.getIntArray("Doneness");
        System.arraycopy(loaded, 0, be.slots.doneness(), 0, Math.min(loaded.length, slots));
        Arrays.fill(be.slots.cooked(), false);
        byte[] cookedBytes = r.getByteArray("Cooked");
        for (int i = 0; i < slots; i++) {
            be.slots.cooked()[i] = i < cookedBytes.length && cookedBytes[i] != 0;
        }
        Arrays.fill(be.slots.overcook(), 0);
        int[] overdue = r.getIntArray("Overcook");
        System.arraycopy(overdue, 0, be.slots.overcook(), 0, Math.min(overdue.length, slots));
        Arrays.fill(be.slots.xpValue(), 0F);
        int[] xp = r.getIntArray("SlotXp");
        for (int i = 0; i < Math.min(xp.length, slots); i++) {
            be.slots.xpValue()[i] = xp[i] / 100F;
        }
        be.slots.pendingXp(r.getFloat("PendingXp", 0F));
        loadIngredients(be, r);
    }

    private static void finishRead(HotPotBlockEntity be) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = be.slots.ingredients().getItem(i);
            be.slots.tracked()[i] = stack.copy();
            be.slots.cookDur()[i] = stack.isEmpty() ? 0 : HotPotCooking.cookTicks(be.getLevel(), stack);
        }
        be.markNeedsVisualRepair();
        be.repairVisualState();
    }

    /** 从存档恢复 81 格食材：优先按 Slot 索引放回原槽。 */
    private static void loadIngredients(HotPotBlockEntity be, StoreReader reader) {
        int slots = HotPotBlockEntity.SLOTS;
        be.slots.ingredients().clearContent();
        int sequential = 0;
        for (StoredItem entry : reader.items()) {
            int slot = entry.hasSlot() ? entry.slot() : sequential;
            ItemStack stack = entry.stack();
            if (slot >= 0 && slot < slots && !stack.isEmpty()) {
                if (stack.getCount() > 1) {
                    stack = stack.copyWithCount(1);
                }
                be.slots.ingredients().setItem(slot, stack);
            }
            sequential++;
        }
    }
}
