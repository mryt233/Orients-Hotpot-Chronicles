package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 条凳：每种原版木板一种，一格长，有朝向（坐上去面朝 facing，放的时候就是玩家面朝的方向）。
 * 同朝向的条凳左右挨着会连成一条长凳：中间的凳腿去掉，凳面接成一整条。
 *
 * <p>坐：空手（或拿着不是方块的东西）右键。点在某一格中间就坐那一格中间；连成长凳后点在两格交界附近，
 * 就坐在两格之间。两个人之间至少隔一整格：比如三格长凳上有人坐在第 1、2 格之间，别人还能坐第 2、3 格之间
 * 或第 3 格中间，但不能坐第 2 格中间（只隔半格）。潜行起身，站到凳子后面。</p>
 *
 * <p>模型由 tools/gen_benches.py 生成，形状常量必须与它一致（朝北时长边沿 x，left = 西 / right = 东）。</p>
 */
public class BenchBlock extends Block implements net.minecraft.block.BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /** 坐着的人的左手边 / 右手边是否接着另一张同朝向的条凳。 */
    public static final BooleanProperty LEFT = BooleanProperty.of("left");
    public static final BooleanProperty RIGHT = BooleanProperty.of("right");

    /** 板凳坐面高度（凳面顶），与模型 y 5..7 一致。 */
    public static final double SEAT_Y = 7.0 / 16.0;
    /** 条凳（一格高）上玩家坐的高度。模型凳面顶在 16/16，但用户 r85 坐上去觉得桌子显高（）：
     *  只把坐的位置抬高 4 像素，凳子外观不变。 */
    public static final double TALL_SEAT_Y = 1.0 + 4.0 / 16.0;
    /** 条凳的腿：上端在凳面下 y=14、z 6.5 / 9.5，到地面各往外岔开 2 像素（≈8°），由 BenchLegsRenderer 画。 */
    public static final float LEG_TOP = 14F;
    public static final float LEG_SPREAD = 2F;
    /** 点在离交界这么近（按一格的比例）就算坐两格中间。 */
    private static final double BETWEEN_ZONE = 0.25;

    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape[]> TALL_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            VoxelShape[] tall = new VoxelShape[4];
            for (int i = 0; i < 4; i++) {
                boolean left = (i & 1) != 0;
                boolean right = (i & 2) != 0;
                boolean single = !left && !right;
                VoxelShape shape = rotated(new double[]{left ? 0 : 0.5, 14, 5, right ? 16 : 15.5, 16, 11}, facing);
                // 腿是斜的，碰撞箱用包住两条腿的竖直方块近似
                if (!left) {
                    double x0 = single ? 1.5 : 4.5;
                    shape = VoxelShapes.union(shape, rotated(new double[]{x0, 0, 4, x0 + 2, 14, 12}, facing));
                }
                if (!right) {
                    double x0 = single ? 12.5 : 9.5;
                    shape = VoxelShapes.union(shape, rotated(new double[]{x0, 0, 4, x0 + 2, 14, 12}, facing));
                }
                tall[i] = shape.simplify();
            }
            TALL_SHAPES.put(facing, tall);
            VoxelShape[] shapes = new VoxelShape[4];
            for (int i = 0; i < 4; i++) {
                boolean left = (i & 1) != 0;
                boolean right = (i & 2) != 0;
                List<double[]> boxes = new ArrayList<>();
                boxes.add(new double[]{left ? 0 : 0.5, 5, 4, right ? 16 : 15.5, 7, 12});
                if (!left) {
                    boxes.add(new double[]{1.5, 0, 4.5, 3.5, 5, 11.5});
                }
                if (!right) {
                    boxes.add(new double[]{12.5, 0, 4.5, 14.5, 5, 11.5});
                }
                VoxelShape shape = VoxelShapes.empty();
                for (double[] b : boxes) {
                    shape = VoxelShapes.union(shape, rotated(b, facing));
                }
                shapes[i] = shape.simplify();
            }
            SHAPES.put(facing, shapes);
        }
    }

    /** 把朝北的盒子按 facing 绕竖轴转过去（和 blockstate 的 y 旋转一致）。 */
    private static VoxelShape rotated(double[] b, Direction facing) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (facing) {
            case EAST -> Block.createCuboidShape(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.createCuboidShape(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.createCuboidShape(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.createCuboidShape(x0, y0, z0, x1, y1, z1);
        };
    }

    /** true = 一格高的条凳（配 1.5 格高火锅桌），false = 矮板凳。 */
    private final boolean tall;
    /** 用的哪种原版木板（条凳的斜腿按它的贴图画）。 */
    private final Block planks;

    public BenchBlock(Block planks) {
        this(planks, false);
    }

    public BenchBlock(Block planks, boolean tall) {
        super(AbstractBlock.Settings.create()
            .mapColor(planks.getDefaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sounds(planks.getDefaultState().getSoundGroup())
            .nonOpaque());
        setDefaultState(getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(LEFT, false)
            .with(RIGHT, false));
        this.tall = tall;
        this.planks = planks;
    }

    public Block planks() {
        return planks;
    }

    public boolean tall() {
        return tall;
    }

    /** 坐面高度（方块单位）。 */
    public double seatY() {
        return tall ? TALL_SEAT_Y : SEAT_Y;
    }

    /** 条凳有方块实体（用来画斜腿）；板凳没有。 */
    @Override
    public net.minecraft.block.entity.BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return tall ? new BenchBlockEntity(pos, state) : null;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    /** 坐着的人左手边的方向。 */
    public static Direction leftOf(Direction facing) {
        return facing.rotateYCounterclockwise();
    }

    /** 连成一条最长几格（2026-10-26 用户：条凳最长 3 格，超过就不再吸附）。 */
    public static final int MAX_LENGTH = 3;

    /**
     * 放下时决定连哪边：数一数左右两边已经连着的有几张，连上后整条不超过 {@link #MAX_LENGTH} 才连；
     * 两边都能连但加起来超了，就连能凑满的那一边（一样长连左边）。已经连好的凳子不会因为后来放的而改变。
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
        BlockView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        Direction left = leftOf(state.get(FACING));
        int l = runLength(world, pos.offset(left), left, state.get(FACING));
        int r = runLength(world, pos.offset(left.getOpposite()), left.getOpposite(), state.get(FACING));
        boolean joinL = l > 0, joinR = r > 0;
        if (1 + l + r > MAX_LENGTH) {
            boolean fitL = l > 0 && 1 + l <= MAX_LENGTH, fitR = r > 0 && 1 + r <= MAX_LENGTH;
            joinL = fitL && (!fitR || l >= r);
            joinR = fitR && !joinL;
        }
        return state.with(LEFT, joinL).with(RIGHT, joinR);
    }

    /** 邻居变了：一边还连着，要那一边的凳子也说连着我才算（谁连谁在放下时就定了）；那边没凳子了就断开。 */
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.get(FACING);
        Direction left = leftOf(facing);
        return state
            .with(LEFT, joinedTo(world.getBlockState(pos.offset(left)), left.getOpposite(), facing))
            .with(RIGHT, joinedTo(world.getBlockState(pos.offset(left.getOpposite())), left, facing));
    }

    /** 这张凳子在 dir 方向上连着别的凳子吗（按它自己的朝向换算成 LEFT / RIGHT）。 */
    private static boolean connected(BlockState state, Direction dir) {
        if (!(state.getBlock() instanceof BenchBlock)) {
            return false;
        }
        Direction left = leftOf(state.get(FACING));
        return dir == left ? state.get(LEFT) : dir == left.getOpposite() && state.get(RIGHT);
    }

    /** 那边的凳子能和我连、而且它在朝我的方向（towardMe）上说连着。 */
    private boolean joinedTo(BlockState neighbor, Direction towardMe, Direction facing) {
        return sameBench(neighbor, facing, tall) && connected(neighbor, towardMe);
    }

    /** 从 start 往 away 方向数：能和我连的、已经一张接一张连着的凳子有几张（start 不是凳子就是 0）。 */
    private int runLength(BlockView world, BlockPos start, Direction away, Direction facing) {
        int n = 0;
        BlockPos p = start;
        BlockState s = world.getBlockState(p);
        while (sameBench(s, facing, tall) && n <= MAX_LENGTH) {
            n++;
            if (!connected(s, away)) {
                break;
            }
            p = p.offset(away);
            s = world.getBlockState(p);
        }
        return n;
    }

    /**
     * 顺着同一条线摆、高矮一样的凳子连成一条（不同木头也连）。朝向相反也算（用户报：拆掉一头再放回去
     * 连不上——人站到凳子另一边放，朝向就反了；凳子前后对称，反着连看不出差别）。只有横竖不一样才不连。
     */
    public static boolean sameBench(BlockState state, Direction facing, boolean tall) {
        return state.getBlock() instanceof BenchBlock bench && bench.tall == tall
            && state.get(FACING).getAxis() == facing.getAxis();
    }

    private VoxelShape shapeOf(BlockState state) {
        int i = (state.get(LEFT) ? 1 : 0) | (state.get(RIGHT) ? 2 : 0);
        return (tall ? TALL_SHAPES : SHAPES).get(state.get(FACING))[i];
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(state);
    }

    // ------------------------------------------------------------------ 坐

    /** 拿着方块时照常放方块（方便接着摆条凳），其余情况交给 onUse 去坐。 */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        return stack.getItem() instanceof BlockItem
            ? ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION
            : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (player.isSneaking() || player.hasVehicle()) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        Direction facing = state.get(FACING);
        Direction right = leftOf(facing).getOpposite();
        // 命中点沿凳子长边（往右手边为正）落在这一格的哪里：0 = 左端，1 = 右端
        double along = hit.getPos().subtract(Vec3d.of(pos)).dotProduct(Vec3d.of(right.getVector()));
        if (right.getDirection() == Direction.AxisDirection.NEGATIVE) {
            along += 1.0;
        }
        List<BlockPos[]> choices = new ArrayList<>();
        if (along > 1 - BETWEEN_ZONE && state.get(RIGHT)) {
            choices.add(new BlockPos[]{pos, pos.offset(right)});
        } else if (along < BETWEEN_ZONE && state.get(LEFT)) {
            choices.add(new BlockPos[]{pos.offset(right.getOpposite()), pos});
        }
        choices.add(new BlockPos[]{pos});   // 交界那边被占了就退回坐这一格中间
        for (BlockPos[] seat : choices) {
            Vec3d at = seatPos(seat);
            if (!occupied(world, at)) {
                sit(world, player, seat, at, facing);
                return ActionResult.SUCCESS;
            }
        }
        player.sendMessage(Text.translatable(HotPotBlockEntity.key("msg.seat_taken")), true);
        return ActionResult.SUCCESS;
    }

    /** 座位点：一格中间，或两格中心的中点（交界处），高度在凳面上。 */
    private Vec3d seatPos(BlockPos[] seat) {
        Vec3d a = Vec3d.ofBottomCenter(seat[0]);
        Vec3d b = Vec3d.ofBottomCenter(seat[seat.length - 1]);
        return a.add(b).multiply(0.5).add(0, seatY(), 0);
    }

    /** 离这个座位不到一整格的地方已经有人坐着（同一条凳上的相邻座位相隔半格或一格）。 */
    private static boolean occupied(World world, Vec3d at) {
        Box area = new Box(at, at).expand(1.0, 0.5, 1.0);
        return !world.getEntitiesByClass(SeatEntity.class, area,
            e -> e.isAlive() && e.getPos().squaredDistanceTo(at) < 0.99 * 0.99).isEmpty();
    }

    private void sit(World world, PlayerEntity player, BlockPos[] seat, Vec3d at, Direction facing) {
        SeatEntity entity = SeatEntity.create(world, at, seat[0], seat[seat.length - 1], facing, seatY());
        world.spawnEntity(entity);
        player.startRiding(entity);
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
