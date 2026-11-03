package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableDecor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 火锅桌上的小摆件（标签 table_decorations）贴着桌面：模型偏移（原版花草随机偏移走的同一条路，
 * 原版渲染和钠都读它）+ 选中框一起挪，见 {@link TableDecor}。
 * 作者：mryt233 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateTableDecorMixin {
    @Inject(method = "getOffset", at = @At("RETURN"), cancellable = true)
    private void orientsHotpot$sitOnTable(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
        double lift = TableDecor.liftOf(world, pos, (BlockState) (Object) this);
        if (lift != 0) {
            cir.setReturnValue(cir.getReturnValue().add(0, lift, 0));
        }
    }

    // 注意：不能让 hasModelOffset 对标签里的方块答 true——原版建形状缓存时，「有偏移 + 有碰撞」的方块必须
    // 声明 dynamicBounds，否则进世界时直接崩（r99 第一版就这样崩了：minecraft:cake has a collision shape and
    // an offset type...）。原版画方块时不管 hasModelOffset，都会调 getModelOffset。

    @Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"), cancellable = true)
    private void orientsHotpot$outlineOnTable(BlockGetter world, BlockPos pos, CollisionContext context,
                                             CallbackInfoReturnable<VoxelShape> cir) {
        double lift = TableDecor.liftOf(world, pos, (BlockState) (Object) this);
        if (lift != 0) {
            cir.setReturnValue(cir.getReturnValue().move(0, lift, 0));
        }
    }
}