// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

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
public class BenchBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 坐着的人的左手边 / 右手边是否接着另一张同朝向的条凳。 */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    /** 板凳坐面高度（凳面顶），与模型 y 5..7 一致。 */
    public static final double SEAT_Y = 7.0 / 16.0;
    /** 条凳（一格高）上玩家坐的高度。模型凳面顶在 16/16，但用户 r85 坐上去觉得桌子显高：
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
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] tall = new VoxelShape[4];
            for (int i = 0; i < 4; i++) {
                boolean left = (i & 1) != 0;
                boolean right = (i & 2) != 0;
                boolean single = !left && !right;
                VoxelShape shape = rotated(new double[]{left ? 0 : 0.5, 14, 5, right ? 16 : 15.5, 16, 11}, facing);
                // mryt233 改：条凳腿碰撞箱与选中框收窄为 5..11，与凳面同宽，避免贴凳走动被卡住。
                if (!left) {
                    double x0 = single ? 1.5 : 4.5;
                    shape = Shapes.or(shape, rotated(new double[]{x0, 0, 5, x0 + 2, 14, 11}, facing));
                }
                if (!right) {
                    double x0 = single ? 12.5 : 9.5;
                    shape = Shapes.or(shape, rotated(new double[]{x0, 0, 5, x0 + 2, 14, 11}, facing));
                }
                tall[i] = shape.optimize();
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
                VoxelShape shape = Shapes.empty();
                for (double[] b : boxes) {
                    shape = Shapes.or(shape, rotated(b, facing));
                }
                shapes[i] = shape.optimize();
            }
            SHAPES.put(facing, shapes);
        }
    }

    /** 把朝北的盒子按 facing 绕竖轴转过去（和 blockstate 的 y 旋转一致）。 */
    private static VoxelShape rotated(double[] b, Direction facing) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (facing) {
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    /** true = 一格高的条凳（配 1.5 格高火锅桌），false = 矮板凳。 */
    private final boolean tall;
    /** 用的哪种原版木板（条凳的斜腿按它的贴图画）。 */
    private final Block planks;

    public BenchBlock(Block planks, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        this(planks, false, properties);
    }

    public BenchBlock(Block planks, boolean tall, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties
            .mapColor(planks.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(planks.defaultBlockState().getSoundType())
            .noOcclusion());
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(LEFT, false)
            .setValue(RIGHT, false));
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
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tall ? new BenchBlockEntity(pos, state) : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    /** 坐着的人左手边的方向。 */
    public static Direction leftOf(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** 连成一条最长几格（2026-10-26 用户：条凳最长 3 格，超过就不再吸附）。 */
    public static final int MAX_LENGTH = 3;
    /** 手持一格高条凳往上叠，整摞最多几格（2026-11-05 用户：一格高条凳搭起来当梯子用）。
     *  mryt233 改（2026-11-07）：上限按「整摞格数」算，含被点的那一格，和高凳那边统一成 16 格。 */
    public static final int MAX_STACK = 16;

    /** 这格方块是不是一格高的条凳（<wood>_bench，tall = true）。 */
    public static boolean isTallBench(BlockState state) {
        return state.getBlock() instanceof BenchBlock bench && bench.tall;
    }

    /**
     * 这格条凳是不是「已经上下搭起来了」（至少 2 层条凳叠在一起）：
     * 单张放在火锅桌边的条凳不触发爬梯子，免得围桌走动时不小心蹭上去；一旦上下搭了 2 层及以上，就能当梯子攀爬。
     */
    public static boolean isStackedTallBench(BlockGetter world, BlockPos pos) {
        if (!isTallBench(world.getBlockState(pos))) {
            return false;
        }
        return isTallBench(world.getBlockState(pos.above())) || isTallBench(world.getBlockState(pos.below()));
    }

    /**
     * 检查实体是否正贴着一摞已经叠起来的条凳（至少 2 层）：
     * 条凳前后方向（z 5..11）比满格内缩 5 像素，玩家贴着侧面走时中心会进格；左右两端（0.5..15.5）内缩 0.5 像素，
     * 水平扩 0.06 格即可让四面贴靠都能顺滑像爬梯子一样上下攀爬。
     *
     * <p>mryt233 改（2026-11-07）：① 站在凳面上（脚踩在条凳上）不算攀爬，免得单张凳旁边的叠塔把人黏住；
     * ② 用复用的 MutableBlockPos 探测，先单格判一次「是不是条凳」就跳过绝大多数空气格，命中后再查上下，
     * 不再每个空气格都查三遍、也不再产生大量临时 BlockPos。作者：mryt233</p>
     */
    public static BlockPos findClimbableStackedBench(net.minecraft.world.entity.LivingEntity entity) {
        Level world = entity.level();
        AABB box = entity.getBoundingBox();
        // 站在凳面上（脚底下那格是条凳）时不算在爬
        BlockPos feet = BlockPos.containing(entity.getX(), box.minY - 0.1, entity.getZ());
        if (entity.onGround() && isTallBench(world.getBlockState(feet))) {
            return null;
        }
        AABB probeBox = box.inflate(0.06, 0.0, 0.06);
        int y0 = net.minecraft.util.Mth.floor(probeBox.minY + 1.0E-3);
        int y1 = net.minecraft.util.Mth.floor(probeBox.maxY - 1.0E-3);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            for (int x = net.minecraft.util.Mth.floor(probeBox.minX); x <= net.minecraft.util.Mth.floor(probeBox.maxX); x++) {
                for (int z = net.minecraft.util.Mth.floor(probeBox.minZ); z <= net.minecraft.util.Mth.floor(probeBox.maxZ); z++) {
                    probe.set(x, y, z);
                    if (!isTallBench(world.getBlockState(probe))) {
                        continue;                        // 一次查询跳过绝大多数空气格
                    }
                    probe.move(Direction.UP);
                    boolean stacked = isTallBench(world.getBlockState(probe));
                    probe.move(Direction.DOWN);
                    if (!stacked) {
                        probe.move(Direction.DOWN);
                        stacked = isTallBench(world.getBlockState(probe));
                        probe.move(Direction.UP);
                    }
                    if (stacked) {
                        return probe.immutable();        // 这一格属于一摞 2 层以上的条凳
                    }
                }
            }
        }
        return null;
    }

    /**
     * 放下时决定连哪边：数一数左右两边已经连着的有几张，连上后整条不超过 {@link #MAX_LENGTH} 才连；
     * 两边都能连但加起来超了，就连能凑满的那一边（一样长连左边）。已经连好的凳子不会因为后来放的而改变。调整
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return placementStateAt(ctx.getLevel(), ctx.getClickedPos(), ctx.getHorizontalDirection());
    }

    /** 计算在 pos 处按 facing 朝向放这张凳子时的连接状态（正常放置和手持条凳往上叠共用）。 */
    public BlockState placementStateAt(BlockGetter world, BlockPos pos, Direction facing) {
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Direction left = leftOf(facing);
        int l = runLength(world, pos.relative(left), left, facing);
        int r = runLength(world, pos.relative(left.getOpposite()), left.getOpposite(), facing);
        boolean joinL = l > 0, joinR = r > 0;
        if (1 + l + r > MAX_LENGTH) {
            boolean fitL = l > 0 && 1 + l <= MAX_LENGTH, fitR = r > 0 && 1 + r <= MAX_LENGTH;
            joinL = fitL && (!fitR || l >= r);
            joinR = fitR && !joinL;
        }
        return state.setValue(LEFT, joinL).setValue(RIGHT, joinR);
    }

    /** 直接往另一张条凳顶面放条凳时，同样触发「步步高升」成就。 */
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide() && this.tall && placer instanceof Player player
                && isTallBench(world.getBlockState(pos.below()))) {
            HotPotCriterion.fire(player, "tall_stool");
        }
    }

    /** 邻居变了：一边还连着，要那一边的凳子也说连着我才算（谁连谁在放下时就定了）；那边没凳子了就断开。 */
    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader world, net.minecraft.world.level.ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        Direction facing = state.getValue(FACING);
        Direction left = leftOf(facing);
        return state
            .setValue(LEFT, joinedTo(world.getBlockState(pos.relative(left)), left.getOpposite(), facing))
            .setValue(RIGHT, joinedTo(world.getBlockState(pos.relative(left.getOpposite())), left, facing));
    }

    /**
     * 条凳被拆、被活塞推走、被炸掉时，把落在这一格上的座位一起清掉（坐着的人当场被放下）。
     * mryt233 改（2026-11-07）：以前条凳没有 onRemove，只能等座位实体下一 tick 自己发现凳子没了，
     * 这一 tick 里人会坐在一张已经不存在的凳子上。
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel world, BlockPos pos, boolean moved) {
        // mryt233 改（26.1）：26.x 把 onRemove 换成 affectNeighborsAfterRemoval，语义相同（方块被替换 / 移除时调）。
        clearSeatsAt(world, pos);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    /** 清掉落在 pos 这一格（含两格交界）上的座位实体。mryt233 改（2026-11-07）新增。作者：mryt233 */
    public static void clearSeatsAt(Level world, BlockPos pos) {
        Vec3 center = Vec3.atBottomCenterOf(pos);
        AABB area = new AABB(center, center).inflate(1.5, 2.0, 1.5);
        for (SeatEntity seat : world.getEntitiesOfClass(SeatEntity.class, area, Entity::isAlive)) {
            if (seat.sitsOn(pos)) {
                seat.discard();
            }
        }
    }

    /** 这张凳子在 dir 方向上连着别的凳子吗（按它自己的朝向换算成 LEFT / RIGHT）。 */
    private static boolean connected(BlockState state, Direction dir) {
        if (!(state.getBlock() instanceof BenchBlock)) {
            return false;
        }
        Direction left = leftOf(state.getValue(FACING));
        return dir == left ? state.getValue(LEFT) : dir == left.getOpposite() && state.getValue(RIGHT);
    }

    /** 那边的凳子能和我连、而且它在朝我的方向（towardMe）上说连着。 */
    private boolean joinedTo(BlockState neighbor, Direction towardMe, Direction facing) {
        return sameBench(neighbor, facing, tall) && connected(neighbor, towardMe);
    }

    /** 从 start 往 away 方向数：能和我连的、已经一张接一张连着的凳子有几张（start 不是凳子就是 0）。 */
    private int runLength(BlockGetter world, BlockPos start, Direction away, Direction facing) {
        int n = 0;
        BlockPos p = start;
        BlockState s = world.getBlockState(p);
        while (sameBench(s, facing, tall) && n <= MAX_LENGTH) {
            n++;
            if (!connected(s, away)) {
                break;
            }
            p = p.relative(away);
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
            && state.getValue(FACING).getAxis() == facing.getAxis();
    }

    private VoxelShape shapeOf(BlockState state) {
        int i = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0);
        return (tall ? TALL_SHAPES : SHAPES).get(state.getValue(FACING))[i];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    // ------------------------------------------------------------------ 往上叠 & 坐

    /**
     * 手里拿着一格高条凳右键已放好的一格高条凳（不潜行时）：顺着这一摞往上找，直接叠到最顶上（最多 16 格），
     * 叠好后就能当梯子爬；潜行或拿别的方块时照常按原版放置，空手或拿非方块物品交给 useWithoutItem 去坐。
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.tall && !player.isShiftKeyDown()
                && stack.getItem() instanceof BlockItem held
                && held.getBlock() instanceof BenchBlock heldBench && heldBench.tall) {
            BlockPos target = findStackTop(world, pos);
            if (target != null) {
                if (!world.isClientSide()) {
                    // mryt233 改（2026-11-07）：叠之前先看目标格和这一摞最顶上——别把人顶出座位、别把方块塞进生物身体里
                    if (!stackSpotFree(world, target, player)) {
                        player.sendOverlayMessage(
                            Component.translatable(HotPotBlockEntity.key("msg.seat_taken")));
                        return InteractionResult.FAIL;
                    }
                    BlockState placed = heldBench.placementStateAt(world, target, player.getDirection());
                    world.setBlock(target, placed, Block.UPDATE_ALL);
                    world.playSound(null, target,
                        heldBench.planks().defaultBlockState().getSoundType().getPlaceSound(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (!player.getAbilities().instabuild) {
                        stack.consume(1, player);
                    }
                    HotPotCriterion.fire(player, "tall_stool");
                }
                return InteractionResult.SUCCESS;
            }
        }
        return stack.getItem() instanceof BlockItem
            ? InteractionResult.PASS
            : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * 从被点的这一格往上找，返回这一摞顶上能放新凳的那一格。
     * 被点的 pos 算第 1 层，整摞叠完不允许超过 {@link #MAX_STACK}；出世界高度、顶上放不下都返回 null。
     * mryt233 改（2026-11-07）：以前从 pos 上面一格起最多走 MAX_STACK 步，整摞实际能到 18 格。
     * 另外被点的那一格可能在这摞中间或顶上（叠高了以后玩家多半点最上面那张），光从它往上数永远只数到 1 层，
     * 所以先往下走到这一摞的最底下，再从底下往上数满整摞。
     */
    private static BlockPos findStackTop(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        int steps = 0;                                   // 只用来防死循环，不参与层数
        while (steps++ < MAX_STACK && isTallBench(world.getBlockState(cursor.below()))) {
            cursor.move(Direction.DOWN);
        }
        // 这时 cursor 已经在整摞最底下，从最底下往上数整摞有几层（含它自己）
        int layers = 1;
        while (layers < MAX_STACK && isTallBench(world.getBlockState(cursor.above()))) {
            cursor.move(Direction.UP);
            layers++;
        }
        if (layers >= MAX_STACK) {
            return null;                                 // 已经是一摞 16 格，再叠就超上限
        }
        BlockPos target = cursor.above().immutable();
        if (world.isOutsideBuildHeight(target)) {
            return null;
        }
        return world.getBlockState(target).canBeReplaced() ? target : null;
    }

    /**
     * 叠凳子之前先看一眼：目标格会不会卡在生物身上；这一摞最顶上有没有人正坐着
     * （叠上去座位会被迫丢弃，人会从半空摔下来）。
     * mryt233 改（2026-11-07）：放在 BenchBlock 是为了不新增公共工具类，也让条凳和高凳共用同一份判断。
     * 作者：mryt233
     */
    public static boolean stackSpotFree(Level world, BlockPos target, Player placer) {
        AABB area = new AABB(target).inflate(0.05);
        // 目标格里已经站了别的生物（放凳子的人自己不算，座位实体本身也不算——它们由下面那条判）
        if (!world.getEntitiesOfClass(Entity.class, area,
                e -> e != placer && e.isAlive() && !(e instanceof SeatEntity)).isEmpty()) {
            return false;
        }
        Vec3 top = Vec3.atBottomCenterOf(target);
        for (SeatEntity seat : world.getEntitiesOfClass(SeatEntity.class,
                new AABB(top, top).inflate(1.0, 2.0, 1.0), Entity::isAlive)) {
            if (seat.isVehicle()) {
                return false;                            // 有人正坐在这摞最上面那张，别往人家头上压凳子
            }
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        // 凳塔中间那几层（头顶还叠着另一张条凳）不坐人，只坐最顶上或单张条凳
        if (this.tall && isTallBench(world.getBlockState(pos.above()))) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction facing = state.getValue(FACING);
        Direction right = leftOf(facing).getOpposite();
        // 命中点沿凳子长边（往右手边为正）落在这一格的哪里：0 = 左端，1 = 右端
        double along = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)).dot(Vec3.atLowerCornerOf(right.getUnitVec3i()));
        if (right.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            along += 1.0;
        }
        List<BlockPos[]> choices = new ArrayList<>();
        if (along > 1 - BETWEEN_ZONE && state.getValue(RIGHT)) {
            choices.add(new BlockPos[]{pos, pos.relative(right)});
        } else if (along < BETWEEN_ZONE && state.getValue(LEFT)) {
            choices.add(new BlockPos[]{pos.relative(right.getOpposite()), pos});
        }
        choices.add(new BlockPos[]{pos});   // 交界那边被占了就退回坐这一格中间
        for (BlockPos[] seat : choices) {
            Vec3 at = seatPos(seat);
            if (!occupied(world, at)) {
                sit(world, player, seat, at, facing);
                return InteractionResult.SUCCESS;
            }
        }
        player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.seat_taken")));
        return InteractionResult.SUCCESS;
    }

    /** 座位点：一格中间，或两格中心的中点（交界处），高度在凳面上。 */
    private Vec3 seatPos(BlockPos[] seat) {
        Vec3 a = Vec3.atBottomCenterOf(seat[0]);
        Vec3 b = Vec3.atBottomCenterOf(seat[seat.length - 1]);
        return a.add(b).scale(0.5).add(0, seatY(), 0);
    }

    /** 离这个座位不到一整格的地方已经有人坐着（同一条凳上的相邻座位相隔半格或一格）。 */
    private static boolean occupied(Level world, Vec3 at) {
        AABB area = new AABB(at, at).inflate(1.0, 0.5, 1.0);
        return !world.getEntitiesOfClass(SeatEntity.class, area,
            e -> e.isAlive() && e.position().distanceToSqr(at) < 0.99 * 0.99).isEmpty();
    }

    private void sit(Level world, Player player, BlockPos[] seat, Vec3 at, Direction facing) {
        SeatEntity entity = SeatEntity.create(world, at, seat[0], seat[seat.length - 1], facing, seatY());
        world.addFreshEntity(entity);
        player.startRiding(entity);
    }
}
