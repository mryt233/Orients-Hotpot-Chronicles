package cn.blockforge.orient.framework.client;

import cn.blockforge.orient.framework.game.PartOutline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 部件选中框的共同画法；返回 true 表示替代原版轮廓。作者：mryt233 */
public final class PartOutlineDrawing {
    private PartOutlineDrawing() {
    }

    public static boolean render(BlockState state, BlockPos pos, double cameraX, double cameraY,
                                 double cameraZ, PoseStack poses, VertexConsumer vertices) {
        Minecraft client = Minecraft.getInstance();
        if (!(state.getBlock() instanceof PartOutline part) || client.player == null || client.level == null) {
            return false;
        }
        var shape = part.aimedPart(state, client.level, pos, client.player);
        if (shape == null || shape.isEmpty()) {
            return false;
        }
        PoseStack.Pose pose = poses.last();
        double ox = pos.getX() - cameraX, oy = pos.getY() - cameraY, oz = pos.getZ() - cameraZ;
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0) {
                return;
            }
            dx /= len;
            dy /= len;
            dz /= len;
            vertices.addVertex(pose, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz))
                .setColor(0, 0, 0, 0.4F).setNormal(pose, dx, dy, dz);
            vertices.addVertex(pose, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz))
                .setColor(0, 0, 0, 0.4F).setNormal(pose, dx, dy, dz);
        });
        return true;
    }
}
