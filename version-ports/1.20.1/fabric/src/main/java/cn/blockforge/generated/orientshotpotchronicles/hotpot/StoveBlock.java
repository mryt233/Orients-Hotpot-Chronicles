package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** 同一方块状态中保存彼此独立的灶台底座与可拆锅体。 */
public class StoveBlock extends Block implements BlockEntityProvider {
    public static final BooleanProperty LIT = BooleanProperty.of("lit");
    public static final EnumProperty<StovePot> POT = EnumProperty.of("pot", StovePot.class);
    public static final EnumProperty<SoupVisual> SOUP = EnumProperty.of("soup", SoupVisual.class);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /** 灶上锅的耳朵所在的轴（同 {@link HotPotBlock#AXIS}），只在有锅时有意义。 */
    public static final EnumProperty<Direction.Axis> POT_AXIS =
        EnumProperty.of("pot_axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);

    // 灶台顶面 11/16，中间是 1/16 内沿、7.6/16 深的方形锅槽；锅坐进槽里，
    // 锅底贴槽底、锅沿顶与方块顶齐平（16/16），锅身 13.5/16 宽，接近地上单独锅的大小。
    // 这些数值与锅模型（stove_*_hot_pot_shell）一一对应。
    public static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 11, 16);
    private static final VoxelShape POT_BODY = Block.createCuboidShape(1.25, 7.6, 1.25, 14.75, 13.6, 14.75);
    private static final VoxelShape POT_RIM_NORTH = Block.createCuboidShape(0.663, 13.6, 0.663, 15.337, 16, 1.837);
    private static final VoxelShape POT_RIM_SOUTH = Block.createCuboidShape(0.663, 13.6, 14.163, 15.337, 16, 15.337);
    private static final VoxelShape POT_RIM_WEST = Block.createCuboidShape(0.663, 13.6, 1.837, 1.837, 16, 14.163);
    private static final VoxelShape POT_RIM_EAST = Block.createCuboidShape(14.163, 13.6, 1.837, 15.337, 16, 14.163);
    private static final VoxelShape POT_HANDLE_WEST = Block.createCuboidShape(0, 14.08, 6.591, 0.663, 15.28, 9.409);
    private static final VoxelShape POT_HANDLE_EAST = Block.createCuboidShape(15.337, 14.08, 6.591, 16, 15.28, 9.409);
    private static final VoxelShape POT_HANDLE_NORTH = Block.createCuboidShape(6.591, 14.08, 0, 9.409, 15.28, 0.663);
    private static final VoxelShape POT_HANDLE_SOUTH = Block.createCuboidShape(6.591, 14.08, 15.337, 9.409, 15.28, 16);
    /** 台面顶面之上的最低命中高度：锅壁从台面往上这 1/4 像素算灶台，避免点槽口误判。 */
    private static final double POT_HIT_MIN_Y = 11.25 / 16.0;
    private static final double POT_FOOTPRINT_MIN = 0.663 / 16.0;
    private static final double POT_FOOTPRINT_MAX = 15.337 / 16.0;

    public static final VoxelShape POT_SHAPE = VoxelShapes.union(
        POT_BODY, POT_RIM_NORTH, POT_RIM_SOUTH, POT_RIM_WEST, POT_RIM_EAST,
        POT_HANDLE_WEST, POT_HANDLE_EAST).simplify();
    private static final VoxelShape POT_SHAPE_Z = VoxelShapes.union(
        POT_BODY, POT_RIM_NORTH, POT_RIM_SOUTH, POT_RIM_WEST, POT_RIM_EAST,
        POT_HANDLE_NORTH, POT_HANDLE_SOUTH).simplify();
    /** 高灶台比矮灶台抬高的像素：灶口、锅、汤整体上移（配高火锅桌，桌面 23/16，灶台砖面比桌面低 2 像素）。
     *  r83 是 11，r84 用户说桌子和灶台再低一点，一起降 1。 */
    public static final double TALL_LIFT = 10.0;

    /** 这一种灶台整体抬高多少像素（矮灶台 0，高灶台 {@link #TALL_LIFT}）。 */
    private final double lift;
    private final VoxelShape baseShape;
    private final VoxelShape combinedShape;
    private final VoxelShape combinedShapeZ;

    public StoveBlock() {
        this(0);
    }

    public StoveBlock(double lift) {
        // 破坏、工具、音效同原版砖块（Blocks.BRICKS）；挖掘工具见 data/minecraft/tags/block/mineable/pickaxe.json
        super(AbstractBlock.Settings.create()
            .mapColor(net.minecraft.block.MapColor.RED)
            .instrument(net.minecraft.block.enums.Instrument.BASEDRUM)
            .requiresTool()
            .strength(2.0F, 6.0F)
            .sounds(net.minecraft.sound.BlockSoundGroup.STONE)
            .nonOpaque()
            .dynamicBounds()
            .luminance(state -> state.get(LIT) ? 13 : 0));
        setDefaultState(getDefaultState()
            .with(LIT, false)
            .with(POT, StovePot.NONE)
            .with(SOUP, SoupVisual.NONE)
            .with(FACING, net.minecraft.util.math.Direction.NORTH)
            .with(POT_AXIS, Direction.Axis.X));
        this.lift = lift;
        this.baseShape = Block.createCuboidShape(0, 0, 0, 16, 11 + lift, 16);
        this.combinedShape = VoxelShapes.union(baseShape, POT_SHAPE.offset(0, lift / 16.0, 0)).simplify();
        this.combinedShapeZ = VoxelShapes.union(baseShape, POT_SHAPE_Z.offset(0, lift / 16.0, 0)).simplify();
    }

    /** 灶台整体抬高的像素（矮灶台 0）。 */
    public double lift() {
        return lift;
    }

    /** 这个方块状态是不是灶台（矮的或高的）。 */
    public static boolean isStove(BlockState state) {
        return state.getBlock() instanceof StoveBlock;
    }

    /** 灶台抬高的方块单位（不是灶台返回 0）。 */
    public static float liftOf(BlockState state) {
        return state.getBlock() instanceof StoveBlock stove ? (float) (stove.lift / 16.0) : 0F;
    }

    private VoxelShape shapeOf(BlockState state) {
        if (!state.get(POT).hasPot()) {
            return baseShape;
        }
        return state.get(POT_AXIS) == Direction.Axis.Z ? combinedShapeZ : combinedShape;
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT, POT, SOUP, FACING, POT_AXIS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // 高灶台伸进上面一格，上面要能放下「上半截」（StoveTopBlock）
        if (lift > 0 && !ctx.getWorld().getBlockState(ctx.getBlockPos().up()).canReplace(ctx)) {
            return null;
        }
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    /** 高灶台放下时在上面那一格放「上半截」，让伸上去的锅也点得到（r84 修 bug，）。 */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable net.minecraft.entity.LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        placeTop(world, pos);
    }

    /** 上面是空的（或能替换的）就补上「上半截」。 */
    public void placeTop(World world, BlockPos pos) {
        if (lift <= 0 || world.isClient) {
            return;
        }
        BlockState above = world.getBlockState(pos.up());
        if (!above.isOf(HotPotContent.STOVE_TOP) && above.isReplaceable()) {
            world.setBlockState(pos.up(), HotPotContent.STOVE_TOP.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                                         BlockPos pos, ShapeContext context) {
        // 锅沉进锅槽后整体仍在本方块高度内，不再需要按视线在台面/锅体之间二选一。
        return shapeOf(state);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos) {
        return shapeOf(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.BlockView world,
                                           BlockPos pos, ShapeContext context) {
        return shapeOf(state);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StoveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (type == HotPotContent.STOVE_BE) {
            return (w, p, s, be) -> StoveBlockEntity.tick(w, p, s, (StoveBlockEntity) be);
        }
        return null;
    }

    /** 1.20.1 移植：1.20 只有这一个右键入口，照 1.21 的顺序分给 useWithItem / useWithoutItem。 */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        return ItemActionResult.dispatch(state, player, hand,
            () -> useWithItem(player.getStackInHand(hand), state, world, pos, player, hand, hit),
            () -> useWithoutItem(state, world, pos, player, hit));
    }

    public ActionResult useWithoutItem(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof StoveBlockEntity stove)) {
            return ActionResult.PASS;
        }
        boolean potHit = isPotHit(state, pos, hit);
        boolean hasPot = state.get(POT).hasPot();
        if (potHit && hasPot && player.isSneaking()) {
            if (!world.isClient) {
                stove.detachPot(player);
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient) {
            if (potHit && hasPot) {
                if (!player.getMainHandStack().isEmpty()) {
                    // 手里拿着燃料/方块等与锅无关的物品时，onUseWithItem 会把它们放行到这里。
                    // 此时直接开灶膛界面，绝不能让锅把燃料入口挡死。
                    player.sendMessage(stove.statusText(), true);
                    stove.openFuelScreen(player);
                } else {
                    // 空手点锅体：先按准星格收取熟食，没有可取的才提示状态。
                    int bigCell = HotPotInteraction.bigCellFromHit(pos, hit,
                        HotPotInteraction.STOVE_INNER_MIN, HotPotInteraction.STOVE_INNER_SIZE);
                    if (!stove.collectCookedSlot(player, stove.firstCookedSlotInCell(bigCell))) {
                        player.sendMessage(stove.statusText(), true);
                    }
                }
            } else {
                // 像熔炉一样：右键灶台（没锅、或点在砖砌灶身上）打开灶膛，
                // 燃料放进界面里的专用槽位；手里拿着什么都不会挡住这个入口。
                if (!hasPot && potRelated(world, player.getMainHandStack())) {
                    player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.no_pot")), true);
                }
                stove.openFuelScreen(player);
            }
        }
        return ActionResult.SUCCESS;
    }

    public ItemActionResult useWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof StoveBlockEntity stove)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            HotPotBlock pot = potOf(blockItem);
            if (pot != null) {
                if (!world.isAir(pos.up()) && !world.getBlockState(pos.up()).isOf(HotPotContent.STOVE_TOP)) {
                    if (!world.isClient) {
                        player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.stove_blocked")), true);
                    }
                    return ItemActionResult.FAIL;
                }
                if (state.get(POT).hasPot()) {
                    if (!world.isClient) {
                        player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.stove_has_pot")), true);
                    }
                    return ItemActionResult.FAIL;
                }
                if (!world.isClient) {
                    stove.attachPot(pot.material(),
                        HotPotBlock.handleAxisFor(player.getHorizontalFacing()));
                    stove.loadPotContents(stack);
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND,
                        SoundCategory.BLOCKS, 0.3F, 1.5F);
                }
                return ItemActionResult.SUCCESS;
            }
        }

        boolean potHit = isPotHit(state, pos, hit);
        boolean hasPot = state.get(POT).hasPot();
        boolean servingTool = HotPotInteraction.isServingTool(stack);
        boolean soupSource = SoupBase.fromStack(stack) != null;
        boolean ingredient = HotPotCooking.acceptable(world, stack);
        boolean scoop = stove.canScoopWith(stack);
        boolean plate = HotPotBlock.isPlate(stack);

        // 潜行点锅体是拆锅，不能被手里的食材/汤底抢走；交给 onUse 处理。
        if (player.isSneaking() && potHit && hasPot) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 只有“确实点在锅体上、灶上确实有锅、而且手里拿的是跟锅有关的物品”才走锅交互。
        // 其余情况一律放行给 onUse 打开灶膛界面：以前这里对食材/工具直接 FAIL，
        // 锅装上后又几乎盖满台面，结果就是拿着东西右键打不开灶台、也加不进燃料。
        if (!potHit || !hasPot || !(plate || scoop || servingTool || soupSource || ingredient)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int bigCell = HotPotInteraction.bigCellFromHit(pos, hit,
                    HotPotInteraction.STOVE_INNER_MIN, HotPotInteraction.STOVE_INNER_SIZE);
        int cookedSlot = stove.firstCookedSlotInCell(bigCell);
        if (plate) {
            if (!world.isClient) {
                stove.pourPlate(player, hand);
            }
            return ItemActionResult.SUCCESS;
        }
        if (scoop) {
            if (!world.isClient) {
                stove.scoopSoup(player, hand);
            }
            return ItemActionResult.SUCCESS;
        }
        if (stack.isOf(net.minecraft.item.Items.BOWL)) {
            if (!world.isClient) {
                stove.serveWithBowl(player, hand, bigCell);
            }
            return ItemActionResult.SUCCESS;
        }
        if (servingTool) {
            if (stack.isOf(HotPotContent.CHOPSTICKS)) {
                // 筷子上夹着盘子里的菜：放进准星对着的那一格；空筷子开面板（捞取交给碗）
                if (!world.isClient) {
                    if (!Chopsticks.held(stack).isEmpty()) {
                        stove.insertHeld(player, stack, stove.firstEmptySlotInCell(bigCell));
                    } else {
                        stove.useWithChopsticks(player);
                    }
                }
                return ItemActionResult.SUCCESS;
            }
            if (!world.isClient && !stove.collectCookedSlot(player, cookedSlot)) {
                player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.no_cooked_here")), true);
            }
            return ItemActionResult.SUCCESS;
        }
        if (soupSource) {
            if (!world.isClient && !stove.pourSoup(player, hand)) {
                return ItemActionResult.FAIL;
            }
            return ItemActionResult.SUCCESS;
        }
        if (!world.isClient && !stove.insertIngredient(player, hand, stove.firstEmptySlotInCell(bigCell))) {
            return ItemActionResult.FAIL;
        }
        return ItemActionResult.SUCCESS;
    }

    /** 手里这件东西是不是冲着锅去的（捞取工具、汤底、食材）。 */
    private static boolean potRelated(World world, ItemStack stack) {
        return HotPotInteraction.isServingTool(stack)
            || SoupBase.fromStack(stack) != null
            || HotPotCooking.acceptable(world, stack);
    }

    /**
     * 判断这一下点的是锅还是灶台。
     *
     * 锅沉在槽里，锅腔（含空锅的内底）低于台面高度，只看 y 会把锅里的点击误判成灶膛，
     * 所以先看命中点是否落在锅沿投影范围内，再看它是否高过台面。
     */
    public static boolean isPotHit(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (!state.get(POT).hasPot()) {
            return false;
        }
        double localX = hit.getPos().x - pos.getX();
        double localZ = hit.getPos().z - pos.getZ();
        boolean inFootprint = localX >= POT_FOOTPRINT_MIN && localX <= POT_FOOTPRINT_MAX
            && localZ >= POT_FOOTPRINT_MIN && localZ <= POT_FOOTPRINT_MAX;
        return inFootprint || hit.getPos().y - pos.getY() >= POT_HIT_MIN_Y + liftOf(state);
    }

    /**
     * 玩家准星是否落在灶上那口锅上（挖掘时用：挖锅只挖锅，挖灶身才挖整个灶台）。
     * 服务端和客户端用同一套射线判断，挖掘速度和最终结果才一致。
     */
    public static boolean aimsAtPot(BlockState state, BlockPos pos, PlayerEntity player) {
        if (!isStove(state) || !state.get(POT).hasPot()) {
            return false;
        }
        HitResult hit = player.raycast(player.isCreative() ? 5.0 : 4.5, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        // 点在高灶台的「上半截」上也算点在这个灶台上（准星位置是一样的）
        BlockPos at = blockHit.getBlockPos();
        boolean here = at.equals(pos)
            || (at.equals(pos.up()) && player.getWorld().getBlockState(at).isOf(HotPotContent.STOVE_TOP));
        return here && isPotHit(state, pos, blockHit);
    }

    /** 对着锅挖时，用锅自己的硬度和工具要求（铜锅同铜块、铁锅同铁块）。 */
    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player,
                                           net.minecraft.world.BlockView world, BlockPos pos) {
        HotPotMaterial material = state.get(POT).material();
        if (material != null && aimsAtPot(state, pos, player)) {
            return HotPotContent.blockFor(material).getDefaultState()
                .calcBlockBreakingDelta(player, world, pos);
        }
        return super.calcBlockBreakingDelta(state, player, world, pos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player != null
            && world.getBlockEntity(pos) instanceof StoveBlockEntity stove) {
            stove.markBreakHandled();
            if (!player.isCreative()) {
                stove.dropAttachedPot(state.get(POT).material());
                stove.dropContents();
                stove.dropFuel();
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                   BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
            && world.getBlockEntity(pos) instanceof StoveBlockEntity stove
            && !stove.consumeBreakHandled()) {
            stove.dropAttachedPot(state.get(POT).material());
            stove.dropContents();
            stove.dropFuel();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof StoveBlockEntity stove) {
            return state.get(POT).hasPot() ? stove.comparatorLevel() : stove.fuelComparatorLevel();
        }
        return 0;
    }

    private static HotPotBlock potOf(BlockItem item) {
        return item.getBlock() instanceof HotPotBlock pot ? pot : null;
    }
}
