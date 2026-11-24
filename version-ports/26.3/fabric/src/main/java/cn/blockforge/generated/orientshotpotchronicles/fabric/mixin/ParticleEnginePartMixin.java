// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.fabric.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.BlockPartParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** mryt233 改：26.3 在挖掘声之后、跳过不可见上半截之前替换本模组分部粒子。 */
@Mixin(ClientLevel.class)
abstract class ParticleEnginePartMixin {
    @Inject(method = "addBreakingBlockEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"),
        cancellable = true)
    private void hotpot$partHit(BlockPos pos, Direction face, boolean withSound, CallbackInfo callback) {
        if (BlockPartParticles.hit(Minecraft.getInstance().particleEngine, pos, face)) callback.cancel();
    }
}
