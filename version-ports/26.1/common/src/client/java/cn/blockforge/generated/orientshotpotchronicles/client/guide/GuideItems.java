// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Locale;

/** 作者：mryt233；2026-11-11：指南图标与物品组件转换（26.1）。 */
public final class GuideItems {
    private GuideItems() { }

    /** 物品 ID 变图标；「锅ID#汤底」= 装着那种汤的锅；「…#lit」= 自定义模型数据 1。 */
    public static ItemStack of(String id) {
        String[] parts = id.split("#", 2);
        Identifier ident = Identifier.tryParse(parts[0]);
        Item item = ident == null ? Items.BARRIER : BuiltInRegistries.ITEM.getOptional(ident).orElse(Items.BARRIER);
        ItemStack stack = new ItemStack(item);
        if (parts.length > 1 && parts[1].equals("lit")) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(1F), List.of(), List.of(), List.of()));
        } else if (parts.length > 1) {
            try {
                CompoundTag nbt = new CompoundTag();
                // mryt233 改（2026-11-12，4D）：指南锅按登记 ID 写新格式。
                SoupDef soup = Soups.get(parts[1].toLowerCase(Locale.ROOT));
                if (soup == null) {
                    throw new IllegalArgumentException(parts[1]);
                }
                nbt.putString("Soup", soup.id().toString());
                stack.set(HotPotContent.POT_CONTENTS, CustomData.of(nbt));
            } catch (IllegalArgumentException e) {
                HotPot.LOGGER.warn("guide book: unknown soup in {}", id);
            }
        }
        return stack;
    }
}
