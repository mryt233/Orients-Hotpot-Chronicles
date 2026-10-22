package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 盘子的大小：几列几行就能放几样食物。一格方块里能摆几个由占地决定（用户要求）：小碟 5×5 一格 5 个（四角 + 中间），
 * 长盘宽 6 一格 2 个，2x2 方盘 11×11 一格 1 个，也能混着摆。尺寸（像素）与 tools/gen_plates.py 的 SIZES 一致，
 * 长边沿方块的 x（axis=z 时整体转 90°）。
 */
public enum PlateSize {
    S1X1("1x1", 1, 1, 5, 5),
    S1X2("1x2", 2, 1, 11, 6),
    S1X3("1x3", 3, 1, 14, 6),
    S2X2("2x2", 2, 2, 11, 11),
    S1X5("1x5", 5, 1, 15, 6),
    S2X4("2x4", 4, 2, 15, 8);

    /** 盘底厚度、盘沿厚度（像素）；摆在火锅桌上整体下移的距离（桌面 11/16）。 */
    public static final double FLOOR = 1.0;
    public static final double RIM_T = 0.5;
    public static final double RIM_H = 0.75;
    public static final double TABLE_DROP = 5.0;

    private final String id;
    private final int cols;
    private final int rows;
    private final double length;
    private final double width;

    PlateSize(String id, int cols, int rows, double length, double width) {
        this.id = id;
        this.cols = cols;
        this.rows = rows;
        this.length = length;
        this.width = width;
    }

    public String id() {
        return id;
    }

    /** 每格最多放几样：都是 3（r89 其他盘子 ×3；r90 小碟也 ×3，小碟正叠、其他压半面，见 PlateFoodRenderer；）。 */
    public int layers() {
        return 3;
    }

    public int cells() {
        return cols * rows;
    }

    public int capacity() {
        return cells() * layers();
    }

    /** 第 i 样放在第几层：先把每格的第一层铺满，再往上叠。 */
    public int layer(int i) {
        return i / cells();
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public double length() {
        return length;
    }

    public double width() {
        return width;
    }

    /** 第 i 样所在格子的中心在朝向 x 的模型里的位置（像素，x、z）；第 i 样在第 i % 格数 格。 */
    public double[] cellCenter(int i) {
        i %= cells();
        double innerL = length - 2 * RIM_T;
        double innerW = width - 2 * RIM_T;
        double cx = 8 - innerL / 2 + innerL / cols * (i % cols + 0.5);
        double cz = 8 - innerW / 2 + innerW / rows * (i / cols + 0.5);
        return new double[]{cx, cz};
    }

    /** 一格的短边（像素），食物按它缩放。 */
    public double cellSize() {
        return Math.min((length - 2 * RIM_T) / cols, (width - 2 * RIM_T) / rows);
    }
}
