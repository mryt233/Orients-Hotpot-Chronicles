package cn.blockforge.generated.orientshotpotchronicles.hotpot;

/**
 * 盘子的大小：几列几行就能放几样食物。一格方块里能摆几个由占地决定（用户要求）：小碟 5×5 一格 5 个（四角 + 中间），
 * 长盘宽 6 一格 2 个，2x2 方盘 11×11 一格 1 个，也能混着摆。尺寸（像素）与 tools/gen_plates.py 的 SIZES 一致，
 * 长边沿方块的 x（axis=z 时整体转 90°）。
 */
public enum PlateSize {
    S1X1("1x1", "small_dish", 1, 1, 5, 5),
    S1X2("1x2", "two_slot_plate", 2, 1, 11, 6),
    S1X3("1x3", "three_slot_long_plate", 3, 1, 14, 6),
    S2X2("2x2", "four_slot_square_plate", 2, 2, 11, 11),
    S1X5("1x5", "five_slot_long_plate", 5, 1, 15, 6),
    S2X4("2x4", "eight_slot_platter", 4, 2, 15, 8),
    /** 原版的碗摆在桌上（r99）：敞口碗上圈 7×7，一格一样、叠 3 层，像小碟一样正叠。不注册成盘子。 */
    BOWL("bowl", "bowl", 1, 1, 7, 7),
    /** 橙子饮料瓶子（r99）：瓶身 6×5，放在桌上像盘子一样占位置；不装菜。 */
    DRINK("drink", "drink", 1, 1, 6, 5);

    /** 盘底厚度、盘沿厚度（像素）；摆在火锅桌上整体下移的距离（桌面 11/16）。 */
    public static final double FLOOR = 1.0;
    public static final double RIM_T = 0.5;
    public static final double RIM_H = 0.75;
    public static final double TABLE_DROP = 5.0;
    /** 碗高 3 像素（tools/gen_bowls.py 的 b 形），盘子是盘底 + 盘沿。 */
    public static final double BOWL_H = 3.0;
    /** 橙子饮料瓶高 15 像素（tools/gen_drink.py）。 */
    public static final double DRINK_H = 15.0;

    private final String id;
    /** 物品 ID 里材质后面那段，和显示名对得上（r102 用户要求）：小碟 small_dish、双格盘 two_slot_plate …… */
    private final String itemId;
    private final int cols;
    private final int rows;
    private final double length;
    private final double width;

    PlateSize(String id, String itemId, int cols, int rows, double length, double width) {
        this.id = id;
        this.itemId = itemId;
        this.cols = cols;
        this.rows = rows;
        this.length = length;
        this.width = width;
    }

    public String id() {
        return id;
    }

    public String itemId() {
        return itemId;
    }

    /** 每格最多放几样：都是 3（r89 其他盘子 ×3；r90 小碟也 ×3，小碟正叠、其他压半面，见 PlateFoodRenderer；调整）。 */
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

    /** 摆在桌上时多高（像素），选中框和碰撞用。 */
    public double height() {
        return this == BOWL ? BOWL_H : this == DRINK ? DRINK_H : FLOOR + RIM_H;
    }

    /** 一格只放一样、正叠（小碟和碗）；其他盘子压半面。 */
    public boolean piles() {
        return cells() == 1;
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

}
