// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * 26.1 的 {@link CompoundTag} 版 {@link StoreReader} 实现（用于物品组件与单测）。
 *
 * <p>mryt233 改（2026-11-13）：适配 26.1 {@code CompoundTag} 的 {@code Optional} 取值接口与
 * {@code ItemStack.CODEC}。</p>
 */
public final class CompoundReader implements StoreReader {
    private static final CompoundTag EMPTY = new CompoundTag();
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    public CompoundReader(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag == null ? EMPTY : tag;
        this.registries = registries != null ? registries
            : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Override
    public int getInt(String key, int fallback) {
        return tag.contains(key) ? tag.getIntOr(key, fallback) : fallback;
    }

    @Override
    public float getFloat(String key, float fallback) {
        return tag.contains(key) ? tag.getFloatOr(key, fallback) : fallback;
    }

    @Override
    public double getDouble(String key, double fallback) {
        return tag.contains(key) ? tag.getDoubleOr(key, fallback) : fallback;
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return tag.contains(key) ? tag.getBooleanOr(key, fallback) : fallback;
    }

    @Override
    public String getString(String key, String fallback) {
        return tag.contains(key) ? tag.getStringOr(key, fallback) : fallback;
    }

    @Override
    public int[] getIntArray(String key) {
        return tag.getIntArray(key).orElseGet(() -> new int[0]);
    }

    @Override
    public List<String> getStringList(String key) {
        ListTag list = tag.getListOrEmpty(key);
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            list.getString(i).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    @Override
    public byte[] getByteArray(String key) {
        return tag.getByteArray(key).orElseGet(() -> new byte[0]);
    }

    @Override
    public boolean has(String key) {
        return tag.contains(key);
    }

    @Override
    public boolean hasString(String key) {
        return tag.get(key) instanceof net.minecraft.nbt.StringTag;
    }

    @Override
    public boolean hasStringList(String key) {
        return tag.get(key) instanceof ListTag;
    }

    @Override
    public boolean hasIntArray(String key) {
        return tag.get(key) instanceof net.minecraft.nbt.IntArrayTag;
    }

    @Override
    public boolean hasCompound(String key) {
        return tag.get(key) instanceof CompoundTag;
    }

    @Override
    public boolean hasNumber(String key) {
        return tag.get(key) instanceof NumericTag;
    }

    @Override
    public ItemStack getItem(String key) {
        Tag itemTag = tag.get(key);
        if (itemTag == null) {
            return ItemStack.EMPTY;
        }
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(ops, itemTag).result().orElse(ItemStack.EMPTY);
    }

    @Override
    public void loadItems(List<ItemStack> out) {
        ListTag list = tag.getListOrEmpty("Items");
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            int slot = entry.getByteOr("Slot", (byte) 0) & 0xFF;
            if (slot >= 0 && slot < out.size()) {
                out.set(slot, ItemStack.CODEC.parse(ops, entry).result().orElse(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public List<StoredItem> items() {
        List<StoredItem> out = new ArrayList<>();
        ListTag list = tag.getListOrEmpty("Items");
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            boolean hasSlot = entry.contains("Slot");
            int slot = hasSlot ? (entry.getByteOr("Slot", (byte) 0) & 0xFF) : -1;
            ItemStack parsed = ItemStack.CODEC.parse(ops, entry).result().orElse(ItemStack.EMPTY);
            out.add(new StoredItem(hasSlot, slot, parsed));
        }
        return out;
    }

    @Override
    public StoreReader child(String key) {
        return new CompoundReader(tag.get(key) instanceof CompoundTag child ? child : EMPTY, registries);
    }

    @Override
    public List<StoreReader> children(String key) {
        List<StoreReader> out = new ArrayList<>();
        ListTag list = tag.getListOrEmpty(key);
        for (int i = 0; i < list.size(); i++) {
            list.getCompound(i).ifPresent(child -> out.add(new CompoundReader(child, registries)));
        }
        return out;
    }
}
