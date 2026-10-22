package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * 摆盘子的那一格（看不见，盘子和菜由 PlateHolderRenderer 画）。下面是火锅桌时整体下移贴着桌面。
 *
 * <ul>
 *   <li>拿盘子右键：在点的位置附近再摆一个（放不下会提示）。</li>
 *   <li>拿食物右键：放进点到的那个盘子。</li>
 *   <li>拿筷子右键：空筷子从点到的盘子夹走最后放的那样；筷子上有菜就放回这个盘子。</li>
 *   <li>空手右键：把点到的那个盘子连菜拿起来；最后一个拿走后这一格变回空气。</li>
 * </ul>
 * 挖掉这一格：所有盘子带着菜掉出来。
 */
public class PlateHolderBlock extends Block implements BlockEntityProvider {
    /** 下面是火锅桌（桌面 11/16，盘子下移 5 像素）。 */
    public static final BooleanProperty ON_TABLE = BooleanProperty.of("on_table");
    /** 下面是高火锅桌（桌面 23/16，盘子上移 7 像素）。 */
    public static final BooleanProperty ON_TALL_TABLE = BooleanProperty.of("on_tall_table");

    public PlateHolderBlock() {
        super(AbstractBlock.Settings.create()
            .mapColor(MapColor.CLEAR)
            .strength(0.3F)
            .sounds(BlockSoundGroup.DECORATED_POT)
            .nonOpaque()
            .dropsNothing()
            // 形状随方块实体（摆了哪些盘子）变、矮桌上还伸出本格：不让原版提前缓存形状（同潜影盒），
            // 否则缓存的是空世界里的默认小方块，和真实碰撞对不上，站上去会抽搐（Claude 改）
            .dynamicBounds()
            .pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY));
        setDefaultState(getDefaultState().with(ON_TABLE, false).with(ON_TALL_TABLE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ON_TABLE, ON_TALL_TABLE);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlateHolderBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    /** 盘子底面离这一格底面多高（像素）：火锅桌上 -5（桌面 11/16），高火锅桌上 +7（桌面 23/16），地上 0。 */
    public static double lift(BlockState state) {
        if (state.get(ON_TALL_TABLE)) {
            return 7.0;
        }
        return state.get(ON_TABLE) ? -PlateSize.TABLE_DROP : 0;
    }

    /** 按下面的方块定好 ON_TABLE / ON_TALL_TABLE。 */
    public static BlockState withSurface(BlockState state, BlockView world, BlockPos pos) {
        net.minecraft.block.Block below = world.getBlockState(pos.down()).getBlock();
        boolean tall = below instanceof TallHotPotTableBlock;
        return state.with(ON_TALL_TABLE, tall).with(ON_TABLE, !tall && below instanceof HotPotTableBlock);
    }

    /** 下面是火锅桌，或者顶面能放东西的方块。 */
    public static boolean supported(WorldView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        return below.getBlock() instanceof HotPotTableBlock
            || Block.sideCoversSmallSquare(world, pos.down(), Direction.UP);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return supported(world, pos);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            if (!supported(world, pos)) {
                return net.minecraft.block.Blocks.AIR.getDefaultState();
            }
            return withSurface(state, world, pos);
        }
        return state;
    }

    private static VoxelShape shapeOf(BlockState state, BlockView world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be
            ? be.shape(lift(state))
            : Block.createCuboidShape(6, 0, 6, 10, 1, 10);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(state, world, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(state, world, pos);
    }

    /** 挖掉或被冲掉时，所有盘子带着菜掉出来（这一格本身不掉东西）。 */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
            && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be) {
            for (PlateHolderBlockEntity.Placed p : be.plates()) {
                Block.dropStack(world, pos, p.toItem());
            }
            be.plates().clear();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    /** 客户端填：当前玩家（鼠标中键选方块时用来找准星对着哪个盘子）；服务端一直是 null。 */
    public static java.util.function.Supplier<PlayerEntity> pickPlayer = () -> null;

    /** 鼠标中键：复制准星对着的那个盘子，里面的菜一起带上（r94，Claude）。 */
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        PlayerEntity player = pickPlayer.get();
        if (player != null && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be) {
            PlateHolderBlockEntity.Placed plate = aimed(player, pos, be);
            if (plate == null && !be.plates().isEmpty()) {
                plate = be.plates().get(0);
            }
            if (plate != null) {
                return plate.toItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /** 准星对着的是这一格里的哪个盘子（服务端和客户端同一套射线）。 */
    @org.jetbrains.annotations.Nullable
    public static PlateHolderBlockEntity.Placed aimed(PlayerEntity player, BlockPos pos, PlateHolderBlockEntity be) {
        net.minecraft.util.hit.HitResult hit = player.raycast(player.getBlockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
            return null;
        }
        double[] at = local(pos, blockHit);
        return be.at(at[0], at[1]);
    }

    /**
     * 挖盘子：只挖准星对着的那一个，带着菜掉出来（创造模式直接拿掉），别的盘子留着；最后一个挖掉后这一格变空气。
     * 准星没对着任何盘子就什么都不做。
     */
    public static void breakOne(World world, BlockPos pos, PlateHolderBlockEntity be, PlayerEntity player) {
        PlateHolderBlockEntity.Placed plate = aimed(player, pos, be);
        if (plate == null) {
            return;
        }
        BlockState plateState = plate.plate.getDefaultState();
        world.syncWorldEvent(null, net.minecraft.world.WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(plateState));
        if (!player.isCreative()) {
            Block.dropStack(world, pos, plate.toItem());
        }
        be.remove(plate);
        if (be.plates().isEmpty()) {
            world.removeBlock(pos, false);
        }
    }

    // ------------------------------------------------------------------ 交互

    /** 点到的位置在这一格里的 x、z（像素）。 */
    private static double[] local(BlockPos pos, BlockHitResult hit) {
        Vec3d v = hit.getPos().subtract(Vec3d.of(pos)).multiply(16);
        return new double[]{v.x, v.z};
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        double[] at = local(pos, hit);
        PlateBlock placing = PlateItem.placedFor(stack);
        if (placing != null) {
            // 盘子、碗、汤碗、果橙多：在这一格再摆一个（r99 起碗和瓶子也走这里）
            if (!world.isClient) {
                if (be.tryAdd(placing, HotPotBlock.handleAxisFor(player.getHorizontalFacing()), at[0], at[1], stack,
                    PlateItem.keepsSelf(stack))) {
                    stack.decrementUnlessCreative(1, player);
                    world.playSound(null, pos, placing.getDefaultState().getSoundGroup().getPlaceSound(),
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                } else {
                    player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
                }
            }
            return ItemActionResult.SUCCESS;
        }
        PlateHolderBlockEntity.Placed plate = be.at(at[0], at[1]);
        if (plate == null) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!plate.holdsFood() && (stack.isOf(HotPotContent.CHOPSTICKS) || PlateItem.holdsFood(world, stack))) {
            // 汤碗、果橙多里放不了菜，也夹不出菜
            if (!world.isClient) {
                player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.cannot_hold_food")), true);
            }
            return ItemActionResult.SUCCESS;
        }
        if (stack.isOf(HotPotContent.CHOPSTICKS)) {
            if (!world.isClient) {
                ItemStack held = Chopsticks.held(stack);
                if (held.isEmpty()) {
                    ItemStack taken = plate.takeLast();
                    if (taken.isEmpty()) {
                        player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_empty")), true);
                    } else {
                        Chopsticks.hold(stack, taken);
                        be.changed();
                        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35F, 1.5F);
                    }
                } else if (plate.add(held)) {
                    Chopsticks.hold(stack, ItemStack.EMPTY);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35F, 1.2F);
                } else {
                    player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_full")), true);
                }
            }
            return ItemActionResult.SUCCESS;
        }
        if (PlateItem.holdsFood(world, stack)) {
            if (!world.isClient) {
                if (plate.add(stack)) {
                    stack.decrementUnlessCreative(1, player);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35F, 1.2F);
                } else {
                    player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_full")), true);
                }
            }
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键：把点到的那个盘子连菜拿起来；潜行时改成取出最后放的那样菜。 */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.getMainHandStack().isEmpty() || !(world.getBlockEntity(pos) instanceof PlateHolderBlockEntity be)) {
            return ActionResult.PASS;
        }
        double[] at = local(pos, hit);
        PlateHolderBlockEntity.Placed plate = be.at(at[0], at[1]);
        if (plate == null) {
            return ActionResult.PASS;
        }
        // 潜行 + 空手右键：按「最后放进去的先出来」取一样菜进背包，按一下取一个（r96，Claude）
        if (player.isSneaking()) {
            if (!world.isClient) {
                ItemStack taken = plate.takeLast();
                if (taken.isEmpty()) {
                    player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.plate_empty")), true);
                } else {
                    player.getInventory().offerOrDrop(taken);
                    be.changed();
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35F, 1.5F);
                }
            }
            return ActionResult.SUCCESS;
        }
        if (!world.isClient) {
            be.remove(plate);
            player.setStackInHand(Hand.MAIN_HAND, plate.toItem());
            world.playSound(null, pos, plate.plate.getDefaultState().getSoundGroup().getHitSound(),
                SoundCategory.BLOCKS, 0.8F, 1.2F);
            if (be.plates().isEmpty()) {
                world.removeBlock(pos, false);
            }
        }
        return ActionResult.SUCCESS;
    }
}
