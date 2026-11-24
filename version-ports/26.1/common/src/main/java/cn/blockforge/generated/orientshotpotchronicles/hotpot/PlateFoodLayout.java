// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 盘子与碗内食材的纯几何排布计算（与查看器 {@code animations.js} 共用同一组零穿插轻错层常数，无渲染器依赖）。
 * 作者：mryt233
 */
public final class PlateFoodLayout {
    public static final double FOOD = 4.0;
    public static final double SMALL_DISH_FOOD = 3.0;
    /** 底层底面离开盘底板面（FLOOR=1.0）的最小安全间隙，消除底面与盘底贴图共面。 */
    public static final double FLOOR_GAP = 0.02;
    /** 小碟（3 px 见方、物品厚 0.1875 px）正叠步进。 */
    public static final double PILE_STEP_SMALL = 0.22;
    /** 碗与标准正叠（4 px 见方、物品厚 0.25 px）步进。 */
    public static final double PILE_STEP_STANDARD = 0.28;
    /** 多格盘子压半面层间步进（跨格最大错层 0.12 px + 倾斜两端落差后仍有正间隙）。 */
    public static final double SHINGLE_RISE = 0.50;
    /** 同层相邻格子之间的微抬阶梯，消除横纵向（含 S1X5 端头夹紧后跨两格）同层共面。 */
    public static final double CELL_STAGGER = 0.04;
    /** 压半面微倾角正切上限。 */
    public static final double MAX_SLOPE_TAN = 0.025;
    private static final float SMALL_DISH_SPIN = -12F;
    private static final float BOWL_SPIN = -15F;

    public record SlotPose(double cx, double y, double cz, float spinDeg, float slopeDeg, float foodPx) {
    }

    private PlateFoodLayout() {
    }

    public static SlotPose pose(PlateSize size, int index, int inCell) {
        int cells = size.cells();
        int cell = index % cells;
        int col = cell % size.cols();
        int row = cell / size.cols();
        int layer = size.layer(index);
        double[] center = size.cellCenter(index);
        boolean smallDish = size == PlateSize.S1X1;
        double food = smallDish ? SMALL_DISH_FOOD : FOOD;
        double thickness = food / 16.0;

        if (smallDish) {
            double y = PlateSize.FLOOR + thickness / 2.0 + FLOOR_GAP + layer * PILE_STEP_SMALL;
            return new SlotPose(center[0], y, center[1], SMALL_DISH_SPIN * layer, 0F, (float) food);
        }
        if (size.piles()) {
            double y = PlateSize.FLOOR + thickness / 2.0 + FLOOR_GAP + layer * PILE_STEP_STANDARD;
            return new SlotPose(center[0], y, center[1], BOWL_SPIN * layer, 0F, (float) food);
        }

        double innerL = size.length() - 2 * PlateSize.RIM_T;
        double x0 = 8.0 - innerL / 2.0;
        double x1 = 8.0 + innerL / 2.0;
        double cx = clampedShingleX(center[0], layer, inCell, x0, x1);
        int cellLevel = size.rows() == 1 ? (col % 3) : ((col & 1) + 2 * (row & 1));
        double stagger = cellLevel * CELL_STAGGER;
        double y = PlateSize.FLOOR + thickness / 2.0 + FLOOR_GAP + stagger + layer * SHINGLE_RISE;
        float slope = 0F;
        if (layer > 0) {
            double prevCx = clampedShingleX(center[0], layer - 1, inCell, x0, x1);
            double actualShift = Math.max(0.0, cx - prevCx);
            slope = (float) -Math.toDegrees(Math.atan(MAX_SLOPE_TAN * Math.min(1.0, actualShift / (FOOD / 2.0))));
        }
        return new SlotPose(cx, y, center[1], 0F, slope, (float) food);
    }

    private static double clampedShingleX(double baseCx, int layer, int inCell, double x0, double x1) {
        double raw = baseCx + (layer - (inCell - 1) / 2.0) * (FOOD / 2.0);
        return Math.min(x1 - FOOD / 2.0, Math.max(x0 + FOOD / 2.0, raw));
    }
}
