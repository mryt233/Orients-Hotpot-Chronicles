// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * 存档读侧的「无版本读写原语」，与 {@link StoreWriter} 对应。
 *
 * <p>mryt233 改（2026-11-11）：第三批 3G 版本适配层。缺键、坏档的兜底值和改造前一致：
 * 数值回 {@code fallback}，数组回空数组，物品回 {@code ItemStack.EMPTY}，子节点回空 reader，
 * 子节点列表回空列表。</p>
 */
public interface StoreReader {
    int getInt(String key, int fallback);

    float getFloat(String key, float fallback);

    double getDouble(String key, double fallback);

    boolean getBoolean(String key, boolean fallback);

    String getString(String key, String fallback);

    /** 缺键回空数组（和 {@code CompoundTag.getIntArray} 一样）。 */
    int[] getIntArray(String key);

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

    /**
     * 当前节点 {@code Items} 的原始条目：带着「有没有 {@code Slot}」交出来，
     * 给「旧存档没有 Slot、按写入顺序放」的兜底用。
     */
    List<StoredItem> items();

    /** 子节点；缺键回空 reader。 */
    StoreReader child(String key);

    /** 子节点列表；缺键回空列表。 */
    List<StoreReader> children(String key);
}
