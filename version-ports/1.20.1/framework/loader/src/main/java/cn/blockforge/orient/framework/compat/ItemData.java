package cn.blockforge.orient.framework.compat;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** 原物品 NBT 的有类型读写；NBT 随物品栈使用原版同步。作者：mryt233 */
public final class ItemData {
    private ItemData() { }

    public static <T> T get(ItemStack stack, ItemDataKey<T> key) {
        if (stack.getTag() == null || !stack.getTag().contains(key.name())) return null;
        return key.codec().parse(NbtOps.INSTANCE, stack.getTag().get(key.name())).result().orElse(null);
    }

    public static <T> T getOrDefault(ItemStack stack, ItemDataKey<T> key, T fallback) {
        T value = get(stack, key);
        return value == null ? fallback : value;
    }

    public static boolean has(ItemStack stack, ItemDataKey<?> key) {
        return stack.getTag() != null && stack.getTag().contains(key.name());
    }

    public static <T> void set(ItemStack stack, ItemDataKey<T> key, T value) {
        if (value == null) { remove(stack, key); return; }
        if (value instanceof ItemContainerContents contents && contents.nonEmptyStream().findAny().isEmpty()) {
            remove(stack, key);
            return;
        }
        Tag encoded = key.codec().encodeStart(NbtOps.INSTANCE, value).result()
            .orElseThrow(() -> new IllegalArgumentException("Cannot encode item data " + key.name()));
        stack.getOrCreateTag().put(key.name(), encoded);
    }

    public static <T> T remove(ItemStack stack, ItemDataKey<T> key) {
        T previous = get(stack, key);
        stack.removeTagKey(key.name());
        return previous;
    }
}
