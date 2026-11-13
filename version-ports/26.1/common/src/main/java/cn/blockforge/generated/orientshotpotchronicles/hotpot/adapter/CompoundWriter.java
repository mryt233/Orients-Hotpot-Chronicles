// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * 26.1 的 {@link CompoundTag} 版 {@link StoreWriter} 实现（用于物品组件与单测）。
 *
 * <p>mryt233 改（2026-11-13）：适配 26.1 {@code ItemStack.CODEC} 与
 * {@code ContainerHelper.saveAllItems(ValueOutput, ...)}。</p>
 */
public final class CompoundWriter implements StoreWriter {
    private final CompoundTag tag;
    private final HolderLookup.Provider registries;

    public CompoundWriter(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries != null ? registries
            : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
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
        for (String value : values) {
            list.add(StringTag.valueOf(value));
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
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(encoded -> tag.put(key, encoded));
        }
    }

    @Override
    public void putItems(List<ItemStack> items) {
        NonNullList<ItemStack> list = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            list.set(i, items.get(i));
        }
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ContainerHelper.saveAllItems(out, list, true);
        Tag itemsTag = out.buildResult().get("Items");
        if (itemsTag != null) {
            tag.put("Items", itemsTag);
        }
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
