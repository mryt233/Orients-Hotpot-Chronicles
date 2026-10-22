package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.List;

/**
 * 盘子里的菜怎么摆（摆在桌上的盘子、手里 / 物品栏里的盘子共用，查看器 animations.js 的 buildPlates 是同一份算法）。
 *
 * <ul>
 *   <li>所有盘子里的菜一样大（4 像素见方），每格最多 3 样；先把每格第一样铺满，再往上加（r89）。</li>
 *   <li>小碟：正叠，每往上一样从上往下看顺时针多转 15°（r90）。</li>
 *   <li>其他盘子：压半面——同一格的几样沿盘子长边错开半个食物，后一样一头搭在前一样上、另一头落到盘底，
 *       往下斜；靠两头的收在盘沿以内（r90）。</li>
 *   <li>每样只比下面那样高 0.15 像素（物品模型厚 0.25 像素），允许稍微嵌进去一点但不会穿透，斜度也很缓（r90 用户反馈）。</li>
 * </ul>
 * 坐标是以长边沿 x 的盘子、盘子中心在 (8, 8) 像素算的；axis = Z 时整体转 90°（和 blockstate 的 y: 90 一样）。
 *
 */
public final class PlateFoodRenderer {
    private static final float FOOD = 4F;
    private static final float RISE = 0.15F;

    private PlateFoodRenderer() {
    }

    /** 画一个盘子里的菜；调用前矩阵在「盘子所在方块」的坐标系（0..1），盘子中心在方块中心、盘底在 y=0。 */
    public static void render(PlateSize size, Direction.Axis axis, List<ItemStack> items, MatrixStack matrices,
                              VertexConsumerProvider consumers, int light, World world, int seed,
                              ItemRenderer itemRenderer) {
        int cells = size.cells();
        double innerL = size.length() - 2 * PlateSize.RIM_T;
        double x0 = 8 - innerL / 2;
        double x1 = 8 + innerL / 2;
        float scale = FOOD / 16F;
        float tilt = (float) Math.toDegrees(Math.atan2(RISE, FOOD / 2));
        matrices.push();
        if (axis == Direction.Axis.Z) {
            matrices.translate(0.5, 0, 0.5);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90F));
            matrices.translate(-0.5, 0, -0.5);
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int cell = i % cells;
            int layer = size.layer(i);
            int inCell = 0;
            for (int j = cell; j < items.size(); j += cells) {
                if (!items.get(j).isEmpty()) {
                    inCell++;
                }
            }
            double[] c = size.cellCenter(i);
            double cx = c[0];
            double y = PlateSize.FLOOR + 0.002;
            float spin = 0F;
            float slope = 0F;
            if (size == PlateSize.S1X1) {
                y += layer * RISE;
                spin = -15F * layer;
            } else {
                cx += (layer - (inCell - 1) / 2.0) * (FOOD / 2);
                cx = Math.min(x1 - FOOD / 2, Math.max(x0 + FOOD / 2, cx));
                if (layer > 0) {
                    slope = -tilt;
                    y += layer * RISE - RISE / 2;
                }
            }
            matrices.push();
            matrices.translate(cx / 16.0, y / 16.0 + layer * 0.001, c[1] / 16.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(slope));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            matrices.scale(scale, scale, scale);
            itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, consumers, world, seed + i);
            matrices.pop();
        }
        matrices.pop();
    }
}
