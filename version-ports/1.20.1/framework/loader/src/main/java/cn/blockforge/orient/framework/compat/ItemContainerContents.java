package cn.blockforge.orient.framework.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** 沿用旧版 container 列表中的 slot:int / item:compound 格式。作者：mryt233 */
public final class ItemContainerContents {
    private record Entry(int slot, ItemStack item) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("slot").forGetter(Entry::slot), ItemStack.CODEC.fieldOf("item").forGetter(Entry::item)
        ).apply(i, Entry::new));
    }
    public static final ItemContainerContents EMPTY = new ItemContainerContents(List.of());
    public static final Codec<ItemContainerContents> CODEC = Entry.CODEC.listOf().xmap(entries -> {
        int size = entries.stream().mapToInt(Entry::slot).max().orElse(-1) + 1;
        if (size > 256 || entries.stream().anyMatch(e -> e.slot() < 0))
            throw new IllegalArgumentException("Invalid container slot");
        List<ItemStack> items = new ArrayList<>(java.util.Collections.nCopies(size, ItemStack.EMPTY));
        for (Entry e : entries) items.set(e.slot(), e.item());
        return new ItemContainerContents(items);
    }, contents -> {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < contents.items.size(); i++) {
            if (!contents.items.get(i).isEmpty()) entries.add(new Entry(i, contents.items.get(i)));
        }
        return entries;
    });
    private final List<ItemStack> items;
    private ItemContainerContents(List<ItemStack> items) {
        this.items = items.stream().map(ItemStack::copy).toList();
    }
    public static ItemContainerContents fromItems(List<ItemStack> items) { return new ItemContainerContents(items); }
    public Stream<ItemStack> stream() { return items.stream().map(ItemStack::copy); }
    public Stream<ItemStack> nonEmptyStream() { return stream().filter(s -> !s.isEmpty()); }
    public void copyInto(List<ItemStack> destination) {
        for (int i = 0; i < destination.size(); i++) destination.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
    }
    public ItemStack copyOne() { return nonEmptyStream().findFirst().orElse(ItemStack.EMPTY); }
    public Iterable<ItemStack> nonEmptyItems() { return nonEmptyStream().toList(); }
}
