// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * 存档写侧的「无版本读写原语」。业务方块实体只描述「写什么」，不直接摸底层存储对象。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。</p>
 */
public interface StoreWriter {
    void putInt(String key, int value);

    void putFloat(String key, float value);

    void putDouble(String key, double value);

    void putBoolean(String key, boolean value);

    void putString(String key, String value);

    void putIntArray(String key, int[] value);

    /** mryt233：按原顺序写字符串列表。 */
    void putStringList(String key, List<String> values);

    void putByteArray(String key, byte[] value);

    /** 单个物品；空栈跳过。 */
    void putItem(String key, ItemStack stack);

    /** 当前节点的物品列表（键名固定 {@code Items}、每条带 {@code Slot}、空表也写）。 */
    void putItems(List<ItemStack> items);

    /** 开一个子节点（例如灶台的 {@code Fuel}）。 */
    StoreWriter child(String key);

    /** 开一个子节点列表（例如桌面的 {@code Plates}）。 */
    StoreChildList childList(String key);
}
