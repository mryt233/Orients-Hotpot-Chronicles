package cn.blockforge.generated.orientshotpotchronicles.client;

import net.minecraft.client.gui.DrawContext;

/**
 * 火锅 / 灶膛界面共用的画框、槽位、缓动与随机工具。
 * 网页预览 viewer/screens.js 里有同名的一一对应实现，改这里要同步改那边。
 */
final class GuiKit {
    static final int RIM_LINE = 0xFF140A04;
    static final int BRASS_DARK = 0xFF5A340E;
    static final int BRASS = 0xFFB47A1C;
    static final int BRASS_HI = 0xFFF0C459;
    static final int COPPER_DARK = 0xFF7A3A12;
    static final int COPPER = 0xFFC97A2A;
    static final int COPPER_SHADE = 0xFF6E3A12;
    static final int WOOD = 0xFF3A2310;
    static final int WOOD_SEAM = 0xFF1B0F05;
    static final int RIVET = 0xFFFFE39A;
    static final int INNER_SHADOW = 0xFF120704;
    static final int SLOT_SHADOW = 0xFF140B05;
    static final int SLOT_LIGHT = 0xFF6B4524;
    static final int SLOT_BG = 0xFF2A190C;
    static final int LABEL = 0xFFE8C88A;
    static final int TITLE = 0xFFF0D9A8;

    private GuiKit() {
    }

    // ------------------------------------------------------------------ 画框

    /** 黄铜包边 + 深色木板面板，左上角为 (0,0)。 */
    static void frame(DrawContext c, int w, int h, int seams, boolean bottomRivets) {
        c.fill(0, 0, w, h, RIM_LINE);
        c.fill(1, 1, w - 1, h - 1, BRASS_DARK);
        c.fill(2, 2, w - 2, h - 2, BRASS);
        c.fill(2, 2, w - 2, 5, BRASS_HI);
        c.fill(2, h - 5, w - 2, h - 2, COPPER_SHADE);
        c.fill(3, 3, w - 3, h - 3, WOOD);
        for (int i = 1; i <= seams; i++) {
            int ly = 3 + i * (h - 6) / (seams + 1);
            c.fill(3, ly, w - 3, ly + 1, WOOD_SEAM);
            c.fill(3, ly + 1, w - 3, ly + 2, 0x14FFFFFF);
        }
        rivet(c, 6, 6);
        rivet(c, w - 10, 6);
        if (bottomRivets) {
            rivet(c, 6, h - 10);
            rivet(c, w - 10, h - 10);
        }
    }

    static void rivet(DrawContext c, int x, int y) {
        c.fill(x, y, x + 4, y + 4, BRASS_DARK);
        c.fill(x, y, x + 4, y + 1, BRASS_HI);
        c.fill(x + 1, y + 1, x + 3, y + 3, RIVET);
    }

    /** 物品坐标 (x, y) 外围的 18x18 凹槽：左上压暗、右下透亮。 */
    static void slotFrame(DrawContext c, int x, int y) {
        c.fill(x - 1, y - 1, x + 17, y + 17, SLOT_LIGHT);
        c.fill(x - 1, y - 1, x + 16, y + 16, SLOT_SHADOW);
        c.fill(x, y, x + 16, y + 16, SLOT_BG);
    }

    static void inventoryFrames(DrawContext c, int x, int y, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(c, x + col * 18, y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotFrame(c, x + col * 18, hotbarY);
        }
    }

    // ------------------------------------------------------------------ 颜色

    private static int clamp255(double v) {
        return Math.max(0, Math.min(255, (int) v));
    }

    static int shade(int color, double f) {
        return (color & 0xFF000000)
            | clamp255(((color >> 16) & 0xFF) * f) << 16
            | clamp255(((color >> 8) & 0xFF) * f) << 8
            | clamp255((color & 0xFF) * f);
    }

    static int alpha(int color, double f) {
        return clamp255(((color >>> 24) & 0xFF) * f) << 24 | (color & 0xFFFFFF);
    }

    static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    // ------------------------------------------------------------------ 动画

    static double frac(double x) {
        return x - Math.floor(x);
    }

    static double easeOutCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }

    static double easeOutQuad(double x) {
        return 1 - (1 - x) * (1 - x);
    }

    static double easeInOutSine(double x) {
        return -(Math.cos(Math.PI * x) - 1) / 2;
    }

    static double easeOutBack(double x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    /** 整数哈希 → [0,1)。纯函数，动画不需要逐帧状态；与网页预览逐位一致。 */
    static double hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFFFFL) / 4294967296.0;
    }
}
