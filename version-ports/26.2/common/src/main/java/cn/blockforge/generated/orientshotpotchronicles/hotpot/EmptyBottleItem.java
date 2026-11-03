// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

/**
 * 喝完的空瓶（r99 橙子饮料，r101 起每种饮料一个）：也能像盘子一样摆在桌上。
 * 作者：mryt233 */
public class EmptyBottleItem extends Item {
    private final DrinkKind kind;

    public EmptyBottleItem(DrinkKind kind, Item.Properties properties) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public DrinkKind kind() {
        return kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return PlateItem.placeOn(context, HotPotContent.DRINK_BLOCK, true);
    }
}
