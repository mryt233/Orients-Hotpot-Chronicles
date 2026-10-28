package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.Instrument;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * 火锅桌：每种原版木板一种，围着灶台拼成 3x3 的大桌。
 *
 * <p>桌面板 y 7..11（与灶台台面齐平）永远铺满整格，拼起来是一整张桌面；桌身 y 0..7 像倒放的楼梯：
 * 朝外（没接桌子/灶台）的一侧收进去 4 像素，留出放腿的空间，接着桌子/灶台的一侧铺到方块边界。</p>
 *
 * <p>模型由 tools/gen_tables.py 生成，形状常量和连接位掩码（北 1 / 东 2 / 南 4 / 西 8）必须与它一致。
 * 不同木头的桌子互相也算连着。</p>
 */
public class HotPotTableBlock extends Block {
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;

    // 与 tools/gen_tables.py 一一对应。
    private static final double TOP_Y0 = 7.0;
    static final double TABLE_TOP = 11.0;       // 与灶台台面齐平
    /** 桌身朝外一侧收进去的像素（与 gen_tables.py 的 RECESS 一致）。 */
    private static final double RECESS = 4.0;

    /** 16 种连接组合的碰撞/描边形状，下标就是四向位掩码。 */
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        VoxelShape top = Block.createCuboidShape(0, TOP_Y0, 0, 16, TABLE_TOP, 16);
        for (int mask = 0; mask < 16; mask++) {
            double[] x = span(mask, 8, 2);
            double[] z = span(mask, 1, 4);
            VoxelShape base = Block.createCuboidShape(x[0], 0, z[0], x[1], TOP_Y0, z[1]);
            SHAPES[mask] = VoxelShapes.union(top, base).simplify();
        }
    }

    /** 一个轴上桌身的范围：接邻一侧到边，朝外一侧收进去 RECESS。 */
    protected static double[] span(int mask, int lowBit, int highBit) {
        boolean low = (mask & lowBit) != 0;
        boolean high = (mask & highBit) != 0;
        double lo = low ? 0 : RECESS;
        double hi = high ? 16 : 16 - RECESS;
        return new double[]{lo, hi};
    }

    private final Block planks;

    public HotPotTableBlock(Block planks) {
        super(AbstractBlock.Settings.create()
            .mapColor(planks.getDefaultMapColor())
            .instrument(Instrument.BASS)
            .strength(2.0F, 3.0F)
            .sounds(planks.getDefaultState().getSoundGroup())
            .nonOpaque());
        this.planks = planks;
        setDefaultState(getDefaultState()
            .with(NORTH, false)
            .with(EAST, false)
            .with(SOUTH, false)
            .with(WEST, false));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return refreshConnections(getDefaultState(), ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return refreshConnections(state, world, pos);
    }

    protected BlockState refreshConnections(BlockState state, BlockView world, BlockPos pos) {
        return state
            .with(NORTH, linksTo(world.getBlockState(pos.north())))
            .with(EAST, linksTo(world.getBlockState(pos.east())))
            .with(SOUTH, linksTo(world.getBlockState(pos.south())))
            .with(WEST, linksTo(world.getBlockState(pos.west())));
    }

    /** 任意木头的火锅桌和灶台都算同一张桌子；接上的一侧桌身铺到边，不收进去。 */
    protected static boolean linksTo(BlockState neighbor) {
        return neighbor.getBlock() instanceof HotPotTableBlock || StoveBlock.isStove(neighbor);
    }

    protected static int mask(BlockState state) {
        return (state.get(NORTH) ? 1 : 0) | (state.get(EAST) ? 2 : 0)
            | (state.get(SOUTH) ? 4 : 0) | (state.get(WEST) ? 8 : 0);
    }

    protected VoxelShape shapeOf(BlockState state) {
        return SHAPES[mask(state)];
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        return shapeOf(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return shapeOf(state);
    }
    // ：NeoForge 可燃接口；下界木不燃烧，原数值 5 / 20。
    @Override
    public boolean isFlammable(BlockState state, BlockView world, BlockPos pos, Direction face) {
        return planks != net.minecraft.block.Blocks.CRIMSON_PLANKS && planks != net.minecraft.block.Blocks.WARPED_PLANKS;
    }

    @Override
    public int getFlammability(BlockState state, BlockView world, BlockPos pos, Direction face) {
        return isFlammable(state, world, pos, face) ? 5 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockView world, BlockPos pos, Direction face) {
        return isFlammable(state, world, pos, face) ? 20 : 0;
    }

}
