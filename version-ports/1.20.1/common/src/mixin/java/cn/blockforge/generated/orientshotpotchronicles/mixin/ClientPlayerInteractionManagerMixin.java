package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 对着灶上的锅挖完时，客户端不要先把整个灶台预测成空气（会闪一下、冒砖块碎屑）；
 * 等服务端只拿掉锅、把新状态发回来。挖掘请求照常发给服务端。
 */
@Mixin(MultiPlayerGameMode.class)
abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$breakPotOnly(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        // 高灶台上半截对着锅挖 = 只挖下面灶上的锅：以前漏了这种，上半截先被预测成空气、闪一下（代码检查 B7，mryt233 2026-11-01）
        boolean topOverPot = state.is(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.STOVE_TOP)
            && StoveBlock.aimsAtPot(client.level.getBlockState(pos.below()), pos.below(), client.player);
        // 灶上的锅只挖锅；盘子格只挖准星对着的那个盘子：都不能先把整格预测成空气
        if (topOverPot || StoveBlock.aimsAtPot(state, pos, client.player)
            || state.is(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.PLATE_HOLDER)
            || (client.level.getBlockEntity(pos) instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.CuttingBoardBlockEntity board
                && !board.item().isEmpty()
                && cn.blockforge.generated.orientshotpotchronicles.hotpot.CuttingBoardBlock.aimsAtItem(pos, client.player))) {
            cir.setReturnValue(false);
        }
    }
}
