// 0.1.2 的 NeoForge 接口迁移：。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PartOutline;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

/**
 * 只框准星对着的那一样（PartOutline，2026-10-23 用户要求，规范见 docs/交互规范手册.md）：
 * 原版画选中框之前问一下方块，给了一个部分就自己画这一部分、不让原版画整格。线的颜色 / 粗细同原版（黑色 40%）。
 *
 */
public final class PartOutlineRenderer {
    private PartOutlineRenderer() {
    }

    public static void register() {
        // 仅接管 PartOutline 的选中框。
        NeoForge.EVENT_BUS.addListener((RenderHighlightEvent.Block event) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }
            BlockPos pos = event.getTarget().getBlockPos();
            var state = client.world.getBlockState(pos);
            if (!(state.getBlock() instanceof PartOutline part)) return;
            VoxelShape shape = part.aimedPart(state, client.world, pos, client.player);
            if (shape == null || shape.isEmpty()) {
                return;
            }
            VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderLayer.getLines());
            MatrixStack.Entry e = event.getPoseStack().peek();
            double ox = pos.getX() - event.getCamera().getPos().x, oy = pos.getY() - event.getCamera().getPos().y, oz = pos.getZ() - event.getCamera().getPos().z;
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= len;
                dy /= len;
                dz /= len;
                vc.vertex(e, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz)).color(0, 0, 0, 0.4F).normal(e, dx, dy, dz);
                vc.vertex(e, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz)).color(0, 0, 0, 0.4F).normal(e, dx, dy, dz);
            });
            event.setCanceled(true);
        });
    }
}
