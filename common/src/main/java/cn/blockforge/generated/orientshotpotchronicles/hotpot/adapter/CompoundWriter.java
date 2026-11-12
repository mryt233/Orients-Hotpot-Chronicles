// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.1 的 {@link StoreWriter} 实现：直接写 {@code CompoundTag}。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。写出来的键名、标签类型、条目顺序和改造前逐字相同：
 * 物品列表仍由 {@code ContainerHelper.saveAllItems} 负责 {@code Items} + 每条 {@code Slot}，
 * 而且和改造前一样<b>空表也写这个键</b>（原版三参数重载的 flag 为 true）。</p>
 */
public final class CompoundWriter implements StoreWriter {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    public CompoundWriter(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
    }

    @Override
    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    @Override
    public void putFloat(String key, float value) {
        tag.putFloat(key, value);
    }

    @Override
    public void putDouble(String key, double value) {
        tag.putDouble(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }

    @Override
    public void putString(String key, String value) {
        tag.putString(key, value);
    }

    @Override
    public void putIntArray(String key, int[] value) {
        tag.putIntArray(key, value);
    }

    @Override
    public void putStringList(String key, List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        tag.put(key, list);
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        tag.putByteArray(key, value);
    }

    @Override
    public void putItem(String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            tag.put(key, stack.save(registries));
        }
    }

    @Override
    public void putItems(List<ItemStack> items) {
        NonNullList<ItemStack> list = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            list.set(i, items.get(i));
        }
        // 借用原版的三参数重载：键名固定 "Items"、每条带 Slot、空表也照写，与改造前一个字节都不差。
        ContainerHelper.saveAllItems(tag, list, registries);
    }

    @Override
    public StoreWriter child(String key) {
        CompoundTag child = new CompoundTag();
        tag.put(key, child);
        return new CompoundWriter(child, registries);
    }

    @Override
    public StoreChildList childList(String key) {
        ListTag list = new ListTag();
        tag.put(key, list);
        return () -> {
            CompoundTag entry = new CompoundTag();
            list.add(entry);
            return new CompoundWriter(entry, registries);
        };
    }
}
