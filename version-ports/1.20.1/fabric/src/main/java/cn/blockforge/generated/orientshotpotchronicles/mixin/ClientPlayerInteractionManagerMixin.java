package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 对着灶上的锅挖完时，客户端不要先把整个灶台预测成空气（会闪一下、冒砖块碎屑）；
 * 等服务端只拿掉锅、把新状态发回来。挖掘请求照常发给服务端。
 */
@Mixin(ClientPlayerInteractionManager.class)
abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$breakPotOnly(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        BlockState state = client.world.getBlockState(pos);
        // 灶上的锅只挖锅；盘子格只挖准星对着的那个盘子：都不能先把整格预测成空气
        if (StoveBlock.aimsAtPot(state, pos, client.player)
            || state.isOf(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.PLATE_HOLDER)) {
            cir.setReturnValue(false);
        }
    }
}
