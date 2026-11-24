// mryt233 改：26.1 同代移植同步。
// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 小石磨方块：无 GUI 实体交互。投入黄豆配水（水桶 / 水瓶）后右键推动上扇石磨转动研磨出豆浆容器，
 * 同时兼容干磨配方。
 * 作者：mryt233
 */
public class StoneMillBlock extends Block implements EntityBlock {
    public static final MapCodec<StoneMillBlock> CODEC = simpleCodec(StoneMillBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape BASE_SHAPE = Shapes.or(
        Block.box(2, 0, 2, 14, 12, 14),
        Block.box(1, 12, 1, 15, 17, 15),
        Block.box(3, 17, 3, 13, 22, 13)
    );

    public StoneMillBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneMillBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof StoneMillBlockEntity mill)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
        }
        if (mill.tryCollectTrough(player, hand, stack)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide());
        }
        if (mill.tryAddWater(player, hand, stack)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide());
        }
        if (mill.tryInsertInput(player, hand, stack)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide());
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof StoneMillBlockEntity mill)) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
        }
        if (player.isShiftKeyDown() && mill.tryWithdrawInput(player)) {
            return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide());
        }
        if (mill.tryGrind(player, state.getValue(FACING))) {
            return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide());
        }
        return Use.blockEmpty(UseOutcome.PASS, world.isClientSide());
    }

    /** mryt233 改：出料口前新放置木桶时尝试接取当前槽里的豆浆。 */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock,
                                   Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, world, pos, neighborBlock, orientation, movedByPiston);
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof StoneMillBlockEntity mill) {
            mill.tryAutoDrainToAdjacentVat();
        }
    }
}
