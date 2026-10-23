package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableDecor;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 火锅桌上的小摆件（标签 table_decorations）贴着桌面：模型偏移（原版花草随机偏移走的同一条路，
 * 原版渲染和钠都读它）+ 选中框一起挪，见 {@link TableDecor}。
 *
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
abstract class BlockStateTableDecorMixin {
    @Inject(method = "getModelOffset", at = @At("RETURN"), cancellable = true)
    private void orientsHotpot$sitOnTable(BlockView world, BlockPos pos, CallbackInfoReturnable<Vec3d> cir) {
        double lift = TableDecor.liftOf(world, pos, (BlockState) (Object) this);
        if (lift != 0) {
            cir.setReturnValue(cir.getReturnValue().add(0, lift, 0));
        }
    }

    // 注意：不能让 hasModelOffset 对标签里的方块答 true——原版建形状缓存时，「有偏移 + 有碰撞」的方块必须
    // 声明 dynamicBounds，否则进世界时直接崩（r99 第一版就这样崩了：minecraft:cake has a collision shape and
    // an offset type...）。原版画方块时不管 hasModelOffset，都会调 getModelOffset。

    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
        at = @At("RETURN"), cancellable = true)
    private void orientsHotpot$outlineOnTable(BlockView world, BlockPos pos, ShapeContext context,
                                             CallbackInfoReturnable<VoxelShape> cir) {
        double lift = TableDecor.liftOf(world, pos, (BlockState) (Object) this);
        if (lift != 0) {
            cir.setReturnValue(cir.getReturnValue().offset(0, lift, 0));
        }
    }
}
