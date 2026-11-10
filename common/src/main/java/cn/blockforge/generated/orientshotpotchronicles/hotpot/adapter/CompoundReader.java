// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.1 的 {@link StoreReader} 实现：直接读 {@code CompoundTag}。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。缺键、坏档的兜底值和改造前逐条一致
 * （数值回 fallback、数组回空数组、物品回 {@code ItemStack.EMPTY}）。</p>
 */
public final class CompoundReader implements StoreReader {
    /** 空 reader 用的空标签：所有键都缺，兜底值统一。 */
    private static final CompoundTag EMPTY = new CompoundTag();
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    public CompoundReader(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag == null ? EMPTY : tag;
        this.registries = registries;
    }

    @Override
    public int getInt(String key, int fallback) {
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }

    @Override
    public float getFloat(String key, float fallback) {
        return tag.contains(key) ? tag.getFloat(key) : fallback;
    }

    @Override
    public double getDouble(String key, double fallback) {
        return tag.contains(key) ? tag.getDouble(key) : fallback;
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return tag.contains(key) ? tag.getBoolean(key) : fallback;
    }

    @Override
    public String getString(String key, String fallback) {
        return tag.contains(key) ? tag.getString(key) : fallback;
    }

    @Override
    public int[] getIntArray(String key) {
        return tag.getIntArray(key);
    }

    @Override
    public byte[] getByteArray(String key) {
        return tag.getByteArray(key);
    }

    @Override
    public boolean has(String key) {
        return tag.contains(key);
    }

    @Override
    public boolean hasCompound(String key) {
        return tag.contains(key, Tag.TAG_COMPOUND);
    }

    @Override
    public boolean hasNumber(String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC);
    }

    @Override
    public ItemStack getItem(String key) {
        if (!tag.contains(key)) {
            return ItemStack.EMPTY;
        }
        ItemStack parsed = ItemStack.parseOptional(registries, tag.getCompound(key));
        return parsed == null ? ItemStack.EMPTY : parsed;
    }

    /**
     * 与 {@code ContainerHelper.loadAllItems} 逐条一致的写法：只把 {@code Items} 里真正写了的槽放回去，
     * 其余槽保持调用方原来的内容；缺 {@code Items} 键时什么都不做（不是清空）。
     * mryt233 改（2026-11-11）：原先先建等长空表再回抄，会把缺键时的目标表清空，与原版不一致。
     */
    @Override
    public void loadItems(List<ItemStack> out) {
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < out.size()) {
                out.set(slot, ItemStack.parse(registries, entry).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public List<StoredItem> items() {
        List<StoredItem> out = new ArrayList<>();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            boolean hasSlot = entry.contains("Slot");
            int slot = hasSlot ? (entry.getByte("Slot") & 0xFF) : -1;
            ItemStack parsed = ItemStack.parseOptional(registries, entry);
            out.add(new StoredItem(hasSlot, slot, parsed == null ? ItemStack.EMPTY : parsed));
        }
        return out;
    }

    @Override
    public StoreReader child(String key) {
        return new CompoundReader(tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : EMPTY, registries);
    }

    @Override
    public List<StoreReader> children(String key) {
        List<StoreReader> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(new CompoundReader(list.getCompound(i), registries));
        }
        return out;
    }
}
