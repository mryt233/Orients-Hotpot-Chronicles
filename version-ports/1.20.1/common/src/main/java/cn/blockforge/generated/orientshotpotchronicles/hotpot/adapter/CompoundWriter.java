// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 的 {@link StoreWriter} 实现：直接写 {@code CompoundTag}。
 */
public final class CompoundWriter implements StoreWriter {
    private final CompoundTag tag;

    public CompoundWriter(CompoundTag tag) {
        this.tag = tag;
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
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    list.add(StringTag.valueOf(value));
                }
            }
        }
        tag.put(key, list);
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        tag.putByteArray(key, value);
    }

    @Override
    public void putItem(String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            tag.put(key, stack.save(new CompoundTag()));
        }
    }

    @Override
    public void putItems(List<ItemStack> items) {
        NonNullList<ItemStack> list = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            list.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(tag, list);
    }

    @Override
    public StoreWriter child(String key) {
        CompoundTag child = new CompoundTag();
        tag.put(key, child);
        return new CompoundWriter(child);
    }

    @Override
    public StoreChildList childList(String key) {
        ListTag list = new ListTag();
        tag.put(key, list);
        return () -> {
            CompoundTag entry = new CompoundTag();
            list.add(entry);
            return new CompoundWriter(entry);
        };
    }
}
