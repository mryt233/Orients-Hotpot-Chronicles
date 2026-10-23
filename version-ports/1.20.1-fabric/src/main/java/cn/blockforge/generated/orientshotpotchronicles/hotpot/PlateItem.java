package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * 盘子物品。不是直接放一个方块，而是在点的那个面上方摆进「盘子格」（{@link PlateHolderBlock}），
 * 一格里能摆好几个。拿着它右键锅是把菜倒进锅（锅那边处理）。
 */
public class PlateItem extends BlockItem {
    private final PlateBlock plate;

    public PlateItem(PlateBlock plate, Settings settings) {
        super(plate, settings);
        this.plate = plate;
    }

    public PlateBlock plate() {
        return plate;
    }

    /**
     * 物品上有菜（盘子装了菜 / 筷子夹着菜）时用自定义模型数据 1，切到 builtin/entity 模型，
     * 由客户端的 HeldFoodItemRenderer 先画原来的样子再把菜画上去；没菜就去掉，用原来的模型（Claude，r91）。
     */
    public static void syncFoodModel(ItemStack stack, boolean hasFood) {
        if (hasFood) {
            ItemData.setModel(stack, 1);
        } else {
            ItemData.clearModel(stack);
        }
    }

    /** 盘子物品按 container 组件里有没有菜同步模型。 */
    public static void syncPlateModel(ItemStack stack) {
        syncFoodModel(stack, ItemData.hasContents(stack));
    }

    /** 能不能装进盘子：能下锅的生食材，或者本身能吃的东西（熟食也能装）；汤碗、果橙多这种整件摆的不算。 */
    public static boolean holdsFood(World world, ItemStack stack) {
        return !stack.isEmpty() && placedFor(stack) == null && (stack.isFood() || HotPotCooking.acceptable(world, stack));
    }

    /**
     * 手里这件东西摆到桌上是什么（r99 起不只盘子）：盘子 → 盘子；原版的碗 → 碗（装的菜跟着）；
     * 汤碗 → 碗（整件记着）；果橙多 / 空瓶 → 瓶子（整件记着）。不能摆返回 null。
     */
    @org.jetbrains.annotations.Nullable
    public static PlateBlock placedFor(ItemStack stack) {
        if (stack.getItem() instanceof PlateItem item) {
            return item.plate();
        }
        if (stack.isOf(net.minecraft.item.Items.BOWL) || stack.getItem() instanceof SoupBowlItem) {
            return HotPotContent.BOWL_BLOCK;
        }
        if (stack.getItem() instanceof DrinkItem || stack.getItem() instanceof EmptyBottleItem) {
            return HotPotContent.DRINK_BLOCK;
        }
        return null;
    }

    /** 整件摆上去（拿起来原样还回去）的东西：汤碗、饮料、空瓶。 */
    public static boolean keepsSelf(ItemStack stack) {
        return stack.getItem() instanceof SoupBowlItem || stack.getItem() instanceof DrinkItem
            || stack.getItem() instanceof EmptyBottleItem;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return placeOn(context, plate, false);
    }

    /**
     * 点在方块顶面：在上面那一格的「盘子格」里摆一个（盘子、碗、瓶子共用）。点的不是顶面返回 PASS，交给原版
     * （碗、汤碗、果橙多接着就是原版的用法）；锅和灶台上不摆。
     */
    public static ActionResult placeOn(ItemUsageContext context, PlateBlock plate, boolean keepSelf) {
        World world = context.getWorld();
        if (context.getSide() != Direction.UP || context.getPlayer() == null) {
            return ActionResult.PASS;
        }
        BlockState clicked = world.getBlockState(context.getBlockPos());
        if (clicked.getBlock() instanceof HotPotBlock || StoveBlock.isStove(clicked)
            || clicked.getBlock() instanceof StoveTopBlock) {
            return ActionResult.PASS;
        }
        BlockPos target = context.getBlockPos().up();
        // 桌面在方块中间（11/16），点在桌面上时目标是桌子上面那一格
        BlockState existing = world.getBlockState(target);
        if (!existing.isOf(HotPotContent.PLATE_HOLDER)) {
            ItemPlacementContext placement = new ItemPlacementContext(context);
            if (!existing.canReplace(placement) || !PlateHolderBlock.supported(world, target)) {
                return ActionResult.PASS;
            }
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!existing.isOf(HotPotContent.PLATE_HOLDER)) {
            world.setBlockState(target, PlateHolderBlock.withSurface(HotPotContent.PLATE_HOLDER.getDefaultState(), world, target));
        }
        if (!(world.getBlockEntity(target) instanceof PlateHolderBlockEntity be)) {
            return ActionResult.FAIL;
        }
        double hx = (context.getHitPos().x - target.getX()) * 16;
        double hz = (context.getHitPos().z - target.getZ()) * 16;
        ItemStack stack = context.getStack();
        Direction.Axis axis = HotPotBlock.handleAxisFor(context.getHorizontalPlayerFacing());
        if (!be.tryAdd(plate, axis, hx, hz, stack, keepSelf)) {
            context.getPlayer().sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
            if (be.plates().isEmpty()) {
                world.removeBlock(target, false);
            }
            return ActionResult.FAIL;
        }
        world.playSound(null, target, plate.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!context.getPlayer().getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
