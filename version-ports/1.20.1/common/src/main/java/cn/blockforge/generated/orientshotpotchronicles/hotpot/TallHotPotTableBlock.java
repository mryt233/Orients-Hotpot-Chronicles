package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.5 格高的火锅桌（配高灶台）：桌面板 y 19..23 铺满整格，下面 0..19 为可统一替换的底座区域；
 * 正对着灶膛口的那张桌子（{@link #OPEN}）去掉底墙只留上方围圈，站在外面能看到灶膛里的火。
 */
public class TallHotPotTableBlock extends HotPotTableBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static final double TOP_Y0 = 19.0;
    static final double TABLE_TOP = 23.0;
    private static final double BAND_Y0 = 16.0;
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    private static final VoxelShape[] OPEN_SHAPES = new VoxelShape[16];

    static {
        VoxelShape top = Block.box(0, TOP_Y0, 0, 16, TABLE_TOP, 16);
        for (int mask = 0; mask < 16; mask++) {
            double[] x = span(mask, 8, 2);
            double[] z = span(mask, 1, 4);
            SHAPES[mask] = Shapes.or(top,
                Block.box(x[0], 0, z[0], x[1], TOP_Y0, z[1])).optimize();
            OPEN_SHAPES[mask] = Shapes.or(top,
                Block.box(x[0], BAND_Y0, z[0], x[1], TOP_Y0, z[1])).optimize();
        }
    }

    public TallHotPotTableBlock(Block planks) {
        this(planks, false);
    }

    public TallHotPotTableBlock(Block like, boolean hardSurface) {
        this(like, hardSurface, true);
    }

    protected TallHotPotTableBlock(Block like, boolean hardSurface, boolean swappable) {
        super(like, hardSurface, swappable);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected double topThresholdPx() {
        return TOP_Y0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    protected BlockState refreshConnections(BlockState state, BlockGetter world, BlockPos pos) {
        boolean open = false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = world.getBlockState(pos.relative(d));
            if (StoveBlock.isStove(neighbor) && neighbor.getValue(StoveBlock.FACING) == d.getOpposite()) {
                open = true;
            }
        }
        return super.refreshConnections(state, world, pos).setValue(OPEN, open);
    }

    @Override
    protected VoxelShape shapeOf(BlockState state) {
        return (state.getValue(OPEN) ? OPEN_SHAPES : SHAPES)[mask(state)];
    }
}
