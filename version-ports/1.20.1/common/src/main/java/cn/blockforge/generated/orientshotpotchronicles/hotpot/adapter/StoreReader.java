// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * 存档读侧的「无版本读写原语」，与 {@link StoreWriter} 对应。
 */
public interface StoreReader {
    int getInt(String key, int fallback);

    float getFloat(String key, float fallback);

    double getDouble(String key, double fallback);

    boolean getBoolean(String key, boolean fallback);

    String getString(String key, String fallback);

    /** 缺键回空数组（和 {@code CompoundTag.getIntArray} 一样）。 */
    int[] getIntArray(String key);

    /** mryt233：按原顺序读取字符串列表；坏类型返回空列表。 */
    List<String> getStringList(String key);

    /** 缺键回空数组（和 {@code CompoundTag.getByteArray} 一样）。 */
    byte[] getByteArray(String key);

    /** 这个键在不在（任意类型，对应 {@code CompoundTag.contains(key)}）。 */
    boolean has(String key);

    /** 这个键是不是子节点（对应 {@code contains(key, Tag.TAG_COMPOUND)}）。 */
    boolean hasCompound(String key);

    /** 这个键是不是数字（对应 {@code contains(key, Tag.TAG_ANY_NUMERIC)}）。 */
    boolean hasNumber(String key);

    /** 单个物品；缺键 / 坏档回 {@code ItemStack.EMPTY}。 */
    ItemStack getItem(String key);

    /** 当前节点的物品列表（{@code Items}），按原版 {@code ContainerHelper.loadAllItems} 的规则放回容器。 */
    void loadItems(List<ItemStack> out);

    /** 当前节点 {@code Items} 的原始条目。 */
    List<StoredItem> items();

    /** 子节点；缺键回空 reader。 */
    StoreReader child(String key);

    /** 子节点列表；缺键回空列表。 */
    List<StoreReader> children(String key);
}
