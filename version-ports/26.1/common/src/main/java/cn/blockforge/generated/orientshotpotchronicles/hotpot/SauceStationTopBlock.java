// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * 蘸料台的「上半截」（2026-10-23 用户：1.5 格高肯定占 2 格）：看不见，占住蘸料台头顶那一格，
 * 别的方块放不进来、不会和台子穿插。做法同高灶台的 StoveTopBlock：形状 = 下面蘸料台的形状下移一格，
 * 点它（打蘸料、开补货界面）原样转给下面的蘸料台，挖它 = 挖蘸料台；下面不是蘸料台了它自己消失。
 * 作者：mryt233 */
public class SauceStationTopBlock extends net.minecraft.world.level.block.Block implements PartOutline {
    public SauceStationTopBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(BELOW, Below.NORTH));
    }

    /**
     * 下面蘸料台的朝向，记在自己的方块状态里（mryt233 2026-11-01）。以前形状现读下面方块、还开了 dynamicShape，
     * 光照线程调 getShape 读世界会和主线程互等，整服卡死（同 {@link StoveTopBlock.Below}）。
     */
    public enum Below implements net.minecraft.util.StringRepresentable {
        NONE("none", null), NORTH("north", Direction.NORTH), EAST("east", Direction.EAST),
        SOUTH("south", Direction.SOUTH), WEST("west", Direction.WEST);

        private final String name;
        private final Direction facing;

        Below(String name, Direction facing) {
            this.name = name;
            this.facing = facing;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static Below of(BlockState station) {
            if (!(station.getBlock() instanceof SauceStationBlock)) {
                return NONE;
            }
            return switch (station.getValue(SauceStationBlock.FACING)) {
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                default -> NORTH;
            };
        }
    }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Below> BELOW =
        net.minecraft.world.level.block.state.properties.EnumProperty.create("below", Below.class);

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BELOW);
    }

    /** 给这个蘸料台配的上半截状态。 */
    public static BlockState mirroring(BlockState station) {
        return HotPotContent.SAUCE_STATION_TOP.defaultBlockState().setValue(BELOW, Below.of(station));
    }

    private static boolean stationBelow(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.below()).getBlock() instanceof SauceStationBlock;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 各朝向预先算好的形状：下面蘸料台的形状往下挪一格。 */
    private static final VoxelShape[] OUTLINES = new VoxelShape[Below.values().length];
    private static final VoxelShape[] COLLISIONS = new VoxelShape[Below.values().length];

    static {
        for (Below b : Below.values()) {
            OUTLINES[b.ordinal()] = b.facing == null ? Shapes.empty() : SauceStationBlock.outline(b.facing).move(0, -1, 0);
            COLLISIONS[b.ordinal()] = b.facing == null ? Shapes.empty() : SauceStationBlock.collision(b.facing).move(0, -1, 0);
        }
    }

    /** 形状只看自己的状态，绝不读世界（见 {@link Below}）。 */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINES[state.getValue(BELOW).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return COLLISIONS[state.getValue(BELOW).ordinal()];
    }

    /** 选中框照下面蘸料台的规则（只框对着的碗），挪到这一格的坐标。 */
    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        BlockState below = world.getBlockState(pos.below());
        if (below.getBlock() instanceof SauceStationBlock station) {
            VoxelShape part = station.aimedPart(below, world, pos.below(), player);
            return part == null ? null : part.move(0, -1, 0);
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return stationBelow(world, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader world, net.minecraft.world.level.ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        if (direction != Direction.DOWN) {
            return state;
        }
        // 下面变了：不是蘸料台就消失，否则照新朝向更新（用传进来的 neighborState，不再读世界）
        Below below = Below.of(neighborState);
        return below == Below.NONE ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state.setValue(BELOW, below);
    }

    /** 准星位置不变，只把「点到的方块」换成下面的蘸料台（SauceStationBlock.aimed 按准星位置算碗）。 */
    private static BlockHitResult toStation(BlockPos pos, BlockHitResult hit) {
        return new BlockHitResult(hit.getLocation(), hit.getDirection(), pos.below(), hit.isInside());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.below());
        if (!(below.getBlock() instanceof SauceStationBlock)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return below.useItemOn(stack, world, player, hand, toStation(pos, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.below());
        if (!(below.getBlock() instanceof SauceStationBlock)) {
            return InteractionResult.PASS;
        }
        return below.useWithoutItem(world, player, toStation(pos, hit));
    }

    /** 挖的快慢按下面的蘸料台算（木头的用斧、铁的用镐）。 */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        return below.getBlock() instanceof SauceStationBlock
            ? below.getDestroyProgress(player, world, pos.below())
            : super.getDestroyProgress(state, player, world, pos);
    }

    /** 挖上半截 = 挖整个蘸料台（工具对才掉蘸料台，格子里的原料照常掉出来）。 */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        BlockState below = world.getBlockState(pos.below());
        if (!world.isClientSide() && below.getBlock() instanceof SauceStationBlock) {
            boolean drop = !player.isCreative() && player.hasCorrectToolForDrops(below);
            world.destroyBlock(pos.below(), drop, player);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(world.getBlockState(pos.below()).getBlock());
    }
}