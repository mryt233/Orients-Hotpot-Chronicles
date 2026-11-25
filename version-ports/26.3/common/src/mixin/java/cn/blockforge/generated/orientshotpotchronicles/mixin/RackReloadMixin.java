// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRules;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 两加载器共用原版的成功提交屏障，异常完成时不触碰旧规则。 */
@Mixin(MinecraftServer.class)
public abstract class RackReloadMixin {
    @Inject(method="reloadResources",at=@At("RETURN"))
    private void hotpot$reloadRackRules(Collection<String> packs, CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        ci.getReturnValue().thenRun(() -> server.execute(() -> RackRules.reload(server)));
    }
}
