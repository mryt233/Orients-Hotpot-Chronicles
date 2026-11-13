// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;

/**
 * 26.1 的 {@link ValueInput} 版 {@link StoreReader} 实现（用于方块实体 {@code loadAdditional}）。
 *
 * <p>mryt233 改（2026-11-13）：直接包装原版 {@link ValueInput}。</p>
 */
public final class ValueInputReader implements StoreReader {
    private final ValueInput in;

    public ValueInputReader(ValueInput in) {
        this.in = in;
    }

    @Override
    public int getInt(String key, int fallback) {
        return in.getIntOr(key, fallback);
    }

    @Override
    public float getFloat(String key, float fallback) {
        return in.getFloatOr(key, fallback);
    }

    @Override
    public double getDouble(String key, double fallback) {
        return in.getDoubleOr(key, fallback);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return in.getBooleanOr(key, fallback);
    }

    @Override
    public String getString(String key, String fallback) {
        return in.getStringOr(key, fallback);
    }

    @Override
    public int[] getIntArray(String key) {
        return in.getIntArray(key).orElseGet(() -> new int[0]);
    }

    @Override
    public List<String> getStringList(String key) {
        return in.read(key, Codec.STRING.listOf()).orElse(List.of());
    }

    @Override
    public byte[] getByteArray(String key) {
        ByteBuffer buf = in.read(key, Codec.BYTE_BUFFER).orElse(null);
        if (buf == null) {
            return new byte[0];
        }
        byte[] out = new byte[buf.remaining()];
        buf.get(out);
        return out;
    }

    @Override
    public boolean has(String key) {
        return in.read(key, Codec.PASSTHROUGH).isPresent();
    }

    @Override
    public boolean hasString(String key) {
        return in.getString(key).isPresent();
    }

    @Override
    public boolean hasStringList(String key) {
        return in.read(key, Codec.STRING.listOf()).isPresent();
    }

    @Override
    public boolean hasIntArray(String key) {
        return in.getIntArray(key).isPresent();
    }

    @Override
    public boolean hasCompound(String key) {
        return in.child(key).isPresent();
    }

    @Override
    public boolean hasNumber(String key) {
        return in.getInt(key).isPresent();
    }

    @Override
    public ItemStack getItem(String key) {
        return in.read(key, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public void loadItems(List<ItemStack> out) {
        for (ItemStackWithSlot item : in.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (item.isValidInContainer(out.size())) {
                out.set(item.slot(), item.stack());
            }
        }
    }

    @Override
    public List<StoredItem> items() {
        List<StoredItem> out = new ArrayList<>();
        for (ValueInput entry : in.childrenListOrEmpty("Items")) {
            boolean hasSlot = entry.getInt("Slot").isPresent();
            int slot = hasSlot ? (entry.getIntOr("Slot", 0) & 0xFF) : -1;
            ItemStack parsed = entry.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY);
            out.add(new StoredItem(hasSlot, slot, parsed));
        }
        return out;
    }

    @Override
    public StoreReader child(String key) {
        return new ValueInputReader(in.childOrEmpty(key));
    }

    @Override
    public List<StoreReader> children(String key) {
        List<StoreReader> out = new ArrayList<>();
        for (ValueInput child : in.childrenListOrEmpty(key)) {
            out.add(new ValueInputReader(child));
        }
        return out;
    }
}
