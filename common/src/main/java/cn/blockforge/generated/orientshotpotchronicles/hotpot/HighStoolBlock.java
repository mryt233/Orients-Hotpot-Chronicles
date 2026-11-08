package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 木制高凳：11 种原版木板各一张，一格高，凳面在 14..16 像素，收腰造型、四腿加四面两层踏脚横撑。
 *
 * <p>三个交互：</p>
 * <ul>
 *   <li>手里拿着高凳右键另一张高凳：顺着这一摞往上找，自动叠到最顶上（最多 16 格），可以一级级叠成凳塔。</li>
 *   <li>全部 11 种都写进 {@code #minecraft:climbable}（由 tools/gen_high_stools.py 生成标签文件）：
 *       碰撞箱比方块边界内缩，玩家贴着凳塔侧面就能像爬梯子一样往上攀。</li>
 *   <li>空手右键坐上去（这一格上面没有别的凳子时才行），座位用的是本模组的 {@link SeatEntity}。</li>
 * </ul>
 *
 * <p>模型和方块状态由 tools/gen_high_stools.py 生成，{@link #SHAPE} 必须和脚本里的凳面、凳腿尺寸一致
 * （凳面 3..13 / 14..16，腿与横撑包络 3.5..12.5 / 0..14）。作者：mryt233 */
public class HighStoolBlock extends Block {
    /** 放置时朝向玩家面朝的方向。 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 正上方这一格是不是也有一张高凳（叠起来的时候 true）。 */
    public static final BooleanProperty STACKED = BooleanProperty.create("stacked");

    /** 11 种高凳方块，顺序同 {@link HotPotContent#TABLE_WOODS}。 */
    public static final List<HighStoolBlock> HIGH_STOOLS = new ArrayList<>();
    /** 对应的 11 个方块物品（注册在 {@link #registerAll()}，创造栏见 HotPotContent.fillCreativeTab）。 */
    public static final List<BlockItem> HIGH_STOOL_ITEMS = new ArrayList<>();

    /** 坐上去的高度（方块单位）：和高条凳一样在凳面上再抬 4 像素，坐着不会陷进凳面。 */
    public static final double SEAT_Y = BenchBlock.TALL_SEAT_Y;

    /** 手持高凳往上叠，整摞最多几格（含被点的那一格，2026-11-07 与条凳统一成 16 格）。 */
    public static final int MAX_STACK = 16;

    /** 凳面 + 凳腿 / 横撑的包络，和生成脚本里的元素尺寸一一对应。 */
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(3.0, 14.0, 3.0, 13.0, 16.0, 13.0),
        Block.box(3.5, 0.0, 3.5, 12.5, 14.0, 12.5)).optimize();

    /** 取声音、地图颜色、放置音效用的原版木板方块。 */
    private final Block planks;

    public HighStoolBlock(Block planks) {
        super(BlockBehaviour.Properties.of()
            .mapColor(planks.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(planks.defaultBlockState().getSoundType())
            .noOcclusion());
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(STACKED, false));
        this.planks = planks;
    }

    /** 注册 11 种高凳方块和物品（幂等：已经注册过就直接返回）。 */
    public static void registerAll() {
        if (!HIGH_STOOLS.isEmpty()) {
            return;
        }
        for (String wood : HotPotContent.TABLE_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(wood + "_planks"));
            HighStoolBlock stool = Framework.platform().registration().blocks().register(
                HotPot.id(wood + "_high_stool"), new HighStoolBlock(planks));
            HIGH_STOOLS.add(stool);
            HIGH_STOOL_ITEMS.add(Framework.platform().registration().items().register(
                HotPot.id(wood + "_high_stool"), new BlockItem(stool, new Item.Properties())));
        }
    }

    /** 这张高凳用的原版木板。 */
    public Block planks() {
        return planks;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STACKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        return defaultBlockState()
            .setValue(FACING, ctx.getHorizontalDirection())
            .setValue(STACKED, ctx.getLevel().getBlockState(pos.above()).getBlock() instanceof HighStoolBlock);
    }

    /** 上面那一格变了就重算 STACKED（往上叠、拆掉都会走到这里）。 */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(STACKED, neighborState.getBlock() instanceof HighStoolBlock);
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,
                                           CollisionContext context) {
        return SHAPE;
    }

    // ------------------------------------------------------------------ 手持高凳往上叠

    /**
     * 手里拿着高凳右键已放好的高凳：顺着这一摞往上找，叠到最顶上那张上面；潜行时不叠，照常往侧面放，
     * 方便挨着摆一排。拿别的方块照常放置（交给原版），空手或拿不是方块的东西交给 {@link #useWithoutItem} 去坐。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem held) {
            if (held.getBlock() instanceof HighStoolBlock stool && !player.isShiftKeyDown()) {
                BlockPos target = stackTarget(world, pos);
                if (target != null) {
                    if (!world.isClientSide) {
                        // mryt233 改（2026-11-07）：叠之前先看目标格和这一摞最顶上，别把坐着的人顶出座位、
                        // 别把方块塞进生物身体里。判断写在 BenchBlock 里，条凳和高凳共用同一份，也免得新建公共工具类
                        if (!BenchBlock.stackSpotFree(world, target, player)) {
                            player.displayClientMessage(
                                Component.translatable(HotPotBlockEntity.key("msg.seat_taken")), true);
                            return ItemInteractionResult.FAIL;
                        }
                        placeOnTop(world, target, stool, player, stack);
                    }
                    return ItemInteractionResult.sidedSuccess(world.isClientSide);
                }
            }
            // 手里是方块：顶到上限 / 顶上放不下 / 潜行 → 交给原版照常往旁边放，不要掉进「坐」那条路
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        // 空手（或拿着不是方块的东西）→ 原版接着问 useWithoutItem，去坐
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 从被点的这一格往上数，返回这一摞顶上空着、能放高凳的那一格；放不下返回 null。
     * mryt233 改（2026-11-07）：被点的 pos 算第 1 层，整摞叠完不超过 {@link #MAX_STACK}，
     * 和条凳那边统一（以前从 pos 本身起走满 16 步，实际能叠到 17 层）。
     * 被点的那一格可能在这摞中间或顶上（叠高了以后玩家多半点最上面那张），所以先往下走到最底下再往上数满整摞。
     */
    private static BlockPos stackTarget(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos probe = pos.mutable();
        int steps = 0;                                   // 只用来防死循环，不参与层数
        while (steps++ < MAX_STACK && world.getBlockState(probe.below()).getBlock() instanceof HighStoolBlock) {
            probe.move(Direction.DOWN);
        }
        // 这时 probe 已经在整摞最底下，从最底下往上数整摞有几层（含它自己）
        int layers = 1;
        while (layers < MAX_STACK && world.getBlockState(probe.above()).getBlock() instanceof HighStoolBlock) {
            probe.move(Direction.UP);
            layers++;
        }
        if (layers >= MAX_STACK) {
            return null;                                 // 已经是一摞 16 格，再叠就超上限
        }
        BlockPos target = probe.above().immutable();
        if (world.isOutsideBuildHeight(target)) {
            return null;
        }
        return world.getBlockState(target).canBeReplaced() ? target : null;
    }

    /** 在 target 放一张同木材的高凳，扣掉手里的一个，放木板音效并记一次「叠高凳」成就。 */
    private static void placeOnTop(Level world, BlockPos target, HighStoolBlock stool,
                                   Player player, ItemStack stack) {
        BlockState placed = stool.defaultBlockState()
            .setValue(FACING, player.getDirection())
            .setValue(STACKED, world.getBlockState(target.above()).getBlock() instanceof HighStoolBlock);
        world.setBlock(target, placed, Block.UPDATE_ALL);
        world.playSound((Player) null, target,
            stool.planks().defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }
        HotPotCriterion.fire(player, "tall_stool");
    }

    // ------------------------------------------------------------------ 坐

    /**
     * 空手右键坐上去：只有这一格上面没有别的凳子（没被 STACKED）才坐，凳塔中间那几层坐不了；
     * 头顶也坐不下一个人时同样不坐。座位上已经有人就提示「座位被占了」（复用条凳的语言键）。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger() || state.getValue(STACKED)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY() + SEAT_Y, pos.getZ() + 0.5);
        // 坐上去以后人占这一格顶面以上（凳子自己是实心的，所以从 y+1 开始量），头顶有实心方块就坐不下
        AABB room = new AABB(at.x - 0.3, pos.getY() + 1.0, at.z - 0.3,
            at.x + 0.3, pos.getY() + 1.0 + player.getBbHeight(), at.z + 0.3);
        if (!world.noCollision(player, room)) {
            return InteractionResult.PASS;
        }
        if (!world.getEntitiesOfClass(Seat.class, new AABB(at, at).inflate(0.9, 1.0, 0.9), Entity::isAlive).isEmpty()) {
            player.displayClientMessage(Component.translatable(HotPotBlockEntity.key("msg.seat_taken")), true);
            return InteractionResult.SUCCESS;
        }
        Seat seat = Seat.of(world, at, pos, state.getValue(FACING));
        world.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.SUCCESS;
    }

    /** 高凳被拆掉时，把这一格上的座位一起清掉（坐着的人会被放下）。 */
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClientSide && !state.is(newState.getBlock())) {
            Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY() + SEAT_Y, pos.getZ() + 0.5);
            for (Seat seat : world.getEntitiesOfClass(Seat.class,
                new AABB(at, at).inflate(1.0, 2.0, 1.0), Entity::isAlive)) {
                seat.discard();
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    /**
     * 高凳上的座位：直接继承条凳的 {@link SeatEntity}（隐形、不存档、上下车的逻辑都一样），
     * 只是「凳子没了就自己消失」的判断换成高凳：坐着的那一格不是高凳、或者上面又叠了一张（人会陷在凳里）
     * 就消失。座位只有服务端算，不需要同步这几个字段。
     */
    public static final class Seat extends SeatEntity {
        /** 坐在哪一格的高凳上。 */
        private BlockPos anchor = BlockPos.ZERO;
        // mryt233 改（2026-11-07）：facing / seatY 复用父类 SeatEntity 的 protected 字段，
        // 以前这里多存一份 facing，父类那份恒为 NORTH，是个埋着的坑。

        public Seat(EntityType<? extends SeatEntity> type, Level world) {
            super(type, world);
        }

        static Seat of(Level world, Vec3 at, BlockPos anchor, Direction facing) {
            Seat seat = new Seat(HotPotContent.SEAT, world);
            seat.anchor = anchor.immutable();
            // mryt233 改（2026-11-07）：写父类的字段，父类的起身落点才能拿到正确朝向和凳面高度
            seat.facing = facing;
            seat.seatY = SEAT_Y;
            seat.moveTo(at.x, at.y, at.z, facing.toYRot(), 0F);
            return seat;
        }

        /**
         * 高凳座位的位置记在 {@link #anchor} 里，基类的 a / b 用不上。
         * mryt233 改（2026-11-07）：覆写一下，免得基类按格清理时把 ZERO 当坐标误伤（条凳 clearSeatsAt 会扫到所有座位）。
         */
        @Override
        public boolean sitsOn(BlockPos pos) {
            return anchor.equals(pos);
        }

        @Override
        public void tick() {
            // Entity.tick() 就是 baseTick()；不调 SeatEntity.tick()，那边只认条凳，会立刻把手上的座位丢掉。
            baseTick();
            if (level().isClientSide) {
                return;
            }
            BlockState state = level().getBlockState(anchor);
            if (!isVehicle()
                || !(state.getBlock() instanceof HighStoolBlock)
                || state.getValue(STACKED)) {
                discard();
            }
        }

        // mryt233 改（2026-11-07）：起身落点直接复用父类的 SeatEntity#dismountBehind，不再重复写一遍。
    }
}
