package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

/**
 * 喝完的空瓶（r99 橙子饮料，r101 起每种饮料一个）：也能像盘子一样摆在桌上。
 *
 */
public class EmptyBottleItem extends Item {
    private final DrinkKind kind;

    public EmptyBottleItem(DrinkKind kind) {
        super(new Item.Settings().maxCount(16));
        this.kind = kind;
    }

    public DrinkKind kind() {
        return kind;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return PlateItem.placeOn(context, HotPotContent.DRINK_BLOCK, true);
    }
}
