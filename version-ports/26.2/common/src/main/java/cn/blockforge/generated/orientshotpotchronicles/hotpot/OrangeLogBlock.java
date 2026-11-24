// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.function.Supplier;
import net.minecraft.advancements.triggers.CriteriaTriggers;
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

/** 橙木原木 / 木头：沿用柱状旋转状态，持斧右键可去皮为对应的去皮变体。 */
public class OrangeLogBlock extends RotatedPillarBlock {
    private final @Nullable Supplier<Block> stripped;

    public OrangeLogBlock(BlockBehaviour.Properties properties, @Nullable Supplier<Block> stripped) {
        super(properties);
        this.stripped = stripped;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stripped == null || player.isSecondaryUseActive()
            || !(stack.is(ItemTags.AXES) || stack.getItem() instanceof AxeItem)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        Block target = stripped.get();
        if (target == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        BlockState nextState = target.defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            level.setBlock(pos, nextState, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, nextState));
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
