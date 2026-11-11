// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client.guide;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** 作者：mryt233；2026-11-11：指南图标与物品组件转换。 */
public final class GuideItems {
    private GuideItems() { }
    /** 物品 ID 变图标；「锅ID#汤底」= 装着那种汤的锅（2026-10-26 用户：火锅分类的书签用番茄汤底的锅）；
     *  「…#lit」= 自定义模型数据 1（灶台物品模型据此换成烧着的 item/brick_stove_lit，用户要灶台书签是燃烧状态）。 */
    public static ItemStack of(String id) {
        String[] parts = id.split("#", 2);
        ResourceLocation ident = ResourceLocation.tryParse(parts[0]);
        Item item = ident == null ? Items.BARRIER : BuiltInRegistries.ITEM.getOptional(ident).orElse(Items.BARRIER);
        ItemStack stack = new ItemStack(item);
        if (parts.length > 1 && parts[1].equals("lit")) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(1));
        } else if (parts.length > 1) {
            try {
                CompoundTag nbt = new CompoundTag();
                nbt.putInt("Base", SoupBase.valueOf(
                    parts[1].toUpperCase(Locale.ROOT)).ordinal());
                stack.set(HotPotContent.POT_CONTENTS,
                    CustomData.of(nbt));
            } catch (IllegalArgumentException e) {
                HotPot.LOGGER.warn("guide book: unknown soup in {}", id);
            }
        }
        return stack;
    }

}
