// 作者：mryt233
package cn.blockforge.sample.hotpotsubmod;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableObjects;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** 可 Shift+右键摆上火锅桌 / 地面格的自定义物件，空手右键拿回、针对单件挖取。 */
public final class CoasterItem extends Item implements TableItem {
    public CoasterItem() {
        super(new Item.Properties());
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return HotpotSubmod.coasterBlock();
    }

    @Override
    public boolean autoPlace(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return Use.item(UseOutcome.PASS, context.getLevel().isClientSide);
        }
        return Use.item(TableObjects.placeInHolderOrOn(context, this), context.getLevel().isClientSide);
    }
}
