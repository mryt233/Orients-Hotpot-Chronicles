package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 盘子里的菜怎么摆（摆在桌上的盘子、手里 / 物品栏里的盘子共用，查看器 animations.js 的 buildPlates 是同一份算法）。
 *
 * <ul>
 *   <li>除了小碟，所有盘子里的菜一样大（4 像素见方），每格最多 3 样；先把每格第一样铺满，再往上加（r89）。</li>
 *   <li>小碟：内腔只有 4×4 像素，菜缩到 3.0 像素见方并保持居中，每往上一样顺时针多转 12°；
 *       1～3 层转完都在内腔以内，不会戳出 5×5 盘沿（r135b）。</li>
 *   <li>碗：正叠，每往上一样从上往下看顺时针多转 15°（r90）。</li>
 *   <li>其他盘子：压半面——同一格的几样沿盘子长边错开半个食物，后一样一头搭在前一样上、另一头落到盘底，
 *       往下斜；靠两头的收在盘沿以内（r90）。</li>
 *   <li>每样只比下面那样高 0.15 像素（物品模型厚 0.25 像素），允许稍微嵌进去一点但不会穿透，斜度也很缓（r90 用户反馈）。</li>
 * </ul>
 * 坐标是以长边沿 x 的盘子、盘子中心在 (8, 8) 像素算的；axis = Z 时整体转 90°（和 blockstate 的 y: 90 一样）。
 * 作者：mryt233 */
public final class PlateFoodRenderer {
    private static final float FOOD = 4F;
    private static final float RISE = 0.15F;

    /**
     * 小碟内腔只有 4×4 像素，4 像素见方的菜一转到第 3 层，对角线就到 5.66 像素、整个顶出 5×5 盘沿了；
     * 缩到 3.0 像素见方，转到 24° 时外接半宽 1.98 像素，正好留在内腔（±2 像素）以内（r135b）。
     */
    private static final float SMALL_DISH_FOOD = 3.0F;
    /** 小碟正叠时每往上一样从上往下看顺时针多转的角度；比碗的 15° 再收 3°，转角小一点才镇得住小盘沿。 */
    private static final float SMALL_DISH_SPIN = -12F;

    private PlateFoodRenderer() {
    }

    /** 画一个盘子里的菜；调用前矩阵在「盘子所在方块」的坐标系（0..1），盘子中心在方块中心、盘底在 y=0。 */
    public static void render(PlateSize size, Direction.Axis axis, List<ItemStack> items, PoseStack matrices,
                              MultiBufferSource consumers, int light, Level world, int seed,
                              ItemRenderer itemRenderer) {
        int cells = size.cells();
        double innerL = size.length() - 2 * PlateSize.RIM_T;
        double x0 = 8 - innerL / 2;
        double x1 = 8 + innerL / 2;
        // 小碟（1x1）单独缩一号，碗和其他大盘子照旧（mryt233 改，2026-11-05）
        boolean smallDish = size == PlateSize.S1X1;
        float food = smallDish ? SMALL_DISH_FOOD : FOOD;
        float scale = food / 16F;
        float tilt = (float) Math.toDegrees(Math.atan2(RISE, FOOD / 2));
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
            int layer = size.layer(i);
            int inCell = 0;
            for (int j = cell; j < items.size(); j += cells) {
                if (!items.get(j).isEmpty()) {
                    inCell++;
                }
            }
            double[] c = size.cellCenter(i);
            double cx = c[0];
            double cz = c[1];
            double y = PlateSize.FLOOR + 0.002;
            float spin = 0F;
            float slope = 0F;
            if (smallDish) {   // 小碟：正叠、层间居中（mryt233 改，2026-11-05）
                y += layer * RISE;
                spin = SMALL_DISH_SPIN * layer;
            } else if (size.piles()) {   // 碗：正叠
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
            matrices.pushPose();
            matrices.translate(cx / 16.0, y / 16.0 + layer * 0.001, cz / 16.0);
            matrices.mulPose(Axis.ZP.rotationDegrees(slope));
            matrices.mulPose(Axis.YP.rotationDegrees(spin));
            matrices.mulPose(Axis.XP.rotationDegrees(90F));
            matrices.scale(scale, scale, scale);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                matrices, consumers, world, seed + i);
            matrices.popPose();
        }
        matrices.popPose();
    }
}
