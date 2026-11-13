// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 26.1 的 {@link ValueOutput} 版 {@link StoreWriter} 实现（用于方块实体 {@code saveAdditional}）。
 *
 * <p>mryt233 改（2026-11-13）：直接包装原版 {@link ValueOutput}。</p>
 */
public final class ValueOutputWriter implements StoreWriter {
    private final ValueOutput out;

    public ValueOutputWriter(ValueOutput out) {
        this.out = out;
    }

    @Override
    public void putInt(String key, int value) {
        out.putInt(key, value);
    }

    @Override
    public void putFloat(String key, float value) {
        out.putFloat(key, value);
    }

    @Override
    public void putDouble(String key, double value) {
        out.putDouble(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        out.putBoolean(key, value);
    }

    @Override
    public void putString(String key, String value) {
        out.putString(key, value);
    }

    @Override
    public void putIntArray(String key, int[] value) {
        out.putIntArray(key, value);
    }

    @Override
    public void putStringList(String key, List<String> values) {
        out.store(key, Codec.STRING.listOf(), values);
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        out.store(key, Codec.BYTE_BUFFER, ByteBuffer.wrap(value));
    }

    @Override
    public void putItem(String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            out.store(key, ItemStack.CODEC, stack);
        }
    }

    @Override
    public void putItems(List<ItemStack> items) {
        NonNullList<ItemStack> list = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            list.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(out, list, true);
    }

    @Override
    public StoreWriter child(String key) {
        return new ValueOutputWriter(out.child(key));
    }

    @Override
    public StoreChildList childList(String key) {
        ValueOutput.ValueOutputList list = out.childrenList(key);
        return () -> new ValueOutputWriter(list.addChild());
    }
}
