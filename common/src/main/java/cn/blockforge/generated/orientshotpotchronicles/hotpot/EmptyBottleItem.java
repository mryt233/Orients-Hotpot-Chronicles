package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

/**
 * 喝完的空瓶（r99 橙子饮料，r101 起每种饮料一个）：也能像盘子一样摆在桌上。
 * 作者：mryt233 */
public class EmptyBottleItem extends Item implements TableItem {
    private final DrinkKind kind;

    public EmptyBottleItem(DrinkKind kind) {
        super(new Item.Properties().stacksTo(16));
        this.kind = kind;
    }

    public DrinkKind kind() {
        return kind;
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return HotPotContent.DRINK_BLOCK;
    }

    @Override
    public void playPlaceSound(Level world, BlockPos pos, ItemStack stack) {
        DrinkBlock.playPlaceSound(world, pos, stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide);
    }
}
