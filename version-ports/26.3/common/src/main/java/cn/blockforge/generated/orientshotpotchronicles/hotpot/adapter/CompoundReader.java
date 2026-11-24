// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * 26.2 的 {@link StoreReader} 实现：包装 {@code ValueInput}。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层；26.2 改为读写 {@code ValueInput}。</p>
 */
public final class CompoundReader implements StoreReader {
    private static final ValueInput EMPTY = TagValueInput.create(
        ProblemReporter.DISCARDING,
        HolderLookup.Provider.create(Stream.empty()),
        new CompoundTag());
    private final ValueInput input;

    public CompoundReader(ValueInput input) {
        this.input = input == null ? EMPTY : input;
    }

    @Override
    public int getInt(String key, int fallback) {
        return input.getIntOr(key, fallback);
    }

    @Override
    public float getFloat(String key, float fallback) {
        return input.getFloatOr(key, fallback);
    }

    @Override
    public double getDouble(String key, double fallback) {
        return input.getDoubleOr(key, fallback);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return input.getBooleanOr(key, fallback);
    }

    @Override
    public String getString(String key, String fallback) {
        return input.getStringOr(key, fallback);
    }

    @Override
    public int[] getIntArray(String key) {
        return input.getIntArray(key).orElseGet(() -> new int[0]);
    }

    @Override
    public List<String> getStringList(String key) {
        Optional<ValueInput.TypedInputList<String>> list = input.list(key, Codec.STRING);
        if (list.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : list.get()) {
            if (value != null) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public byte[] getByteArray(String key) {
        Optional<ByteBuffer> buf = input.read(key, Codec.BYTE_BUFFER);
        if (buf.isEmpty()) {
            return new byte[0];
        }
        ByteBuffer buffer = buf.get();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public boolean has(String key) {
        return input.read(key, Codec.PASSTHROUGH).isPresent();
    }

    @Override
    public boolean hasCompound(String key) {
        return input.child(key).isPresent();
    }

    @Override
    public boolean hasNumber(String key) {
        return input.getInt(key).isPresent();
    }

    @Override
    public ItemStack getItem(String key) {
        ItemStack parsed = input.read(key, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        return parsed == null ? ItemStack.EMPTY : parsed;
    }

    @Override
    public void loadItems(List<ItemStack> out) {
        for (ItemStackWithSlot entry : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (entry.isValidInContainer(out.size())) {
                out.set(entry.slot(), entry.stack());
            }
        }
    }

    @Override
    public List<StoredItem> items() {
        List<StoredItem> out = new ArrayList<>();
        for (ValueInput entry : input.childrenListOrEmpty("Items")) {
            Optional<Integer> slotOpt = entry.getInt("Slot");
            boolean hasSlot = slotOpt.isPresent();
            int slot = hasSlot ? (slotOpt.get() & 0xFF) : -1;
            ItemStack parsed = entry.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY);
            out.add(new StoredItem(hasSlot, slot, parsed == null ? ItemStack.EMPTY : parsed));
        }
        return out;
    }

    @Override
    public StoreReader child(String key) {
        return new CompoundReader(input.child(key).orElse(EMPTY));
    }

    @Override
    public List<StoreReader> children(String key) {
        List<StoreReader> out = new ArrayList<>();
        for (ValueInput child : input.childrenListOrEmpty(key)) {
            out.add(new CompoundReader(child));
        }
        return out;
    }
}
