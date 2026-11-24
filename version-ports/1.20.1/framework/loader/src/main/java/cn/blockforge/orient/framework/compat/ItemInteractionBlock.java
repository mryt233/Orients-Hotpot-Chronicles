package cn.blockforge.orient.framework.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 在旧版单个 use 回调内按物品、空手顺序调用内容逻辑。
 *
 * <p>mryt233 改（2026-11-15）：方法改名为 {@code compatUseItemOn} / {@code compatUseWithoutItem}，
 * 避免与 1.20.1 第三方扩展注入在 {@code BlockBehaviour} 上的同名 1.21+ 回调冲突并绕过
 * {@link #dispatchUse}。</p>
 * 作者：mryt233
 */
public interface ItemInteractionBlock {
    default ItemInteractionResult compatUseItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    default InteractionResult compatUseWithoutItem(BlockState state, Level world, BlockPos pos,
                                                     Player player, BlockHitResult hit) { return InteractionResult.PASS; }
    default InteractionResult dispatchUse(BlockState state, Level world, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult result = compatUseItemOn(player.getItemInHand(hand), state, world, pos, player, hand, hit);
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND)
            return compatUseWithoutItem(state, world, pos, player, hit);
        return result.result();
    }
    static ItemInteractionResult compatUseItemOn(BlockState state, ItemStack stack, Level world, Player player,
                                                 InteractionHand hand, BlockHitResult hit) {
        return state.getBlock() instanceof ItemInteractionBlock block
            ? block.compatUseItemOn(stack, state, world, hit.getBlockPos(), player, hand, hit)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    static InteractionResult compatUseWithoutItem(BlockState state, Level world, Player player, BlockHitResult hit) {
        return state.getBlock() instanceof ItemInteractionBlock block
            ? block.compatUseWithoutItem(state, world, hit.getBlockPos(), player, hit) : InteractionResult.PASS;
    }
}
