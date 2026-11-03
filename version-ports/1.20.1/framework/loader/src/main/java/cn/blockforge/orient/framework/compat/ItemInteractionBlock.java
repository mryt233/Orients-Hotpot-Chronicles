package cn.blockforge.orient.framework.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 在旧版单个 use 回调内按物品、空手顺序调用内容逻辑。作者：mryt233 */
public interface ItemInteractionBlock {
    default ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    default InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                              Player player, BlockHitResult hit) { return InteractionResult.PASS; }
    default InteractionResult dispatchUse(BlockState state, Level world, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult result = useItemOn(player.getItemInHand(hand), state, world, pos, player, hand, hit);
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND)
            return useWithoutItem(state, world, pos, player, hit);
        return result.result();
    }
    static ItemInteractionResult useItemOn(BlockState state, ItemStack stack, Level world, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return state.getBlock() instanceof ItemInteractionBlock block
            ? block.useItemOn(stack, state, world, hit.getBlockPos(), player, hand, hit)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    static InteractionResult useWithoutItem(BlockState state, Level world, Player player, BlockHitResult hit) {
        return state.getBlock() instanceof ItemInteractionBlock block
            ? block.useWithoutItem(state, world, hit.getBlockPos(), player, hit) : InteractionResult.PASS;
    }
}
