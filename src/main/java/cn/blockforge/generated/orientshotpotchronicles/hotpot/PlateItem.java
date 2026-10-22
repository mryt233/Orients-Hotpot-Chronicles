package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
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
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new net.minecraft.component.type.CustomModelDataComponent(1));
        } else {
            stack.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
        }
    }

    /** 盘子物品按 container 组件里有没有菜同步模型。 */
    public static void syncPlateModel(ItemStack stack) {
        syncFoodModel(stack, stack.getOrDefault(DataComponentTypes.CONTAINER,
            net.minecraft.component.type.ContainerComponent.DEFAULT).streamNonEmpty().findAny().isPresent());
    }

    /** 能不能装进盘子：能下锅的生食材，或者本身能吃的东西（熟食也能装）。 */
    public static boolean holdsFood(World world, ItemStack stack) {
        return !stack.isEmpty() && (stack.contains(DataComponentTypes.FOOD) || HotPotCooking.acceptable(world, stack));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (context.getSide() != Direction.UP || context.getPlayer() == null) {
            return ActionResult.PASS;
        }
        BlockPos target = context.getBlockPos().up();
        // 桌面在方块中间（11/16），点在桌面上时目标是桌子上面那一格
        BlockState existing = world.getBlockState(target);
        if (!existing.isOf(HotPotContent.PLATE_HOLDER)) {
            ItemPlacementContext placement = new ItemPlacementContext(context);
            if (!existing.canReplace(placement) || !PlateHolderBlock.supported(world, target)) {
                return ActionResult.FAIL;
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
        if (!be.tryAdd(plate, axis, hx, hz, stack)) {
            context.getPlayer().sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
            if (be.plates().isEmpty()) {
                world.removeBlock(target, false);
            }
            return ActionResult.FAIL;
        }
        world.playSound(null, target, plate.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
        stack.decrementUnlessCreative(1, context.getPlayer());
        return ActionResult.SUCCESS;
    }
}
