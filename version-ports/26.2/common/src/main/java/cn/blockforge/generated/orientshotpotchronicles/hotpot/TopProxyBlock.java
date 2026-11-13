// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：第三批 3G 右键结果改走适配小包（意图 → 原版常量），逻辑不变。
// mryt233 改：26.2 原版 API（updateShape / InteractionResult / getCloneItemStack / isClientSide()）。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.game.PartOutline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 上半截公共行为；形状只读自身 below，禁止在形状查询中访问世界或开启 dynamicShape。 */
public abstract class TopProxyBlock<T extends Enum<T> & StringRepresentable> extends Block implements PartOutline {
    protected TopProxyBlock(Properties properties) { super(properties); }

    protected abstract EnumProperty<T> belowProperty();
    protected abstract T belowOf(BlockState state);
    protected abstract boolean accepts(BlockState state);
    protected abstract VoxelShape[] outlines();
    protected VoxelShape[] collisions() { return outlines(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(belowProperty());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return outlines()[state.getValue(belowProperty()).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return collisions()[state.getValue(belowProperty()).ordinal()];
    }

    @Override
    public VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        BlockState below = world.getBlockState(pos.below());
        if (accepts(below)) {
            VoxelShape part = ((PartOutline) below.getBlock()).aimedPart(below, world, pos.below(), player);
            return part == null ? null : part.move(0, -1, 0);
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return belowOf(world.getBlockState(pos.below())).ordinal() != 0;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction != Direction.DOWN) return state;
        T below = belowOf(neighborState);
        return below.ordinal() == 0 ? Blocks.AIR.defaultBlockState() : state.setValue(belowProperty(), below);
    }

    private static BlockHitResult toBelow(BlockPos pos, BlockHitResult hit) {
        return new BlockHitResult(hit.getLocation(), hit.getDirection(), pos.below(), hit.isInside());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.below());
        return accepts(below) ? below.useItemOn(stack, world, player, hand, toBelow(pos, hit))
            : Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState below = world.getBlockState(pos.below());
        return accepts(below) ? below.useWithoutItem(world, player, toBelow(pos, hit))
            : Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        return accepts(below) ? below.getDestroyProgress(player, world, pos.below())
            : super.getDestroyProgress(state, player, world, pos);
    }

    protected void beforeDestroyBelow(Level world, BlockPos pos, BlockState below, Player player) { }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        BlockState below = world.getBlockState(pos.below());
        if (!world.isClientSide() && accepts(below)) {
            boolean drop = !player.isCreative() && player.hasCorrectToolForDrops(below);
            beforeDestroyBelow(world, pos.below(), below, player);
            world.destroyBlock(pos.below(), drop, player);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        BlockState below = world.getBlockState(pos.below());
        return accepts(below) ? below.getCloneItemStack(world, pos.below(), includeData)
            : super.getCloneItemStack(world, pos, state, includeData);
    }
}
