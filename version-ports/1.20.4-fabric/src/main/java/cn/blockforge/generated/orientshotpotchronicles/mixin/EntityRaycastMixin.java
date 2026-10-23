package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.TallReach;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 准星选方块（客户端准星、服务端判断对着哪个盘子都走这里）：高火锅桌伸进上一格的桌面也要能被选中，
 * 不然会选到后面那张桌子。具体见 {@link TallReach}。
 * 作者：Claude
 */
@Mixin(Entity.class)
abstract class EntityRaycastMixin {
    @Inject(method = "raycast", at = @At("RETURN"), cancellable = true)
    private void orientsHotpot$tallReach(double maxDistance, float tickDelta, boolean includeFluids,
                                         CallbackInfoReturnable<HitResult> cir) {
        if (includeFluids) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Vec3d start = self.getCameraPosVec(tickDelta);
        Vec3d end = start.add(self.getRotationVec(tickDelta).multiply(maxDistance));
        BlockHitResult hit = TallReach.raycast(self.getWorld(), start, end, ShapeContext.of(self));
        if (hit != null) {
            cir.setReturnValue(hit);
        }
    }
}
