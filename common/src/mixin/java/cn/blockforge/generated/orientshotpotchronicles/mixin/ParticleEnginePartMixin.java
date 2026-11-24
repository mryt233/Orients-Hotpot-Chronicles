// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.BlockPartParticles;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在原版跳过不可见上半截之前处理自家分部挖掘效果，其他方块继续走原版。 */
@Mixin(ParticleEngine.class)
abstract class ParticleEnginePartMixin {
    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void hotpot$partHit(BlockPos pos, Direction face, CallbackInfo callback) {
        if (BlockPartParticles.hit((ParticleEngine) (Object) this, pos, face)) callback.cancel();
    }
}
