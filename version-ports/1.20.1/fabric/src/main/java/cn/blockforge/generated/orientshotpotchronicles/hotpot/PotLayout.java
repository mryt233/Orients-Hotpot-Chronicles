package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import java.util.ArrayList;
import java.util.List;

/**
 * 锅里怎么分格（2026-10-23 鸳鸯锅 / 田字锅，用户定；）。
 *
 * <p>后台仍是 81 格（9 个大格 × 每格 3x3 小格），格局只决定：锅分几边、每边一种汤（{@link #sides}）、
 * 每个大格属于哪边（{@link #sideOfCell}），在锅里画在哪（{@link #cellCenter}），准星点在哪一格 / 哪一边
 * （{@link #cellAt} / {@link #sideAt}），以及汤面按哪几块画（{@link #regions}）。</p>
 * <ul>
 *   <li>九宫格锅：1 边，9 格。</li>
 *   <li>S 形鸳鸯锅（太极）：2 边，每边 4 格；O 形（中间一圈）：圈外 6 格、圈里 2 格；正中那格不用。</li>
 *   <li>方心鸳鸯锅：和 O 形一样，中间那圈是正方形（2026-10-25）。</li>
 *   <li>直隔板鸳鸯锅：中间一道直隔板，左右各 3 格（中间一列不用）。</li>
 *   <li>田字锅：十字隔板分四块，每块一种汤、2 格（正中那格不用）。</li>
 * </ul>
 * <p>以后加别的分法：加一个枚举值，补 sides / 每格归属 / 每格位置 / {@link #sideAt} 的形状，
 * tools/gen_yuanyang.py 的 classify 同步。汤底不和格局绑定，任何汤底都能倒进任何一边。</p>
 *
 * <p>坐标一律是「模型坐标」里锅腔的比例 0..1（u 向东、v 向南）；锅按 axis=z 摆时模型绕 y 转了 90°，
 * 用 {@link #toModel} / {@link #toWorld} 换算。</p>
 */
public enum PotLayout {
    NINE_GRID(1, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0}, null),
    YUANYANG_S(2, new int[]{0, 0, 1, 0, -1, 1, 0, 1, 1}, new double[][]{
        {2.6, 2.4}, {7.8, 3.0}, {11.1, 2.4},
        {2.4, 6.6}, {6.5, 6.5}, {10.6, 6.4},
        {1.9, 10.6}, {5.2, 10.0}, {10.4, 10.6}}),
    YUANYANG_O(2, new int[]{0, 0, 0, 1, -1, 1, 0, 0, 0}, new double[][]{
        {2.2, 2.2}, {6.5, 1.6}, {10.8, 2.2},
        {5.0, 6.5}, {6.5, 6.5}, {8.0, 6.5},
        {2.2, 10.8}, {6.5, 11.4}, {10.8, 10.8}}),
    /** 方心鸳鸯锅（2026-10-25 用户：和 O 形锅一样，中间那圈换成正方形）：圈外 6 格、方格里 2 格，正中那格不用。 */
    YUANYANG_SQUARE(2, new int[]{0, 0, 0, 1, -1, 1, 0, 0, 0}, new double[][]{
        {2.2, 2.2}, {6.5, 1.6}, {10.8, 2.2},
        {5.0, 6.5}, {6.5, 6.5}, {8.0, 6.5},
        {2.2, 10.8}, {6.5, 11.4}, {10.8, 10.8}}),
    SPLIT(2, new int[]{0, -1, 1, 0, -1, 1, 0, -1, 1}, new double[][]{
        {3.25, 2.2}, {6.5, 2.2}, {9.75, 2.2},
        {3.25, 6.5}, {6.5, 6.5}, {9.75, 6.5},
        {3.25, 10.8}, {6.5, 10.8}, {9.75, 10.8}}),
    /** 田字：左上 A、右上 B、右下 C、左下 D，每块 2 格（大格按风车分）。 */
    FOUR_GRID(4, new int[]{0, 0, 1, 3, -1, 1, 3, 2, 2}, new double[][]{
        {1.9, 3.25}, {4.6, 3.25}, {9.75, 1.9},
        {3.25, 8.4}, {6.5, 6.5}, {9.75, 4.6},
        {3.25, 11.1}, {8.4, 9.75}, {11.1, 9.75}});

    /** 最多几边（田字 4 边）。方块实体按这个留汤底位置。 */
    public static final int MAX_SIDES = 4;
    public static final int SIDE_A = 0;
    public static final int SIDE_B = 1;
    /** 不用的大格 / 隔板上。 */
    public static final int NONE = -1;

    private static final double INNER_PX = 13.0;
    private static final double HALF_WALL = 0.5;
    private static final double RING = 3.4;
    /** 方心锅中间方格的半边长（像素）：6×6 的方格，面积和 O 形锅的圆差不多。 */
    private static final double SQUARE = 3.0;

    private final int sides;
    private final int[] cellSides;
    private final double[][] centersPx;

    PotLayout(int sides, int[] cellSides, double[][] centersPx) {
        this.sides = sides;
        this.cellSides = cellSides;
        this.centersPx = centersPx;
    }

    /** 锅分几边（每边一种汤）。 */
    public int sides() {
        return sides;
    }

    /** 不止一种汤（鸳鸯锅、田字锅）。 */
    public boolean multiSoup() {
        return sides > 1;
    }

    /** 大格属于哪边汤；{@link #NONE} = 这一格不用。 */
    public int sideOfCell(int bigCell) {
        return bigCell < 0 || bigCell > 8 ? NONE : cellSides[bigCell];
    }

    public boolean usesCell(int bigCell) {
        return sideOfCell(bigCell) != NONE;
    }

    /** 81 格里的一格属于哪个大格。 */
    public static int cellOfSlot(int slot) {
        return (slot / 9 / 3) * 3 + (slot % 9) / 3;
    }

    /**
     * 81 格里的一格属于哪边汤（{@link #NONE} = 这一格不用）。九宫格锅按大格；鸳鸯 / 田字锅按界面上这一小格
     * 整个落在哪边——界面像九宫格那样一格格排满锅，压到隔板上的格子空着不放（用户 2026-10-24 定）。
     */
    public int sideOfSlot(int slot) {
        if (this == NINE_GRID) {
            return sideOfCell(cellOfSlot(slot));
        }
        ensureSlots();
        return slot < 0 || slot >= 81 ? NONE : slotSides[slot];
    }

    /** 这一格的东西在锅里画在哪个位置（大格编号，见 {@link #cellCenter}）：九宫格按大格，其他按同边最近的位置。 */
    public int spotOfSlot(int slot) {
        if (this == NINE_GRID) {
            return cellOfSlot(slot);
        }
        ensureSlots();
        return slot < 0 || slot >= 81 ? NONE : slotSpots[slot];
    }

    /** 鸳鸯 / 田字锅界面：9×9 小格（每格 18 像素）均匀排在 170 像素的锅面里，四周各留这么多像素。和 HotPotScreenHandler 一致。 */
    public static final int GUI_INSET = 4;
    public static final int GUI_SIZE = 170;

    private int[] slotSides;
    private int[] slotSpots;

    private synchronized void ensureSlots() {
        if (slotSides != null) {
            return;
        }
        int[] sides = new int[81];
        int[] spots = new int[81];
        for (int slot = 0; slot < 81; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            double x0 = (GUI_INSET + col * 18 + 1) / (double) GUI_SIZE;
            double y0 = (GUI_INSET + row * 18 + 1) / (double) GUI_SIZE;
            double span = 16 / (double) GUI_SIZE;
            int side = sideAt(x0, y0);
            // 整格（取 5×5 个点）都在同一边才用；碰到隔板或跨两边就空着
            for (int i = 0; i <= 4 && side != NONE; i++) {
                for (int j = 0; j <= 4; j++) {
                    if (sideAt(x0 + span * i / 4, y0 + span * j / 4) != side) {
                        side = NONE;
                        break;
                    }
                }
            }
            sides[slot] = side;
            int best = NONE;
            if (side != NONE) {
                double cu = x0 + span / 2;
                double cv = y0 + span / 2;
                double bestD = Double.MAX_VALUE;
                for (int cell = 0; cell < 9; cell++) {
                    if (sideOfCell(cell) != side) {
                        continue;
                    }
                    double[] c = cellCenter(cell);
                    double d = (c[0] - cu) * (c[0] - cu) + (c[1] - cv) * (c[1] - cv);
                    if (d < bestD) {
                        bestD = d;
                        best = cell;
                    }
                }
            }
            spots[slot] = best;
        }
        slotSpots = spots;
        slotSides = sides;
    }

    /** 大格中心（锅腔比例 0..1，模型坐标）。 */
    public double[] cellCenter(int bigCell) {
        if (centersPx == null) {
            return new double[]{(bigCell % 3 + 0.5) / 3.0, (bigCell / 3 + 0.5) / 3.0};
        }
        double[] px = centersPx[bigCell];
        return new double[]{px[0] / INNER_PX, px[1] / INNER_PX};
    }

    /** 锅腔里一点属于哪边（模型坐标比例）；隔板上返回 {@link #NONE}。和 gen_yuanyang.py 的 classify 一致。 */
    public int sideAt(double u, double v) {
        double x = u * INNER_PX;
        double z = v * INNER_PX;
        double c = INNER_PX / 2;
        switch (this) {
            case NINE_GRID:
                return SIDE_A;
            case SPLIT:
                return Math.abs(x - c) < HALF_WALL ? NONE : (x < c ? SIDE_A : SIDE_B);
            case FOUR_GRID:
                if (Math.abs(x - c) < HALF_WALL || Math.abs(z - c) < HALF_WALL) {
                    return NONE;
                }
                return z < c ? (x < c ? 0 : 1) : (x < c ? 3 : 2);
            case YUANYANG_SQUARE: {
                double d = Math.max(Math.abs(x - c), Math.abs(z - c));
                if (Math.abs(d - SQUARE) < HALF_WALL) {
                    return NONE;
                }
                return d < SQUARE ? SIDE_B : SIDE_A;
            }
            case YUANYANG_O: {
                double d = Math.hypot(x - c, z - c);
                if (Math.abs(d - RING) < HALF_WALL) {
                    return NONE;
                }
                return d < RING ? SIDE_B : SIDE_A;
            }
            default: {
                // S：上半段是以 (c, r) 为圆心的右半圆，下半段是以 (c, c + r) 为圆心的左半圆，太极那样
                double r = c / 2;
                double dUp = x >= c ? Math.abs(Math.hypot(x - c, z - r) - r) : Math.hypot(x - c, Math.min(Math.abs(z), Math.abs(z - c)));
                double dDown = x <= c ? Math.abs(Math.hypot(x - c, z - c - r) - r) : Math.hypot(x - c, Math.min(Math.abs(z - c), Math.abs(z - 2 * c)));
                if (Math.min(dUp, dDown) < HALF_WALL) {
                    return NONE;
                }
                boolean left = z < c
                    ? x < c + Math.sqrt(Math.max(0, r * r - (z - r) * (z - r)))
                    : x < c - Math.sqrt(Math.max(0, r * r - (z - c - r) * (z - c - r)));
                return left ? SIDE_A : SIDE_B;
            }
        }
    }

    /** 准星点在哪个大格（模型坐标比例）：九宫格按 3x3；其他先看在哪边，再找那边最近的一格；点在隔板上就找最近的一格。 */
    public int cellAt(double u, double v) {
        if (this == NINE_GRID) {
            int col = Math.max(0, Math.min(2, (int) Math.floor(u * 3)));
            int row = Math.max(0, Math.min(2, (int) Math.floor(v * 3)));
            return row * 3 + col;
        }
        int side = sideAt(u, v);
        int best = NONE;
        double bestD = Double.MAX_VALUE;
        for (int cell = 0; cell < 9; cell++) {
            int s = sideOfCell(cell);
            if (s == NONE || (side != NONE && s != side)) {
                continue;
            }
            double[] c = cellCenter(cell);
            double d = (c[0] - u) * (c[0] - u) + (c[1] - v) * (c[1] - v);
            if (d < bestD) {
                bestD = d;
                best = cell;
            }
        }
        return best;
    }

    /** 准星点在哪边：点在隔板上就算最近那一格的那边。 */
    public int sideNear(double u, double v) {
        int side = sideAt(u, v);
        return side != NONE ? side : sideOfCell(cellAt(u, v));
    }

    /** 世界里锅腔的比例坐标 → 模型坐标（锅按 axis=z 摆时模型绕 y 转了 90°）。 */
    public static double[] toModel(double u, double v, boolean rotated) {
        return rotated ? new double[]{v, 1 - u} : new double[]{u, v};
    }

    /** 模型坐标 → 世界里锅腔的比例坐标。 */
    public static double[] toWorld(double u, double v, boolean rotated) {
        return rotated ? new double[]{1 - v, u} : new double[]{u, v};
    }

    private List<float[]>[] regionCache;

    /**
     * 一边汤面由哪些长方块拼成（模型坐标比例 {u0, v0, u1, v1}）：按半像素格子分类、一行一行合并，
     * 和 gen_yuanyang.py 生成隔板用的格子同一套。
     */
    @SuppressWarnings("unchecked")
    public synchronized List<float[]> regions(int side) {
        if (regionCache == null) {
            regionCache = new List[MAX_SIDES];
            for (int i = 0; i < MAX_SIDES; i++) {
                regionCache[i] = new ArrayList<>();
            }
            int n = (int) (INNER_PX * 2);
            for (int j = 0; j < n; j++) {
                int i = 0;
                while (i < n) {
                    int s = sideAt((i + 0.5) / n, (j + 0.5) / n);
                    int k = i;
                    while (k < n && sideAt((k + 0.5) / n, (j + 0.5) / n) == s) {
                        k++;
                    }
                    if (s != NONE) {
                        regionCache[s].add(new float[]{(float) i / n, (float) j / n, (float) k / n, (float) (j + 1) / n});
                    }
                    i = k;
                }
            }
        }
        return regionCache[side];
    }
}
