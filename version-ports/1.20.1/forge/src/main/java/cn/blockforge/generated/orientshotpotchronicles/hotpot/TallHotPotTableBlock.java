package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * 1.5 格高的火锅桌（配高灶台）：下面一格高是原版砖块、上面半格是木板；桌面板 20..24 铺满整格，
 * 下面 0..20 朝外一侧收进 4 像素。正对着灶膛口的那张桌子（{@link #OPEN}）去掉砖身只留木头部分，
 * 站在外面能看到灶膛里的火。模型由 tools/gen_tall_preview.py 生成，形状常量要和它一致。
 */
public class TallHotPotTableBlock extends HotPotTableBlock {
    /** 旁边有灶台的火口正对着这张桌子。 */
    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    // 桌面板 19..23（r83 是 20..24，r84 用户说再低一点，降 1；调整），与 tools/gen_tall.py 的 TOP0 / TOP1 一致
    private static final double TOP_Y0 = 19.0;
    static final double TABLE_TOP = 23.0;
    private static final double BAND_Y0 = 16.0;
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    private static final VoxelShape[] OPEN_SHAPES = new VoxelShape[16];

    static {
        VoxelShape top = Block.createCuboidShape(0, TOP_Y0, 0, 16, TABLE_TOP, 16);
        for (int mask = 0; mask < 16; mask++) {
            double[] x = span(mask, 8, 2);
            double[] z = span(mask, 1, 4);
            SHAPES[mask] = VoxelShapes.union(top,
                Block.createCuboidShape(x[0], 0, z[0], x[1], TOP_Y0, z[1])).simplify();
            OPEN_SHAPES[mask] = VoxelShapes.union(top,
                Block.createCuboidShape(x[0], BAND_Y0, z[0], x[1], TOP_Y0, z[1])).simplify();
        }
    }

    public TallHotPotTableBlock(Block planks) {
        super(planks);
        setDefaultState(getDefaultState().with(OPEN, false));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPEN);
    }

    @Override
    public BlockState refreshConnections(BlockState state, BlockView world, BlockPos pos) {
        boolean open = false;
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockState neighbor = world.getBlockState(pos.offset(d));
            // 灶台的 FACING 就是火口朝向；火口朝着这张桌子 = 它的 FACING 指回来
            if (StoveBlock.isStove(neighbor) && neighbor.get(StoveBlock.FACING) == d.getOpposite()) {
                open = true;
            }
        }
        return super.refreshConnections(state, world, pos).with(OPEN, open);
    }

    @Override
    public VoxelShape shapeOf(BlockState state) {
        return (state.get(OPEN) ? OPEN_SHAPES : SHAPES)[mask(state)];
    }
}
