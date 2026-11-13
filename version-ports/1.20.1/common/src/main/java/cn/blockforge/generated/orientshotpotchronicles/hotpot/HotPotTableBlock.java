package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    /** 一个轴上桌身的范围：接邻一侧到边，朝外一侧收进去 RECESS。 */
    protected static double[] span(int mask, int lowBit, int highBit) {
        boolean low = (mask & lowBit) != 0;
        boolean high = (mask & highBit) != 0;
        double lo = low ? 0 : RECESS;
        double hi = high ? 16 : 16 - RECESS;
        return new double[]{lo, hi};
    }

    public HotPotTableBlock(Block planks) {
        this(planks, false);
    }

    public HotPotTableBlock(Block like, boolean hardSurface) {
        super(BlockBehaviour.Properties.of()
            .mapColor(like.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, hardSurface ? 6.0F : 3.0F)
            .sound(like.defaultBlockState().getSoundType())
            .noOcclusion());
        registerDefaultState(defaultBlockState()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false));
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return refreshConnections(state, world, pos);
    }

    protected BlockState refreshConnections(BlockState state, BlockGetter world, BlockPos pos) {
        return state
            .setValue(NORTH, linksTo(world.getBlockState(pos.north())))
            .setValue(EAST, linksTo(world.getBlockState(pos.east())))
            .setValue(SOUTH, linksTo(world.getBlockState(pos.south())))
            .setValue(WEST, linksTo(world.getBlockState(pos.west())));
    }

    /** 任意木头的火锅桌和灶台都算同一张桌子；接上的一侧桌身铺到边，不收进去。 */
    protected static boolean linksTo(BlockState neighbor) {
        return neighbor.getBlock() instanceof HotPotTableBlock || StoveBlock.isStove(neighbor);
    }

    protected static int mask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
            | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }

    protected VoxelShape shapeOf(BlockState state) {
        return SHAPES[mask(state)];
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                               CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
                                        CollisionContext context) {
        return shapeOf(state);
    }

    /** mryt233 改：旧版 Forge 读取方块扩展；数值与底本的原版火表一致。 */
    public boolean isFlammable(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return FireRules.flammable(this);
    }

    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return isFlammable(state, world, pos, face) ? 20 : 0;
    }

    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return isFlammable(state, world, pos, face) ? 5 : 0;
    }
}
