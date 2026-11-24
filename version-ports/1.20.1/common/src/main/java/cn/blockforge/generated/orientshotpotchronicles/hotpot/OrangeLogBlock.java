// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.compat.ItemInteractionBlock;
import cn.blockforge.orient.framework.compat.ItemInteractionResult;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 橙木原木 / 木头：沿用柱状旋转状态，持斧右键可去皮为对应的去皮变体（1.20.1 适配）。作者：mryt233 */
public class OrangeLogBlock extends RotatedPillarBlock implements ItemInteractionBlock {
    private final @Nullable Supplier<Block> stripped;

    public OrangeLogBlock(BlockBehaviour.Properties properties, @Nullable Supplier<Block> stripped) {
        super(properties);
        this.stripped = stripped;
    }

    @Override
    public ItemInteractionResult compatUseItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (stripped == null || player.isSecondaryUseActive()
            || !(stack.is(ItemTags.AXES) || stack.getItem() instanceof AxeItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Block target = stripped.get();
        if (target == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockState nextState = target.defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide) {
            level.setBlock(pos, nextState, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, nextState));
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** mryt233 改：1.20.1 的单回调交互入口。 */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        return dispatchUse(state, world, pos, player, hand, hit);
    }
}
