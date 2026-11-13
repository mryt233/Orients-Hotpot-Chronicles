// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 盘子物品。不是直接放一个方块，而是在点的那个面上方摆进「盘子格」（{@link PlateHolderBlock}），
 * 一格里能摆好几个。拿着它右键锅是把菜倒进锅（锅那边处理）。
 */
public class PlateItem extends BlockItem implements TableItem {
    private final PlateBlock plate;

    public PlateItem(PlateBlock plate, Item.Properties settings) {
        super(plate, settings);
        this.plate = plate;
    }

    public PlateBlock plate() {
        return plate;
    }

    @Override
    public PlateBlock tableBlock(ItemStack stack) {
        return plate;
    }

    @Override
    public boolean keepsSelf(ItemStack stack) {
        return false;
    }

    /**
     * 物品上有菜（盘子装了菜 / 筷子夹着菜）时用自定义模型数据 1，切到 builtin/entity 模型，
     * 由客户端的 HeldFoodItemRenderer 先画原来的样子再把菜画上去；没菜就去掉，用原来的模型（r91）。
     */
    public static void syncFoodModel(ItemStack stack, boolean hasFood) {
        if (hasFood) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(1.0F), List.of(), List.of(), List.of()));
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    /** 盘子物品按 container 组件里有没有菜同步模型。 */
    public static void syncPlateModel(ItemStack stack) {
        syncFoodModel(stack, stack.getOrDefault(DataComponents.CONTAINER,
            ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent());
    }

    /** 能不能装进盘子：委托给 {@link TableObjects#holdsFood}。 */
    public static boolean holdsFood(Level world, ItemStack stack) {
        return TableObjects.holdsFood(world, stack);
    }

    /** 保留旧入口签名，委托给 {@link TableObjects#placedFor}。 */
    @Nullable
    public static PlateBlock placedFor(ItemStack stack) {
        return TableObjects.placedFor(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return Use.item(TableObjects.placeOn(context, this), context.getLevel().isClientSide());
    }

    /** 保留旧入口签名，委托给 {@link TableObjects#placeOn}。 */
    public static InteractionResult placeOn(UseOnContext context, PlateBlock plate, boolean keepSelf) {
        return Use.item(TableObjects.placeOn(context, plate, keepSelf), context.getLevel().isClientSide());
    }
}
