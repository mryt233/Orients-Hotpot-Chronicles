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
 * 26.2 的 {@link StoreWriter} 实现：包装 {@code ValueOutput}。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层；26.2 改为写入 {@code ValueOutput}。</p>
 */
public final class CompoundWriter implements StoreWriter {
    private final ValueOutput output;

    public CompoundWriter(ValueOutput output) {
        this.output = output;
    }

    @Override
    public void putInt(String key, int value) {
        output.putInt(key, value);
    }

    @Override
    public void putFloat(String key, float value) {
        output.putFloat(key, value);
    }

    @Override
    public void putDouble(String key, double value) {
        output.putDouble(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        output.putBoolean(key, value);
    }

    @Override
    public void putString(String key, String value) {
        output.putString(key, value);
    }

    @Override
    public void putIntArray(String key, int[] value) {
        output.putIntArray(key, value);
    }

    @Override
    public void putStringList(String key, List<String> values) {
        ValueOutput.TypedOutputList<String> list = output.list(key, Codec.STRING);
        for (String value : values) {
            if (value != null) {
                list.add(value);
            }
        }
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        output.store(key, Codec.BYTE_BUFFER, ByteBuffer.wrap(value));
    }

    @Override
    public void putItem(String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            output.store(key, ItemStack.CODEC, stack);
        }
    }

    @Override
    public void putItems(List<ItemStack> items) {
        NonNullList<ItemStack> list = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            list.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(output, list);
    }

    @Override
    public StoreWriter child(String key) {
        return new CompoundWriter(output.child(key));
    }

    @Override
    public StoreChildList childList(String key) {
        ValueOutput.ValueOutputList list = output.childrenList(key);
        return () -> new CompoundWriter(list.addChild());
    }
}
