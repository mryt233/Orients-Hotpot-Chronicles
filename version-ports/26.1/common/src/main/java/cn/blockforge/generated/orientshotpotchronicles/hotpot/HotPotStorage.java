// mryt233 改：26.1 存档入口，保持所有键名和槽位格式。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;

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
    /**
     * mryt233 改（2026-11-05）：存档里每边红汤辣度档位（r136a）的字段名，第 0 边沿用老式短名 Spicy。
     * 旧存档没有这几个键时一律读成 0 档（微辣），不报错。
     */
    static final String[] SPICY_KEYS = {"Spicy", "SpicyB", "SpicyC", "SpicyD"};

    private HotPotStorage() {
    }

    /** 写锅里的东西。 */
    static void write(HotPotBlockEntity be, CompoundTag nbt, HolderLookup.Provider registries) {
        var output = StorageSupport.output(registries);
        write(be, output);
        nbt.merge(output.buildResult());
    }

    static void write(HotPotBlockEntity be, ValueOutput nbt) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int side = 0; side < be.bases.length; side++) {
            nbt.putInt(BASE_KEYS[side], be.bases[side]);
            // mryt233 改（2026-11-05）：每边的红汤辣度档位（r136a），区块存档和整锅打包（pot_contents）都要带着
            nbt.putInt(SPICY_KEYS[side], be.spicyLevels[side]);
        }
        // mryt233 改（2026-11-07）：IntArrayTag 是直接存数组引用（不 clone），
        // 不拷贝就会让 NBT 别名着方块实体的活数组：打包成锅物品之后再清空锅，
        // 熟度 / 过头计时会被一起抹掉。这里的出口虽然都还会再 copy 一次，但不该靠那个巧合。
        nbt.putIntArray("Doneness", be.doneness.clone());
        byte[] cookedBytes = new byte[slots];
        for (int i = 0; i < slots; i++) {
            cookedBytes[i] = (byte) (be.cooked[i] ? 1 : 0);
        }
        nbt.store("Cooked", Codec.BYTE_BUFFER, ByteBuffer.wrap(cookedBytes));
        // mryt233 改（2026-11-04）：熟了以后在锅里继续煮的刻数（豆腐煮过头碎块用），重启不丢。
        nbt.putIntArray("Overcook", be.overcook.clone());   // mryt233 改（2026-11-07）：同上，防御性拷贝
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
        ContainerHelper.saveAllItems(nbt, list);
    }

    /** 读锅里的东西；读完顺手修复方块状态。 */
    static void read(HotPotBlockEntity be, CompoundTag nbt, HolderLookup.Provider registries) {
        read(be, TagValueInput.create(ProblemReporter.DISCARDING, registries, nbt));
    }

    static void read(HotPotBlockEntity be, ValueInput nbt) {
        int slots = HotPotBlockEntity.SLOTS;
        for (int side = 0; side < be.bases.length; side++) {
            be.bases[side] = nbt.getIntOr(BASE_KEYS[side], -1);
            // mryt233 改（2026-11-05）：旧存档没有辣度键 = 微辣（0 档）；超出 0..3 的值夹回来
            int spicy = nbt.getIntOr(SPICY_KEYS[side], 0);
            be.spicyLevels[side] = Math.max(0, Math.min(3, spicy));
        }
        java.util.Arrays.fill(be.doneness, 0);
        // mryt233 改（2026-11-07）：getIntArray 返回的是标签内部那个数组，这里用 System.arraycopy
        // 逐份拷进方块实体自己的数组（写侧对应地 clone 再放进标签），两边都不互相持有引用。
        int[] loaded = nbt.getIntArray("Doneness").orElseGet(() -> new int[0]);
        System.arraycopy(loaded, 0, be.doneness, 0, Math.min(loaded.length, slots));
        java.util.Arrays.fill(be.cooked, false);
        ByteBuffer buffer = nbt.read("Cooked", Codec.BYTE_BUFFER).orElseGet(() -> ByteBuffer.allocate(0));
        byte[] cookedBytes = new byte[buffer.remaining()];
        buffer.get(cookedBytes);
        for (int i = 0; i < slots; i++) {
            be.cooked[i] = i < cookedBytes.length && cookedBytes[i] != 0;
        }
        java.util.Arrays.fill(be.overcook, 0);
        int[] overdue = nbt.getIntArray("Overcook").orElseGet(() -> new int[0]);   // mryt233 改（2026-11-07）：同上，靠 System.arraycopy 拷进自有数组
        System.arraycopy(overdue, 0, be.overcook, 0, Math.min(overdue.length, slots));
        java.util.Arrays.fill(be.xpValue, 0F);
        int[] xp = nbt.getIntArray("SlotXp").orElseGet(() -> new int[0]);   // mryt233 改（2026-11-07）：同上，逐项换算写进自有数组
        for (int i = 0; i < Math.min(xp.length, slots); i++) {
            be.xpValue[i] = xp[i] / 100F;
        }
        be.pendingXp = nbt.getFloatOr("PendingXp", 0F);
        // 按 Slot 索引恢复，不再依赖 SimpleInventory.readNbtList 的顺序重排。
        loadIngredients(be, nbt);
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
    private static void loadIngredients(HotPotBlockEntity be, ValueInput nbt) {
        int slots = HotPotBlockEntity.SLOTS;
        be.ingredients.clearContent();
        var list = nbt.childrenListOrEmpty("Items");
        int sequential = 0;
        for (ValueInput entry : list) {
            int slot = entry.getInt("Slot").isPresent() ? (entry.getByteOr("Slot", (byte) 0) & 0xFF) : sequential;
            ItemStack parsed = entry.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY);
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
