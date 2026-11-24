// mryt233 改：26.1 同代移植同步。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateFoodLayout;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.client.renderer.texture.OverlayTexture;
import cn.blockforge.orient.framework.client.RenderFrame;
import cn.blockforge.generated.orientshotpotchronicles.client.RenderItems;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 盘子里的菜怎么摆（摆在桌上的盘子、手里 / 物品栏里的盘子共用，计算委托 {@link PlateFoodLayout}，与查看器 animations.js 一致）。
 * 作者：mryt233 */
public final class PlateFoodRenderer {
    private PlateFoodRenderer() {
    }

    /** 画一个盘子里的菜；调用前矩阵在「盘子所在方块」的坐标系（0..1），盘子中心在方块中心、盘底在 y=0。 */
    public static void render(PlateSize size, Direction.Axis axis, List<ItemStack> items, PoseStack matrices,
                              RenderFrame consumers, int light, Level world, int seed,
                              RenderItems itemRenderer) {
        int cells = size.cells();
        matrices.pushPose();
        if (axis == Direction.Axis.Z) {
            matrices.translate(0.5, 0, 0.5);
            matrices.mulPose(Axis.YN.rotationDegrees(90F));
            matrices.translate(-0.5, 0, -0.5);
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int cell = i % cells;
            int inCell = 0;
            for (int j = cell; j < items.size(); j += cells) {
                if (!items.get(j).isEmpty()) {
                    inCell++;
                }
            }
            PlateFoodLayout.SlotPose pose = PlateFoodLayout.pose(size, i, inCell);
            float scale = pose.foodPx() / 16F;
            matrices.pushPose();
            matrices.translate(pose.cx() / 16.0, pose.y() / 16.0, pose.cz() / 16.0);
            matrices.mulPose(Axis.ZP.rotationDegrees(pose.slopeDeg()));
            matrices.mulPose(Axis.YP.rotationDegrees(pose.spinDeg()));
            matrices.mulPose(Axis.XP.rotationDegrees(90F));
            matrices.scale(scale, scale, scale);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                matrices, consumers, world, seed + i);
            matrices.popPose();
        }
        matrices.popPose();
    }
}
