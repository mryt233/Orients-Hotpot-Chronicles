// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableDecor;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 区块快照仍提供世界访问，避免在渲染工作线程读取客户端活动世界。 */
@Mixin(ModelBlockRenderer.class)
abstract class TableDecorModelOffsetMixin {
    @Redirect(method = "tesselateBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 orientsHotpot$tableOffset(BlockState state, BlockPos pos, BlockQuadOutput output,
            float x, float y, float z, BlockAndTintGetter level, BlockPos blockPos, BlockState blockState,
            BlockStateModel model, long seed) {
        return state.getOffset(pos).add(0, TableDecor.liftOf(level, pos, state), 0);
    }
}
