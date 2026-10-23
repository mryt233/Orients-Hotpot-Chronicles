package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 灶膛界面：像熔炉一样只有一个燃料槽，左边是砌砖灶膛。不显示温度和剩余秒数，火的状态全靠画面：
 * 在烧是火舌，有燃料没点火是一点明灭的余烬，什么都没有就只剩炉灰。
 * 火苗是几簇各自跳动的火舌（多层正弦叠加，非线性闪烁），火舌逐行收窄、尖端摆动；
 * 炭床明暗起伏，火星缓出上飘渐隐。燃料槽下方的火焰图标像原版熔炉一样随燃料烧尽。
 * 只负责显示，燃料能否放入、烧多久都由 {@link StoveScreenHandler} 决定。
 * 网页预览 viewer/screens.js 的 stove 与本类一一对应，改这里要同步改那边。
 */
public class StoveScreen extends HandledScreen<StoveScreenHandler> {
    // 灶膛砖面直接用原版砖块贴图，和灶台方块一致
    private static final Identifier STOVE_SIDE = Identifier.ofVanilla("textures/block/bricks.png");

    private static final int W = 176;
    private static final int H = 166;
    // 灶膛（相对面板）
    // 灶膛 + 箭头 + 燃料槽作为一组，在面板里水平居中
    private static final int HX = 44;
    private static final int HY = 23;
    private static final int HW = 52;
    private static final int HH = 46;
    private static final int INV_X = 8;
    private static final int INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    /** 14x14 火焰图标：'#' 外焰，'o' 内焰。 */
    private static final String[] FLAME = {
        "      #       ",
        "      ##      ",
        "     ###      ",
        "     ###  #   ",
        "  #  ####  #  ",
        "  # #####  ## ",
        " ## ####o# ## ",
        " ######oo#### ",
        " #####ooo#### ",
        "######oooo####",
        "#####oooooo###",
        "####oooooooo##",
        " ###oooooooo# ",
        "  ##########  ",
    };

    /** 三层火舌：高度倍率、最大半宽、颜色（外焰 → 焰心）。 */
    private static final double[][] FLAME_LAYERS = {{1, 3.2}, {0.7, 2.1}, {0.42, 1.1}};
    private static final int[] FLAME_COLORS = {0xFFD8401F, 0xFFF08A2C, 0xFFFBE08A};

    public StoveScreen(StoveScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.translatable(HotPotBlockEntity.key("screen.stove")));
        this.backgroundWidth = W;
        this.backgroundHeight = H;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        double t = Util.getMeasuringTimeMs() / 1000.0;
        context.getMatrices().push();
        context.getMatrices().translate(this.x, this.y, 0);

        frame(context, W, H, 3, false);
        drawHearth(context, t);

        int fx = StoveScreenHandler.FUEL_X;
        int fy = StoveScreenHandler.FUEL_Y;
        context.fill(fx - 4, fy - 4, fx + 20, fy + 20, RIM_LINE);
        context.fill(fx - 3, fy - 3, fx + 19, fy + 19, BRASS_DARK);
        context.fill(fx - 2, fy - 2, fx + 18, fy + 18, BRASS);
        context.fill(fx - 2, fy - 2, fx + 18, fy - 1, BRASS_HI);
        slotFrame(context, fx, fy);
        // 燃料槽 → 灶膛的黄铜箭头（朝左）
        int ax = HX + HW + 3;
        int ay = 39;
        context.fill(ax + 5, ay + 1, ax + 8, ay + 8, BRASS);
        context.fill(ax + 3, ay + 3, ax + 8, ay + 6, BRASS_HI);
        context.fill(ax + 1, ay + 4, ax + 8, ay + 5, BRASS_HI);
        drawFlameGauge(context, fx + 1, fy + 22, handler.burnTicks() > 0 ? handler.burnRatio() : 0);
        inventoryFrames(context, INV_X, INV_Y, HOTBAR_Y);

        context.getMatrices().pop();
    }

    private void drawHearth(DrawContext c, double t) {
        // 砖面铺灶台侧面贴图（16x16 整张），和方块本身对得上
        for (int ty = 0; ty < HH; ty += 16) {
            for (int tx = 0; tx < HW; tx += 16) {
                c.drawTexture(STOVE_SIDE, HX + tx, HY + ty, 0, 0, Math.min(16, HW - tx), Math.min(16, HH - ty), 16, 16);
            }
        }
        c.fill(HX - 1, HY - 1, HX + HW + 1, HY, RIM_LINE);
        c.fill(HX - 1, HY + HH, HX + HW + 1, HY + HH + 1, RIM_LINE);
        c.fill(HX - 1, HY, HX, HY + HH, RIM_LINE);
        c.fill(HX + HW, HY, HX + HW + 1, HY + HH, RIM_LINE);

        int ax = HX + 8;
        int ay = HY + 9;
        int aw = HW - 16;
        int ah = HH - 12;
        int bottom = ay + ah;
        int mouth = 0xFF0A0503;
        c.fill(ax + 6, ay, ax + aw - 6, bottom, mouth);
        c.fill(ax + 3, ay + 3, ax + aw - 3, bottom, mouth);
        c.fill(ax, ay + 8, ax + aw, bottom, mouth);

        boolean burning = handler.burnTicks() > 0;
        if (!burning) {
            // 冷灶：灰白炉灰；有燃料等点火时一点余烬缓缓明灭
            c.fill(ax + 3, bottom - 3, ax + aw - 3, bottom, 0xFF2E2A26);
            for (int k = 0; k < 6; k++) {
                c.fill(ax + 5 + k * 5, bottom - 3 + k % 2, ax + 7 + k * 5, bottom - 2 + k % 2, 0xFF4A443D);
            }
            if (handler.hasFuelInSlot()) {
                double u = frac(t / 2.4);
                double a = 0.25 + 0.35 * easeInOutSine(u < 0.5 ? u * 2 : 2 - u * 2);
                c.fill(ax + 14, bottom - 3, ax + 22, bottom - 1, alpha(0xFFB8361C, a));
            }
            return;
        }
        float ratio = handler.burnRatio();
        double heat = 0.3 + 0.7 * easeOutQuad(ratio);
        // 火光映在砖面上
        c.fill(HX, HY, HX + HW, HY + HH, alpha(0xFFFF8A2A, 0.07 + 0.05 * Math.sin(t * 2.3)));
        c.fill(ax, ay + 8, ax + aw, bottom, alpha(0xFF5A1606, 0.9));
        c.fill(ax + 3, ay + 3, ax + aw - 3, bottom, alpha(0xFF5A1606, 0.5));
        // 炭床
        int base = bottom - 3;
        for (int k = 0; k < 10; k++) {
            int x = ax + 2 + k * (aw - 4) / 10;
            double glow = 0.5 + 0.5 * Math.sin(t * 2.1 + k * 1.3);
            c.fill(x, base, x + 3, bottom, mix(0xFFB8361C, 0xFFF0A23C, glow));
            if (k % 3 == 1) {
                c.fill(x + 1, base + 1, x + 2, base + 2, mix(0xFFF0A23C, 0xFFFBE08A, glow));
            }
        }
        // 火舌：每簇三层嵌套的水滴形，逐行收窄，越往上摆得越厉害
        double maxH = (base - ay - 2) * heat;
        for (int layer = 0; layer < FLAME_LAYERS.length; layer++) {
            double hMul = FLAME_LAYERS[layer][0];
            double wMax = FLAME_LAYERS[layer][1];
            int color = FLAME_COLORS[layer];
            for (int j = 0; j < 5; j++) {
                int cx = ax + 6 + j * 6;
                double n = 0.5 + 0.5 * (0.55 * Math.sin(t * 6.3 + j * 1.9)
                    + 0.3 * Math.sin(t * 11.7 + j * 0.7) + 0.15 * Math.sin(t * 23.1 + j * 2.3));
                double edge = j == 0 || j == 4 ? 0.62 : j == 2 ? 1 : 0.85;
                int hj = (int) Math.round(maxH * (0.55 + 0.45 * n) * edge * hMul);
                double sway = Math.sin(t * 4.7 + j * 2.1);
                for (int yy = 0; yy < hj; yy++) {
                    double f = yy / (double) hj;
                    double half = wMax * (1 - Math.pow(f, 1.7));
                    double xc = cx + sway * 1.6 * f * f;
                    int y = base - yy - 1;
                    int x0 = (int) Math.round(xc - half);
                    int x1 = (int) Math.round(xc + half) + 1;
                    if (y <= archTop(x0, ax, ay, aw) || x1 <= x0) {
                        continue;
                    }
                    c.fill(Math.max(ax, x0), y, Math.min(ax + aw, x1), y + 1, color);
                }
            }
        }
        // 火星：缓出上飘、渐隐
        for (int i = 0; i < 6; i++) {
            double life = (0.9 + hash(i, 7) * 0.7) / (0.6 + 0.6 * ratio);
            int cyc = (int) Math.floor(t / life + hash(i, 3));
            double u = frac(t / life + hash(i, 3));
            int x0 = ax + 6 + (int) Math.floor(hash(i, cyc) * (aw - 12));
            int y = (int) Math.round(base - 2 - (base - ay - 4) * easeOutCubic(u));
            int x = x0 + (int) Math.round(Math.sin(u * 5 + i) * 2);
            if (y < archTop(x, ax, ay, aw) + 1) {
                continue;
            }
            c.fill(x, y, x + 1, y + 1, alpha(0xFFFBE08A, Math.pow(1 - u, 1.5)));
        }
    }

    /** 拱形炉口在横坐标 x 处的顶边。 */
    private static int archTop(int x, int ax, int ay, int aw) {
        if (x >= ax + 6 && x < ax + aw - 6) {
            return ay;
        }
        if (x >= ax + 3 && x < ax + aw - 3) {
            return ay + 3;
        }
        return ay + 8;
    }

    /** 原版熔炉式火焰：燃料越少，从上往下熄掉越多。 */
    private static void drawFlameGauge(DrawContext c, int x, int y, float ratio) {
        int lit = Math.round(14 * ratio);
        for (int ry = 0; ry < 14; ry++) {
            String row = FLAME[ry];
            for (int rx = 0; rx < 14; rx++) {
                char ch = row.charAt(rx);
                if (ch == ' ') {
                    continue;
                }
                boolean on = ry >= 14 - lit;
                c.fill(x + rx, y + ry, x + rx + 1, y + ry + 1,
                    on ? (ch == 'o' ? 0xFFFBE08A : 0xFFE2582B) : 0xFF24160A);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title, 12, 7, TITLE, true);
        context.fill(12, 17, W - 12, 18, BRASS_DARK);

        context.drawText(textRenderer, Text.translatable(HotPotBlockEntity.key("screen.inventory")),
            INV_X, INV_Y - 11, LABEL, true);
    }

    /** 空燃料槽上悬停：说明能放什么（原先常驻的提示行挪到这里，给背包标签腾位置）。 */
    /** 原版 HandledScreen.render 不画悬停提示，要子类自己调（漏了这步，格子里的东西指上去看不到名字和属性；2026-10-23 用户报，）。 */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        Slot focused = this.focusedSlot;
        if (focused == null || focused.id != 0 || focused.hasStack() || !handler.getCursorStack().isEmpty()) {
            super.drawMouseoverTooltip(context, mouseX, mouseY);
            return;
        }
        List<OrderedText> lines = new ArrayList<>();
        lines.add(Text.translatable(HotPotBlockEntity.key("screen.stove.fuel")).formatted(Formatting.GOLD).asOrderedText());
        lines.addAll(textRenderer.wrapLines(
            Text.translatable(HotPotBlockEntity.key("screen.stove.hint")).formatted(Formatting.GRAY), 140));
        if (handler.burnTicks() <= 0 && !handler.hasFuelInSlot()) {
            lines.addAll(textRenderer.wrapLines(
                Text.translatable(HotPotBlockEntity.key("screen.stove.waiting")).formatted(Formatting.DARK_GRAY), 140));
        }
        context.drawOrderedTooltip(textRenderer, lines, mouseX, mouseY);
    }
}
