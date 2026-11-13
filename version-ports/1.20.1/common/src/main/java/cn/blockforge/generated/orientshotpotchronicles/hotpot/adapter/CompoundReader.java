// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 的 {@link StoreReader} 实现：直接读 {@code CompoundTag}。
 *
 * <p>缺键、坏档的兜底值保持一致：数值回 fallback、数组与字符串列表回空、物品回 {@code ItemStack.EMPTY}。</p>
 */
public final class CompoundReader implements StoreReader {
    private static final CompoundTag EMPTY = new CompoundTag();
    private final CompoundTag tag;

    public CompoundReader(CompoundTag tag) {
        this.tag = tag == null ? EMPTY : tag;
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
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : new int[0];
    }

    @Override
    public List<String> getStringList(String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return List.copyOf(result);
    }

    @Override
    public byte[] getByteArray(String key) {
        return tag.contains(key, Tag.TAG_BYTE_ARRAY) ? tag.getByteArray(key) : new byte[0];
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
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag.getCompound(key));
    }

    @Override
    public void loadItems(List<ItemStack> out) {
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < out.size()) {
                out.set(slot, ItemStack.of(entry));
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
            out.add(new StoredItem(hasSlot, slot, ItemStack.of(entry)));
        }
        return out;
    }

    @Override
    public StoreReader child(String key) {
        return new CompoundReader(tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : EMPTY);
    }

    @Override
    public List<StoreReader> children(String key) {
        List<StoreReader> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(new CompoundReader(list.getCompound(i)));
        }
        return out;
    }
}
