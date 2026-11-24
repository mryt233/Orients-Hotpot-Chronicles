// mryt233 改：26.1 原版 API（保留动态换材质方块实体与旧预设兼容构造）。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 火锅桌：统一的桌架融合架构基础方块（支持桌面 / 底座分部位动态换材质并兼容第三方方块）。
 *
 * <p>桌面板 y 7..11（与灶台台面齐平）永远铺满整格；桌身 y 0..7 朝外一侧收进去 4 像素。
 * 任意可连接火锅桌和灶台相连；全桌统一不可燃，挖掘速度和破坏粒子 / 声音随当前材质走。</p>
 */
public class HotPotTableBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    // 与 tools/gen_tables.py 一一对应。
    private static final double TOP_Y0 = 7.0;
    static final double TABLE_TOP = 11.0;       // 与灶台台面齐平
    /** 桌身朝外一侧收进去的像素（与 gen_tables.py 的 RECESS 一致）。 */
    private static final double RECESS = 4.0;

    /** 16 种连接组合的碰撞/描边形状，下标就是四向位掩码。 */
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        VoxelShape top = Block.box(0, TOP_Y0, 0, 16, TABLE_TOP, 16);
        for (int mask = 0; mask < 16; mask++) {
            double[] x = span(mask, 8, 2);
            double[] z = span(mask, 1, 4);
            VoxelShape base = Block.box(x[0], 0, z[0], x[1], TOP_Y0, z[1]);
            SHAPES[mask] = Shapes.or(top, base).optimize();
        }
    }

    private final boolean swappable;

    /** 一个轴上桌身的范围：接邻一侧到边，朝外一侧收进去 RECESS。 */
    protected static double[] span(int mask, int lowBit, int highBit) {
        boolean low = (mask & lowBit) != 0;
        boolean high = (mask & highBit) != 0;
        double lo = low ? 0 : RECESS;
        double hi = high ? 16 : 16 - RECESS;
        return new double[]{lo, hi};
    }

    public HotPotTableBlock(Block planks, BlockBehaviour.Properties properties) {
        this(planks, false, true, properties);
    }

    public HotPotTableBlock(Block like, boolean hardSurface, BlockBehaviour.Properties properties) {
        this(like, hardSurface, true, properties);
    }

    /** mryt233 改（2026-11-16，0.3.1 Phase 4）：统一不可燃、固定硬度 2.0，支持区分通用桌（挂方块实体）与旧预设兼容载体。 */
    protected HotPotTableBlock(Block like, boolean hardSurface, boolean swappable, BlockBehaviour.Properties properties) {
        super(properties
            .mapColor(like.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, hardSurface ? 6.0F : 3.0F)
            .sound(like.defaultBlockState().getSoundType())
            .noOcclusion());
        this.swappable = swappable;
        registerDefaultState(defaultBlockState()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false));
    }

    public boolean isSwappable() {
        return swappable;
    }

    protected double topThresholdPx() {
        return TOP_Y0;
    }

    public TableMaterials.Slot slotAt(BlockPos pos, Vec3 hitLocation) {
        double localY = hitLocation.y - pos.getY();
        return localY >= (topThresholdPx() / 16.0) - 1.0e-4
            ? TableMaterials.Slot.TOP
            : TableMaterials.Slot.BASE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return swappable ? new HotPotTableBlockEntity(pos, state) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return refreshConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof HotPotTableBlockEntity be) {
            TableMaterialData data = be.materials();
            if (data != null && !data.top().isEmpty()) {
                BlockState topState = be.topBlockState();
                world.playSound(null, pos, topState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        return refreshConnections(state, world, pos);
    }

    protected BlockState refreshConnections(BlockState state, BlockGetter world, BlockPos pos) {
        return state
            .setValue(NORTH, linksTo(world.getBlockState(pos.north())))
            .setValue(EAST, linksTo(world.getBlockState(pos.east())))
            .setValue(SOUTH, linksTo(world.getBlockState(pos.south())))
            .setValue(WEST, linksTo(world.getBlockState(pos.west())));
    }

    /** 任意火锅桌和灶台都算同一张桌子；接上的一侧桌身铺到边，不收进去。 */
    protected static boolean linksTo(BlockState neighbor) {
        return neighbor.getBlock() instanceof HotPotTableBlock || StoveBlock.isStove(neighbor);
    }

    protected static int mask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
            | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }

    /** mryt233 改（2026-11-19）：粒子只从命中的桌面/底座区域生成，复用原有分界与形状。 */
    public VoxelShape particlePartShape(BlockState state, TableMaterials.Slot slot) {
        double split = topThresholdPx();
        VoxelShape region = slot == TableMaterials.Slot.TOP
            ? Block.box(0, split, 0, 16, 32, 16) : Block.box(0, -16, 0, 16, split, 16);
        return Shapes.join(shapeOf(state), region, net.minecraft.world.phys.shapes.BooleanOp.AND);
    }

    protected VoxelShape shapeOf(BlockState state) {
        return SHAPES[mask(state)];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                                  CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
                                           CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.or(shapeOf(state), Block.box(0, 7, 0, 16, 16, 16));
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        BlockState matState = activeMiningState(world, pos);
        return TableMaterials.destroyProgress(matState, player, world, pos);
    }

    private BlockState activeMiningState(BlockGetter world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof HotPotTableBlockEntity be) {
            TableMaterialData data = be.materials();
            if (!data.top().isEmpty()) {
                return TableMaterials.resolveDisplayState(data.top(), Blocks.OAK_PLANKS.defaultBlockState());
            }
            if (!data.base().isEmpty()) {
                return TableMaterials.resolveDisplayState(data.base(), Blocks.BRICKS.defaultBlockState());
            }
        }
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        BlockState matState = activeMiningState(level, pos);
        level.levelEvent(player, 2001, pos, Block.getId(matState));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(this instanceof TallHotPotTableBlock
            ? HotPotContent.TALL_HOTPOT_TABLE_ITEM : HotPotContent.HOTPOT_TABLE_ITEM);
        if (world.getBlockEntity(pos) instanceof HotPotTableBlockEntity be && be.materials().hasAny()) {
            stack.set(HotPotContent.TABLE_MATERIALS, be.materials());
        } else if (this instanceof TableMaterials.LegacyTableFlag) {
            TableMaterials.Preset preset = TableMaterials.legacyPreset(this);
            if (preset != null) {
                stack.set(HotPotContent.TABLE_MATERIALS,
                    TableMaterialData.fromLegacyPreset(preset, this instanceof TallHotPotTableBlock));
            }
        }
        return stack;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSecondaryUseActive() || TableObjects.item(stack) != null
            || !(stack.getItem() instanceof BlockItem blockItem)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        Block targetBlock = blockItem.getBlock();
        BlockState candidate = targetBlock.defaultBlockState();
        if (candidate.is(TableDecor.TABLE_DECORATIONS) || !TableMaterials.accepts(candidate)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        if (this instanceof TableMaterials.LegacyTableFlag) {
            if (!world.isClientSide()) {
                TableMigration.upgradeBlock(world, pos, state);
            }
        }
        if (world.getBlockEntity(pos) instanceof HotPotTableBlockEntity be) {
            TableMaterials.Slot slot = slotAt(pos, hit.getLocation());
            if (be.applyMaterial(player, hand, stack, slot, targetBlock)) {
                return Use.blockItem(UseOutcome.SUCCESS_SOFT, world.isClientSide());
            }
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
        }
        if (this instanceof TableMaterials.LegacyTableFlag) {
            if (!world.isClientSide()) {
                TableMigration.upgradeBlock(world, pos, state);
            }
        }
        if (world.getBlockEntity(pos) instanceof HotPotTableBlockEntity be) {
            TableMaterials.Slot slot = slotAt(pos, hit.getLocation());
            if (be.tryPopMaterial(player, slot)) {
                return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide());
            }
        }
        return Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
    }
}
