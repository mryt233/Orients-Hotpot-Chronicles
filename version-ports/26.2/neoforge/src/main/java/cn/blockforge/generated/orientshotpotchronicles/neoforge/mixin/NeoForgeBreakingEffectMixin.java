// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.neoforge.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.BlockPartParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge 26.2 持续挖掘粒子入口拦截。作者：mryt233 */
@Mixin(ClientLevel.class)
abstract class NeoForgeBreakingEffectMixin {
    @Inject(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/HitResult;)V",
        at = @At("HEAD"), cancellable = true)
    private void hotpot$partHit(BlockPos pos, Direction face, HitResult hitResult, CallbackInfo callback) {
        if (BlockPartParticles.hit(Minecraft.getInstance().particleEngine, pos, face)) callback.cancel();
    }
}
