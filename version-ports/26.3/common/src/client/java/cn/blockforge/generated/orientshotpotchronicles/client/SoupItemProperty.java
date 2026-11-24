// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/** 保留原锅物品的汤底谓词及组件键名。 */
public record SoupItemProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<SoupItemProperty> CODEC = MapCodec.unit(SoupItemProperty::new);
    @Override public MapCodec<SoupItemProperty> type() { return CODEC; }
    @Override public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        var data = stack.get(HotPotContent.POT_CONTENTS);
        SoupDef soup = data == null ? null : Soups.get(SoupIds.fromPotTag(data.copyTag(), 0));
        return soup == null ? 0F : (Soups.indexOf(soup) + 1) / 10F;
    }
}
