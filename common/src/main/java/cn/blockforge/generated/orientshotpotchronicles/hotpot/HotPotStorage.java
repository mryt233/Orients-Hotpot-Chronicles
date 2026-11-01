package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;

/**
 * 锅的存档读写（从 {@link HotPotBlockEntity} 抽出来）：
 * 汤底、81 格食材、熟度、经验。方块存档和「装着东西的锅」物品共用这一份格式。
 * <p>
 * 只做序列化，不含任何玩法判断；所以能独立成类。
 * 字段本身仍在 {@link HotPotBlockEntity} 里（同包可见），这里直接读写。
 * 作者：mryt233 */
public final class HotPotStorage {
    /** 存档里每边汤底的字段名（第 0 边沿用老存档的 Base）。 */
    static final String[] BASE_KEYS = {"Base", "BaseB", "BaseC", "BaseD"};

    private HotPotStorage() {
    }

    /** 写锅里的东西。 */
    static void write(HotPotBlockEntity be, CompoundTag nbt, HolderLookup.Provider registries) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int side = 0; side < be.bases.length; side++) {
            nbt.putInt(BASE_KEYS[side], be.bases[side]);
        }
        nbt.putIntArray("Doneness", be.doneness);
        byte[] cookedBytes = new byte[slots];
        for (int i = 0; i < slots; i++) {
            cookedBytes[i] = (byte) (be.cooked[i] ? 1 : 0);
        }
        nbt.putByteArray("Cooked", cookedBytes);
        int[] xp = new int[slots];
        for (int i = 0; i < slots; i++) {
            xp[i] = Math.round(be.xpValue[i] * 100F);
        }
        nbt.putIntArray("SlotXp", xp);
        nbt.putFloat("PendingXp", be.pendingXp);
        // 用带 Slot 索引的列表保存 81 格，避免 SimpleInventory.toNbtList 只按顺序
        // 写出、恢复时把物品重排到槽 0（表现为「物品乱换位置 / 全部挤到左上角」）。
        NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++) {
            list.set(i, be.ingredients.getItem(i));
        }
        ContainerHelper.saveAllItems(nbt, list, registries);
    }

    /** 读锅里的东西；读完顺手修复方块状态。 */
    static void read(HotPotBlockEntity be, CompoundTag nbt, HolderLookup.Provider registries) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int side = 0; side < be.bases.length; side++) {
            be.bases[side] = nbt.contains(BASE_KEYS[side]) ? nbt.getInt(BASE_KEYS[side]) : -1;
        }
        java.util.Arrays.fill(be.doneness, 0);
        int[] loaded = nbt.getIntArray("Doneness");
        System.arraycopy(loaded, 0, be.doneness, 0, Math.min(loaded.length, slots));
        java.util.Arrays.fill(be.cooked, false);
        byte[] cookedBytes = nbt.getByteArray("Cooked");
        for (int i = 0; i < slots; i++) {
            be.cooked[i] = i < cookedBytes.length && cookedBytes[i] != 0;
        }
        java.util.Arrays.fill(be.xpValue, 0F);
        int[] xp = nbt.getIntArray("SlotXp");
        for (int i = 0; i < Math.min(xp.length, slots); i++) {
            be.xpValue[i] = xp[i] / 100F;
        }
        be.pendingXp = nbt.getFloat("PendingXp");
        // 按 Slot 索引恢复，不再依赖 SimpleInventory.readNbtList 的顺序重排。
        loadIngredients(be, nbt, registries);
        // 兼容处理：旧版本或异常网络包可能曾把堆叠数量写入锅槽；加载时按物理槽语义截断，
        // 但保留每份物品自身的 NBT / Data Component，避免生熟状态被粗暴合并。
        for (int i = 0; i < slots; i++) {
            ItemStack stack = be.ingredients.getItem(i);
            be.tracked[i] = stack.copy();
            be.cookDur[i] = stack.isEmpty() ? 0 : HotPotCooking.cookTicks(be.getLevel(), stack);
        }
        be.markNeedsVisualRepair();
        be.repairVisualState();
    }

    /**
     * 从 NBT 恢复 81 格食材。新版存档每条都带 {@code Slot} 索引，按索引放回原槽；
     * 旧版（{@code SimpleInventory.toNbtList} 生成、无 {@code Slot} 字段）退化为按写入顺序
     * 依次放入 0..N，保持与旧行为一致。这样无论走哪条路径，物品都不会被重排到左上角。
     */
    private static void loadIngredients(HotPotBlockEntity be, CompoundTag nbt,
                                        HolderLookup.Provider registries) {
        int slots = HotPotBlockEntity.SLOTS;
        be.ingredients.clearContent();
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        int sequential = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.contains("Slot") ? (entry.getByte("Slot") & 0xFF) : sequential;
            ItemStack parsed = ItemStack.parseOptional(registries, entry);
            ItemStack stack = parsed == null ? ItemStack.EMPTY : parsed;
            if (slot >= 0 && slot < slots && !stack.isEmpty()) {
                if (stack.getCount() > 1) {
                    stack = stack.copyWithCount(1);
                }
                be.ingredients.setItem(slot, stack);
            }
            sequential++;
        }
    }
}