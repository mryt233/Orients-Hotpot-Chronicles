// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import cn.blockforge.orient.framework.compat.ItemInteractionBlock;
import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 开口大木桶方块：敞口木桶模型（非原版封闭木桶 / 非炼药锅），负责豆浆点卤加压成冷豆腐及清水泡发干货（1.20.1 适配）。
 * 作者：mryt233
 */
public class WoodenVatBlock extends Block implements EntityBlock, ItemInteractionBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_LID = BooleanProperty.create("has_lid");

    private static final VoxelShape OUTER = Shapes.or(
        Block.box(1, 0, 1, 15, 3, 15),
        Block.box(1, 3, 1, 15, 14, 2.5),
        Block.box(1, 3, 13.5, 15, 14, 15),
        Block.box(1, 3, 2.5, 2.5, 14, 13.5),
        Block.box(13.5, 3, 2.5, 15, 14, 13.5)
    );
    private static final VoxelShape WITH_LID = Block.box(1, 0, 1, 15, 14.5, 15);

    public WoodenVatBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(HAS_LID, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_LID);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(HAS_LID, false);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(HAS_LID) ? WITH_LID : OUTER;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodenVatBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type != HotPotContent.WOODEN_VAT_BE) {
            return null;
        }
        return world.isClientSide
            ? (lvl, p, st, be) -> WoodenVatBlockEntity.clientTick(lvl, p, st, (WoodenVatBlockEntity) be)
            : (lvl, p, st, be) -> WoodenVatBlockEntity.serverTick(lvl, p, st, (WoodenVatBlockEntity) be);
    }

    @Override
    public ItemInteractionResult compatUseItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof WoodenVatBlockEntity vat)) {
            return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide);
        }
        if (vat.tryCollectOutput(player)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (vat.tryApplyLid(player, hand, stack)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (vat.tryPourFluid(player, hand, stack)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        if (vat.tryInsertItem(player, hand, stack)) {
            return Use.blockItem(UseOutcome.SUCCESS, world.isClientSide);
        }
        return Use.blockItem(UseOutcome.PASS_TO_EMPTY, world.isClientSide);
    }

    @Override
    public InteractionResult compatUseWithoutItem(BlockState state, Level world, BlockPos pos,
                                                    Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof WoodenVatBlockEntity vat)) {
            return Use.blockEmpty(UseOutcome.PASS, world.isClientSide);
        }
        if (vat.tryInteractEmpty(player)) {
            return Use.blockEmpty(UseOutcome.SUCCESS, world.isClientSide);
        }
        return Use.blockEmpty(UseOutcome.PASS, world.isClientSide);
    }

    /** mryt233 改：1.20.1 的单回调交互入口。 */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        return dispatchUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof WoodenVatBlockEntity vat) {
            vat.dropContents();
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
