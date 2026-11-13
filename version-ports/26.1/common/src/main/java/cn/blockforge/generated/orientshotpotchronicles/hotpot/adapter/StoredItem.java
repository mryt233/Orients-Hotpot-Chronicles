// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter;

import net.minecraft.world.item.ItemStack;

/**
 * {@code Items} 里的一条：{@code Slot} 字段有没有、值是几、物品是什么。
 * 旧存档（没有 {@code Slot}）靠 {@code hasSlot} 区分。
 */
public record StoredItem(boolean hasSlot, int slot, ItemStack stack) {
}
