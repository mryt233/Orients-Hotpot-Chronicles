// mryt233 改：26.1 原版 API；合入 HotPotInteraction 与 Use 适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

/** 同一方块状态中保存彼此独立的灶台底座与可拆锅体。 */
public class StoveBlock extends Block implements PartOutline, EntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final EnumProperty<StovePot> POT = EnumProperty.create("pot", StovePot.class);
    public static final EnumProperty<SoupVisual> SOUP = EnumProperty.create("soup", SoupVisual.class);
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 灶上锅的耳朵所在的轴（同 {@link HotPotBlock#AXIS}），只在有锅时有意义。 */
    public static final EnumProperty<Direction.Axis> POT_AXIS =
        EnumProperty.create("pot_axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);

    // 灶台顶面 11/16，中间是 1/16 内沿、7.6/16 深的方形锅槽；锅坐进槽里，
    // 锅底贴槽底、锅沿顶与方块顶齐平（16/16），锅身 13.5/16 宽，接近地上单独锅的大小。
    // 这些数值与锅模型（stove_*_hot_pot_shell）一一对应。
    public static final VoxelShape BASE_SHAPE = Block.box(0, 0, 0, 16, 11, 16);
    private static final VoxelShape POT_BODY = Block.box(1.25, 7.6, 1.25, 14.75, 13.6, 14.75);
    private static final VoxelShape POT_RIM_NORTH = Block.box(0.663, 13.6, 0.663, 15.337, 16, 1.837);
    private static final VoxelShape POT_RIM_SOUTH = Block.box(0.663, 13.6, 14.163, 15.337, 16, 15.337);
    private static final VoxelShape POT_RIM_WEST = Block.box(0.663, 13.6, 1.837, 1.837, 16, 14.163);
    private static final VoxelShape POT_RIM_EAST = Block.box(14.163, 13.6, 1.837, 15.337, 16, 14.163);
    private static final VoxelShape POT_HANDLE_WEST = Block.box(0, 14.08, 6.591, 0.663, 15.28, 9.409);
    private static final VoxelShape POT_HANDLE_EAST = Block.box(15.337, 14.08, 6.591, 16, 15.28, 9.409);
    private static final VoxelShape POT_HANDLE_NORTH = Block.box(6.591, 14.08, 0, 9.409, 15.28, 0.663);
    private static final VoxelShape POT_HANDLE_SOUTH = Block.box(6.591, 14.08, 15.337, 9.409, 15.28, 16);
    /** 台面顶面之上的最低命中高度：锅壁从台面往上这 1/4 像素算灶台，避免点槽口误判。 */
    private static final double POT_HIT_MIN_Y = 11.25 / 16.0;
    private static final double POT_FOOTPRINT_MIN = 0.663 / 16.0;
    private static final double POT_FOOTPRINT_MAX = 15.337 / 16.0;

    public static final VoxelShape POT_SHAPE = Shapes.or(
        POT_BODY, POT_RIM_NORTH, POT_RIM_SOUTH, POT_RIM_WEST, POT_RIM_EAST,
        POT_HANDLE_WEST, POT_HANDLE_EAST).optimize();
    private static final VoxelShape POT_SHAPE_Z = Shapes.or(
        POT_BODY, POT_RIM_NORTH, POT_RIM_SOUTH, POT_RIM_WEST, POT_RIM_EAST,
        POT_HANDLE_NORTH, POT_HANDLE_SOUTH).optimize();
    /** 高灶台比矮灶台抬高的像素：灶口、锅、汤整体上移（配高火锅桌，桌面 23/16，灶台砖面比桌面低 2 像素）。
     * r83 是 11，r84 用户说桌子和灶台再低一点，一起降 1。 */
    public static final double TALL_LIFT = 10.0;

    /** 这一种灶台整体抬高多少像素（矮灶台 0，高灶台 {@link #TALL_LIFT}）。 */
    private final double lift;
    private final VoxelShape baseShape;
    private final VoxelShape combinedShape;
    private final VoxelShape combinedShapeZ;

    public StoveBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        this(0, properties);
    }

    public StoveBlock(double lift, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        // 破坏、音效同原版砖块（Blocks.BRICKS）；挖掘工具见 data/minecraft/tags/block/mineable/pickaxe.json。
        // mryt233 改（2026-11-05）：不强制工具。灶台里可能坐着整口锅和燃料，空手或拿错工具挖也要照常
        // 掉出灶台（掉落逻辑见 StoveBlock#playerWillDestroy），不能因为没拿镐子就把玩家的东西吞掉；
        // 镐子只是挖得快。上半截 tall_brick_stove_top 也照这里，见 StoveTopBlock。
        super(properties
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(2.0F, 6.0F)
            .sound(SoundType.STONE)
            .noOcclusion()
            .dynamicShape()
            .lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        registerDefaultState(defaultBlockState()
            .setValue(LIT, false)
            .setValue(POT, StovePot.NONE)
            .setValue(SOUP, SoupVisual.NONE)
            .setValue(FACING, Direction.NORTH)
            .setValue(POT_AXIS, Direction.Axis.X));
        this.lift = lift;
        this.baseShape = Block.box(0, 0, 0, 16, 11 + lift, 16);
        this.combinedShape = Shapes.or(baseShape, POT_SHAPE.move(0, lift / 16.0, 0)).optimize();
        this.combinedShapeZ = Shapes.or(baseShape, POT_SHAPE_Z.move(0, lift / 16.0, 0)).optimize();
    }

    /** 选中框只框准星对着的那样：锅（连锅耳朵）或者灶身（PartOutline）。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        if (!state.getValue(POT).hasPot()) {
            return null;
        }
        if (aimsAtPot(state, pos, player)) {
            VoxelShape pot = state.getValue(POT_AXIS) == Direction.Axis.Z ? POT_SHAPE_Z : POT_SHAPE;
            return pot.move(0, lift / 16.0, 0);
        }
        return baseShape;
    }

    /**
     * 某个高度的灶台在某种锅状态下的形状，和构造器里预算的 baseShape / combinedShape 同一算法。
     * 高灶台「上半截」按自己记下的锅状态取形状用（不能去读世界，见 StoveTopBlock；mryt233 2026-11-01）。
     */
    static VoxelShape shapeFor(double lift, boolean hasPot, Direction.Axis axis) {
        VoxelShape base = Block.box(0, 0, 0, 16, 11 + lift, 16);
        if (!hasPot) {
            return base;
        }
        return Shapes.or(base, (axis == Direction.Axis.Z ? POT_SHAPE_Z : POT_SHAPE).move(0, lift / 16.0, 0)).optimize();
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
        if (state.getBlock() instanceof HotPotBlock && state.getValue(HotPotBlock.ON_CAMPFIRE)) {
            return (float) (-HotPotBlock.CAMPFIRE_DROP / 16.0);   // 坐在篝火上的锅整体下沉
        }
        return state.getBlock() instanceof StoveBlock stove ? (float) (stove.lift / 16.0) : 0F;
    }

    private VoxelShape shapeOf(BlockState state) {
        if (!state.getValue(POT).hasPot()) {
            return baseShape;
        }
        return state.getValue(POT_AXIS) == Direction.Axis.Z ? combinedShapeZ : combinedShape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POT, SOUP, FACING, POT_AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 高灶台伸进上面一格，上面要能放下「上半截」（StoveTopBlock）
        if (lift > 0 && !ctx.getLevel().getBlockState(ctx.getClickedPos().above()).canBeReplaced(ctx)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /** 高灶台放下时在上面那一格放「上半截」，让伸上去的锅也点得到（r84 修 bug）。 */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        placeTop(world, pos);
    }

    /** 上面是空的（或能替换的）就补上「上半截」；已有的上半截记的锅状态不对就纠正（旧存档没有这个状态，mryt233 2026-11-01）。 */
    public void placeTop(Level world, BlockPos pos) {
        if (lift <= 0 || world.isClientSide()) {
            return;
        }
        BlockState above = world.getBlockState(pos.above());
        BlockState want = StoveTopBlock.mirroring(world.getBlockState(pos));
        if (above.is(HotPotContent.STOVE_TOP)) {
            if (above != want) {
                world.setBlock(pos.above(), want, Block.UPDATE_CLIENTS);
            }
        } else if (above.canBeReplaced()) {
            world.setBlock(pos.above(), want, Block.UPDATE_ALL);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world,
                                  BlockPos pos, CollisionContext context) {
        // 锅沉进锅槽后整体仍在本方块高度内，不再需要按视线在台面/锅体之间二选一。
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world,
                                           BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == HotPotContent.STOVE_BE) {
            return (w, p, s, be) -> StoveBlockEntity.tick(w, p, s, (StoveBlockEntity) be);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof StoveBlockEntity stove)) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
        }
        boolean potHit = isPotHit(state, pos, hit);
        boolean hasPot = state.getValue(POT).hasPot();
        if (potHit && hasPot && player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                stove.detachPot(player);
            }
            return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide());
        }

        if (world.isClientSide() && potHit && hasPot && player.getMainHandItem().isEmpty()) {
            // 空手捞看到的那样（2026-10-26）：先把这一刻浮在准星那格上的是哪样报给服务端
            HotPotInteraction.clientChopsticksAim.accept(stove, HotPotInteraction.cellFromHit(stove, player, pos, hit,
                HotPotInteraction.STOVE_INNER_MIN, HotPotInteraction.STOVE_INNER_SIZE));
        }
        if (!world.isClientSide()) {
            if (potHit && hasPot) {
                if (!player.getMainHandItem().isEmpty()) {
                    // 手里拿着燃料/方块等与锅无关的物品时，onUseWithItem 会把它们放行到这里。
                    // 此时直接开灶膛界面，绝不能让锅把燃料入口挡死。
                    player.sendOverlayMessage(stove.statusText());
                    stove.openFuelScreen(player);
                } else {
                    // 空手点锅体：先按准星格收取熟食，没有可取的才提示状态。
                    int bigCell = HotPotInteraction.cellFromHit(stove, player, pos, hit,
                        HotPotInteraction.STOVE_INNER_MIN, HotPotInteraction.STOVE_INNER_SIZE);
                    if (!stove.collectShown(player, bigCell)) {
                        player.sendOverlayMessage(stove.statusText());
                    }
                }
            } else {
                // 像熔炉一样：右键灶台（没锅、或点在砖砌灶身上）打开灶膛，
                // 燃料放进界面里的专用槽位；手里拿着什么都不会挡住这个入口。
                if (!hasPot && potRelated(world, player.getMainHandItem())) {
                    player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.no_pot")));
                }
                stove.openFuelScreen(player);
            }
        }
        return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof StoveBlockEntity stove)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        // 其他模组（面条物语等）的东西先处理：煮面锅要放到灶台上、空碗要先盛汤（见 HotPotHooks）
        InteractionResult hooked = HotPotHooks.blockUse(world, player, hand, stack, pos, state, hit);
        if (hooked != null) {
            return hooked;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            HotPotBlock pot = potOf(blockItem);
            if (pot != null) {
                if (!world.isEmptyBlock(pos.above()) && !world.getBlockState(pos.above()).is(HotPotContent.STOVE_TOP)) {
                    if (!world.isClientSide()) {
                        player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.stove_blocked")));
                    }
                    return Use.blockItem(UseOutcome.FAIL, world.isClientSide());
                }
                if (state.getValue(POT).hasPot()) {
                    if (!world.isClientSide()) {
                        player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.stove_has_pot")));
                    }
                    return Use.blockItem(UseOutcome.FAIL, world.isClientSide());
                }
                if (!world.isClientSide()) {
                    if (!stove.attachPot(pot.material(), HotPotBlock.handleAxisFor(player.getDirection()))) {
                        player.sendOverlayMessage(
                            Component.translatable(HotPotBlockEntity.key("msg.stove_has_pot")));
                        return Use.blockItem(UseOutcome.FAIL, world.isClientSide());
                    }
                    stove.loadPotContents(stack);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    world.playSound(null, pos, SoundEvents.ANVIL_LAND,
                        SoundSource.BLOCKS, 0.3F, 1.5F);
                }
                return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide());
            }
        }

        boolean potHit = isPotHit(state, pos, hit);
        boolean hasPot = state.getValue(POT).hasPot();
        boolean servingTool = HotPotInteraction.isServingTool(stack);
        boolean soupSource = Soups.fromStack(stack) != null;
        boolean ingredient = HotPotCooking.acceptable(world, stack);
        boolean spicyModifier = HotPotBlockEntity.isSpicyModifier(stack);
        boolean scoop = stove.canScoopWith(stack);
        boolean plate = HotPotBlock.isPlate(stack);

        if (player.isShiftKeyDown() && potHit && hasPot) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }

        if (!potHit || !hasPot || !(plate || scoop || servingTool || soupSource || ingredient || spicyModifier)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }

        return HotPotInteraction.useItem(stove, stack, world, pos, player, hand, hit,
            HotPotInteraction.STOVE_INNER_MIN, HotPotInteraction.STOVE_INNER_SIZE,
            new boolean[]{plate, scoop, servingTool, soupSource, spicyModifier, ingredient});
    }

    /** 手里这件东西是不是冲着锅去的（捞取工具、汤底、食材、调辣度的辣椒 / 冰块）。 */
    private static boolean potRelated(Level world, ItemStack stack) {
        return HotPotInteraction.isServingTool(stack)
            || Soups.fromStack(stack) != null
            || HotPotBlockEntity.isSpicyModifier(stack)
            || HotPotCooking.acceptable(world, stack);
    }

    /**
     * 判断这一下点的是锅还是灶台。
     *
     * 锅沉在槽里，锅腔（含空锅的内底）低于台面高度，只看 y 会把锅里的点击误判成灶膛，
     * 所以先看命中点是否落在锅沿投影范围内，再看它是否高过台面。
     */
    public static boolean isPotHit(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (!state.getValue(POT).hasPot()) {
            return false;
        }
        double localX = hit.getLocation().x - pos.getX();
        double localZ = hit.getLocation().z - pos.getZ();
        boolean inFootprint = localX >= POT_FOOTPRINT_MIN && localX <= POT_FOOTPRINT_MAX
            && localZ >= POT_FOOTPRINT_MIN && localZ <= POT_FOOTPRINT_MAX;
        return inFootprint || hit.getLocation().y - pos.getY() >= POT_HIT_MIN_Y + liftOf(state);
    }

    /**
     * 玩家准星是否落在灶上那口锅上（挖掘时用：挖锅只挖锅，挖灶身才挖整个灶台）。
     * 服务端和客户端用同一套射线判断，挖掘速度和最终结果才一致。
     */
    public static boolean aimsAtPot(BlockState state, BlockPos pos, Player player) {
        if (!isStove(state) || !state.getValue(POT).hasPot()) {
            return false;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        // 点在高灶台的「上半截」上也算点在这个灶台上（准星位置是一样的）
        BlockPos at = blockHit.getBlockPos();
        boolean here = at.equals(pos)
            || (at.equals(pos.above()) && player.level().getBlockState(at).is(HotPotContent.STOVE_TOP));
        return here && isPotHit(state, pos, blockHit);
    }

    /** mryt233 改：上下两格共用锅 / 灶身的名称判定，直接取描述键避免递归。 */
    static net.minecraft.network.chat.MutableComponent aimedName(Player player) {
        if (player != null && player.level() != null) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = blockHit.getBlockPos();
                BlockPos stovePos = player.level().getBlockState(hitPos).is(HotPotContent.STOVE_TOP)
                    ? hitPos.below() : hitPos;
                BlockState stoveState = player.level().getBlockState(stovePos);
                if (isStove(stoveState)) {
                    HotPotMaterial material = stoveState.getValue(POT).material();
                    if (material != null && isPotHit(stoveState, stovePos, blockHit)) {
                        return HotPotContent.blockFor(material).getName();
                    }
                    return Component.translatable(stoveState.getBlock().getDescriptionId());
                }
            }
        }
        return null;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        net.minecraft.network.chat.MutableComponent name = aimedName(PlateHolderBlock.pickPlayer.get());
        return name == null ? super.getName() : name;
    }

    /** 鼠标中键：对着锅复制锅，对着灶身复制灶台（2026-10-23 用户：锅和灶台是两个独立的东西）。 */
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        Player player = PlateHolderBlock.pickPlayer.get();
        HotPotMaterial material = state.getValue(POT).material();
        if (player != null && material != null && aimsAtPot(state, pos, player)) {
            // mryt233 改：两种模式均保留锅内组件，供指示器显示汤底；空锅不附加内容组件。
            if (world.getBlockEntity(pos) instanceof StoveBlockEntity stove) {
                return stove.toPotItem(material);
            }
            return new ItemStack(HotPotContent.blockFor(material));
        }
        return super.getCloneItemStack(world, pos, state, includeData);
    }

    /**
     * 对着锅挖时，用锅自己的硬度和工具要求（12 口锅统一 2.0 / 6.0、不强制工具，见
     * {@link HotPotBlock#settingsFor}）；对着灶身挖就按灶台自己的。
     * mryt233 改（2026-11-05）：以前铜锅照铜块、铁锅照铁块，铁锅空手挖极慢。
     */
    @Override
    protected float getDestroyProgress(BlockState state, Player player,
                                       BlockGetter world, BlockPos pos) {
        HotPotMaterial material = state.getValue(POT).material();
        if (material != null && aimsAtPot(state, pos, player)) {
            return HotPotContent.blockFor(material).defaultBlockState()
                .getDestroyProgress(player, world, pos);
        }
        return super.getDestroyProgress(state, player, world, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player != null
            && world.getBlockEntity(pos) instanceof StoveBlockEntity stove) {
            stove.markBreakHandled();
            if (!player.isCreative()) {
                stove.dropAttachedPot(state.getValue(POT).material());
                stove.dropContents();
                stove.dropFuel();
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, net.minecraft.core.Direction direction) {
        if (world.getBlockEntity(pos) instanceof StoveBlockEntity stove) {
            return state.getValue(POT).hasPot() ? stove.comparatorLevel() : stove.fuelComparatorLevel();
        }
        return 0;
    }

    private static HotPotBlock potOf(BlockItem item) {
        return item.getBlock() instanceof HotPotBlock pot ? pot : null;
    }
}
