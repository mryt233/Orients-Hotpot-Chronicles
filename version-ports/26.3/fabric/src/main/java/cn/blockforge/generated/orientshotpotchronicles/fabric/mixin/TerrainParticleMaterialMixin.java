// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.fabric.mixin;

import cn.blockforge.generated.orientshotpotchronicles.client.BlockPartParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 仅 Fabric：为源位置明确的桌架粒子补上实际材质，不覆盖其他方块或 NeoForge 的 ModelData。mryt233 改（26.2 适配）。 */
@Mixin(TerrainParticle.class)
abstract class TerrainParticleMaterialMixin extends SingleQuadParticle {
    protected TerrainParticleMaterialMixin(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At("TAIL"))
    private void hotpot$materialSprite(ClientLevel level, double x, double y, double z,
                                      double dx, double dy, double dz, BlockState state, BlockPos pos,
                                      CallbackInfo callback) {
        var sprite = BlockPartParticles.materialSprite(level, pos, state);
        if (sprite != null) setSprite(sprite);
    }
}
