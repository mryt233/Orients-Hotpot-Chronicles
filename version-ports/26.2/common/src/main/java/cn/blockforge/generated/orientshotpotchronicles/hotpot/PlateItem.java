// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 盘子物品。不是直接放一个方块，而是在点的那个面上方摆进「盘子格」（{@link PlateHolderBlock}），
 * 一格里能摆好几个。拿着它右键锅是把菜倒进锅（锅那边处理）。
 */
public class PlateItem extends BlockItem {
    private final PlateBlock plate;

    public PlateItem(PlateBlock plate, Item.Properties settings) {
        super(plate, settings);
        this.plate = plate;
    }

    public PlateBlock plate() {
        return plate;
    }

    /**
     * 物品上有菜（盘子装了菜 / 筷子夹着菜）时用自定义模型数据 1，切到 builtin/entity 模型，
     * 由客户端的 HeldFoodItemRenderer 先画原来的样子再把菜画上去；没菜就去掉，用原来的模型（r91）。
     */
    public static void syncFoodModel(ItemStack stack, boolean hasFood) {
        if (hasFood) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(java.util.List.of((float) (1)), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    /** 盘子物品按 container 组件里有没有菜同步模型。 */
    public static void syncPlateModel(ItemStack stack) {
        syncFoodModel(stack, stack.getOrDefault(DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent());
    }

    /** 能不能装进盘子：能下锅的生食材，或者本身能吃的东西（熟食也能装）；汤碗、橙子饮料这种整件摆的不算。 */
    public static boolean holdsFood(Level world, ItemStack stack) {
        return !stack.isEmpty() && placedFor(stack) == null && (stack.has(DataComponents.FOOD) || HotPotCooking.acceptable(world, stack));
    }

    /**
     * 手里这件东西摆到桌上是什么（r99 起不只盘子）：盘子 → 盘子；原版的碗 → 碗（装的菜跟着）；
     * 汤碗 → 碗（整件记着）；橙子饮料 / 空瓶 → 瓶子（整件记着）。不能摆返回 null。
     * <p>
     * 竹筷<b>有意不在这里返回</b>（r136b，2026-11-05 用户）：拿筷子点普通碗盘时要继续走「夹菜 / 放菜」那条路，
     * 若这里返回筷架方块，点盘子就会被当成「再摆一个盘子」。竹筷上桌由
     * {@link ChopsticksItem#useOn} 自己调 {@link #placeOn} 摆 {@link HotPotContent#CHOPSTICK_REST_BLOCK}。
     */
    @org.jetbrains.annotations.Nullable
    public static PlateBlock placedFor(ItemStack stack) {
        if (stack.getItem() instanceof PlateItem item) {
            return item.plate();
        }
        if (stack.is(net.minecraft.world.item.Items.BOWL) || stack.getItem() instanceof SoupBowlItem
            || stack.getItem() instanceof DippingSauceItem) {
            return HotPotContent.BOWL_BLOCK;
        }
        if (stack.getItem() instanceof DrinkItem || stack.getItem() instanceof EmptyBottleItem) {
            return HotPotContent.DRINK_BLOCK;
        }
        if (stack.getItem() instanceof CupItem) {
            return HotPotContent.CUP_BLOCK;
        }
        return null;
    }

    /**
     * 整件摆上去（拿起来原样还回去）的东西：汤碗、饮料、空瓶、玻璃杯；
     * 还有直接摆在桌面上的竹筷（r136b，2026-11-05 用户）——这样空手拿起来或挖掉时，
     * 原样还回这双筷子以及它夹着的那道菜。
     */
    public static boolean keepsSelf(ItemStack stack) {
        return stack.getItem() instanceof SoupBowlItem || stack.getItem() instanceof DippingSauceItem
            || stack.getItem() instanceof DrinkItem
            || stack.getItem() instanceof EmptyBottleItem
            || stack.getItem() instanceof CupItem
            || stack.is(HotPotContent.CHOPSTICKS);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return placeOn(context, plate, false);
    }

    /**
     * 点在方块顶面：在上面那一格的「盘子格」里摆一个（盘子、碗、瓶子共用）。点的不是顶面返回 PASS，交给原版
     * （碗、汤碗、橙子饮料接着就是原版的用法）；锅和灶台上不摆。
     */
    public static InteractionResult placeOn(UseOnContext context, PlateBlock plate, boolean keepSelf) {
        Level world = context.getLevel();
        if (context.getClickedFace() != Direction.UP || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        BlockState clicked = world.getBlockState(context.getClickedPos());
        if (clicked.getBlock() instanceof HotPotBlock || StoveBlock.isStove(clicked)
            || clicked.getBlock() instanceof StoveTopBlock) {
            return InteractionResult.PASS;
        }
        BlockPos target = context.getClickedPos().above();
        // 桌面在方块中间（11/16），点在桌面上时目标是桌子上面那一格
        BlockState existing = world.getBlockState(target);
        if (!existing.is(HotPotContent.PLATE_HOLDER)) {
            BlockPlaceContext placement = new BlockPlaceContext(context);
            if (!existing.canBeReplaced(placement) || !PlateHolderBlock.supported(world, target)) {
                return InteractionResult.PASS;
            }
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!existing.is(HotPotContent.PLATE_HOLDER)) {
            world.setBlock(target, PlateHolderBlock.withSurface(HotPotContent.PLATE_HOLDER.defaultBlockState(), world, target),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        if (!(world.getBlockEntity(target) instanceof PlateHolderBlockEntity be)) {
            return InteractionResult.FAIL;
        }
        double hx = (context.getClickLocation().x - target.getX()) * 16;
        double hz = (context.getClickLocation().z - target.getZ()) * 16;
        ItemStack stack = context.getItemInHand();
        Direction.Axis axis = HotPotBlock.handleAxisFor(context.getHorizontalDirection());
        if (!be.tryAdd(plate, axis, hx, hz, stack, keepSelf)) {
            context.getPlayer().sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.plate_no_room")));
            if (be.plates().isEmpty()) {
                world.removeBlock(target, false);
            }
            return InteractionResult.FAIL;
        }
        world.playSound(null, target, plate.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        stack.consume(1, context.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
