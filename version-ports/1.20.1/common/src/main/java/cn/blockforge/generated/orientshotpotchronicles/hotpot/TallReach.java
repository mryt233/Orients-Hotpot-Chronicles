package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * 1.5 格高的方块（高火锅桌）上半截伸进了上面那一格。原版射线逐格只看「这一格自己的方块」，
 * 走到桌子上方那一格时看见的是空气，于是穿过桌面、选中后面那张桌子（用户报的 bug）。
 * 这里按原版顺序逐格走，每一格额外看一眼下面那格方块伸上来的部分，取更近的那个；
 * 只有命中来自「伸上来的部分」时才返回结果，其他情况交回原版。
 * 2026-10-23 加反方向：矮火锅桌上的盘子 / 饮料底面比自己这一格低 5 像素，沉进了下面桌子那一格；
 * 原版查到桌子那一格只认桌子，准星对着盘子下半截选中的是桌子（用户报：桌上的饮料、菜中键复制不了）。
 * 所以每一格也看一眼上面那格方块垂下来的部分。
 * 作者：mryt233 */
public final class TallReach {
    private TallReach() {
    }

    private record Found(BlockHitResult hit, boolean fromBelow) {
    }

    /** 命中了下面方块伸上来 / 上面方块垂下来的部分就返回这个命中，否则 null（用原版结果）。 */
    public static BlockHitResult raycast(BlockGetter world, Vec3 start, Vec3 end, CollisionContext context) {
        Found found = BlockGetter.traverseBlocks(start, end, null, (unused, pos) -> hitAt(world, pos, start, end, context),
            unused -> null);
        return found != null && found.fromBelow() ? found.hit() : null;
    }

    private static Found hitAt(BlockGetter world, BlockPos pos, Vec3 start, Vec3 end, CollisionContext context) {
        BlockState state = world.getBlockState(pos);
        BlockHitResult own = world.clipWithInteractionOverride(start, end, pos, state.getShape(world, pos, context), state);
        BlockHitResult below = null;
        BlockPos downPos = pos.below();
        BlockState down = world.getBlockState(downPos);
        if (!down.isAir()) {
            VoxelShape shape = down.getShape(world, downPos, context);
            if (!shape.isEmpty() && shape.max(Direction.Axis.Y) > 1.0) {
                BlockHitResult hit = shape.clip(start, end, downPos);
                // 只算落在这一格里的命中；落在下面那格的交给原版走到那一格时处理
                if (hit != null && hit.getLocation().y >= pos.getY() - 1.0E-7) {
                    below = hit;
                }
            }
        }
        BlockPos upPos = pos.above();
        BlockState up = world.getBlockState(upPos);
        if (!up.isAir()) {
            VoxelShape shape = up.getShape(world, upPos, context);
            if (!shape.isEmpty() && shape.min(Direction.Axis.Y) < 0.0) {
                BlockHitResult hit = shape.clip(start, end, upPos);
                if (hit != null && hit.getLocation().y < upPos.getY() + 1.0E-7
                    && (below == null || hit.getLocation().distanceToSqr(start) < below.getLocation().distanceToSqr(start))) {
                    below = hit;   // 「不是这一格自己的」命中，和伸上来的一样处理
                }
            }
        }
        if (own == null && below == null) {
            return null;
        }
        if (below == null) {
            return new Found(own, false);
        }
        if (own == null || below.getLocation().distanceToSqr(start) < own.getLocation().distanceToSqr(start)) {
            return new Found(below, true);
        }
        return new Found(own, false);
    }
}