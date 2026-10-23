package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotMaterial;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Locale;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 筷子视角的火锅屏幕（横版）：左边是俯视的九宫格铜锅，右边是状态卡片和背包。
 * 高 212，1080p 默认界面缩放下也能完整显示。
 *
 * 汤面动画都是时间的纯函数，全部走缓动曲线：
 * 气泡原地鼓起（easeOutBack）再破成一圈渐隐的涟漪（easeOutCubic），火越旺越密；
 * 配料随对流慢慢打转；高光斑淡入淡出；蒸汽上飘散开。
 * 网页预览 viewer/screens.js 的 hotpot 与本类一一对应，改这里要同步改那边。
 */
public class HotPotScreen extends HandledScreen<HotPotScreenHandler> {
    private static final int W = 368;
    private static final int H = 212;
    private static final int PANEL_X = 196;
    private static final int GW = 9 * 18 + 2 * HotPotScreenHandler.GAP; // 170

    private static final int BAR_BG = 0xE0120B08;
    private static final int BAR_DONE = 0xFF63D14C;

    public HotPotScreen(HotPotScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.translatable(HotPotBlockEntity.key("screen.title")));
        this.backgroundWidth = W;
        this.backgroundHeight = H;
    }

    // ------------------------------------------------------------- 汤底配色

    private record Soup(int base, int hi, int lo, int[] bits) {
    }

    private static final Soup NO_SOUP = new Soup(0xFF1B1C20, 0xFFFFFFFF, 0xFF808080, new int[]{0xFF808080});

    private static Soup soup(int baseId) {
        SoupBase soup = SoupBase.byId(baseId);
        if (soup == null) {
            return NO_SOUP;
        }
        return switch (soup) {
            case CLEAR -> new Soup(0xFFB4BCBC, 0xFFF4F4EC, 0xFFC6CCCA,   // 清汤配色同锅里的新贴图（2026-10-23，Claude 改）
                new int[]{0xFF6FAF3A, 0xFF8FCF52, 0xFFD04A2A});
            case MILKY -> new Soup(0xFFE3D7BF, 0xFFFFFFFF, 0xFFF2E7D2, new int[]{0xFFFFF8EA, 0xFF6FAF3A, 0xFFD9C9A8});
            case MUSHROOM -> new Soup(0xFF7C5836, 0xFFE8C88A, 0xFF9A7048, new int[]{0xFF4A3220, 0xFFC9A16A, 0xFF5E4026});
            case SPICY -> new Soup(0xFFB32A1E, 0xFFFFB070, 0xFFD94A2E, new int[]{0xFFE8862A, 0xFF7A1408, 0xFF3A1208});
        };
    }

    /** 锅身金属配色：取自锅贴图（铜锅 = 原版铜块色系，铁锅 = 铁灰），锅沿、隔条、铆钉都用它，不再是金色。 */
    private record Metal(int shade, int dark, int mid, int light, int rivet) {
    }

    private static final Metal COPPER_METAL = new Metal(0xFF6E3524, 0xFF8E4A33, 0xFFC26B4C, 0xFFE3826C, 0xFFF2A88C);
    private static final Metal IRON_METAL = new Metal(0xFF101113, 0xFF1C1E21, 0xFF3D3F44, 0xFF5A5D63, 0xFF7A7D84);

    private Metal metal() {
        return handler.potMaterial() == HotPotMaterial.IRON ? IRON_METAL : COPPER_METAL;
    }

    private float burnRatio() {
        int total = Math.max(1, handler.burnTotal());
        return Math.min(1F, handler.burnTicks() / (float) total);
    }

    /** 0 = 汤面平静；有汤且灶火在烧时 0.45~1，随剩余燃料变旺。 */
    private double heat() {
        return handler.hasSoup() && handler.burnTicks() > 0 ? 0.45 + 0.55 * burnRatio() : 0;
    }

    // ------------------------------------------------------------- 背景

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        double t = Util.getMeasuringTimeMs() / 1000.0;
        context.getMatrices().push();
        context.getMatrices().translate(this.x, this.y, 0);

        frame(context, W, H, 4, false);
        rivet(context, 6, H - 10);
        // 顶部铭牌
        context.fill(6, 4, W - 6, 17, 0xE01A0F07);
        context.fill(6, 17, W - 6, 18, BRASS_DARK);
        context.fill(8, 5, W - 8, 6, 0x30FFF0C0);

        int gx = HotPotScreenHandler.GRID_X;
        int gy = HotPotScreenHandler.GRID_Y;
        drawPot(context, gx, gy);
        context.fill(gx - 2, gy - 2, gx + GW + 2, gy + GW + 2, INNER_SHADOW);
        drawSoup(context, gx, gy, t);
        drawDividers(context, gx, gy);
        if (heat() > 0) {
            drawSteam(context, gx, gy, t);
        }

        // 状态卡片
        int px0 = PANEL_X;
        int px1 = W - 6;
        context.fill(px0, 20, px1, 104, RIM_LINE);
        context.fill(px0 + 1, 21, px1 - 1, 103, 0xFF22140A);
        context.fill(px0 + 1, 21, px1 - 1, 22, 0x20FFF0C0);
        inventoryFrames(context, HotPotScreenHandler.INV_X, HotPotScreenHandler.INV_Y, HotPotScreenHandler.HOTBAR_Y);

        context.getMatrices().pop();
    }

    private void drawPot(DrawContext c, int gx, int gy) {
        Metal m = metal();
        int x0 = gx - 7;
        int y0 = gy - 7;
        int x1 = gx + GW + 7;
        int y1 = gy + GW + 7;
        c.fill(x0, y0, x1, y1, RIM_LINE);
        c.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, m.dark());
        c.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, m.mid());
        c.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 5, m.light());
        c.fill(x0 + 2, y1 - 5, x1 - 2, y1 - 2, m.shade());
        c.fill(x0 + 2, y0 + 2, x0 + 5, y1 - 2, m.light());
        c.fill(x1 - 5, y0 + 2, x1 - 2, y1 - 2, m.shade());
        for (int i = 0; i <= 2; i++) {
            int cx = x0 + 4 + i * (x1 - x0 - 10) / 2;
            potRivet(c, cx, y0 + 2, m);
            potRivet(c, cx, y1 - 6, m);
            int cy = y0 + 4 + i * (y1 - y0 - 10) / 2;
            potRivet(c, x0 + 2, cy, m);
            potRivet(c, x1 - 6, cy, m);
        }
    }

    private static void potRivet(DrawContext c, int x, int y, Metal m) {
        c.fill(x, y, x + 4, y + 4, m.dark());
        c.fill(x, y, x + 4, y + 1, m.light());
        c.fill(x + 1, y + 1, x + 3, y + 3, m.rivet());
    }

    private void drawSoup(DrawContext c, int gx, int gy, double t) {
        Soup soup = soup(handler.baseId());
        c.fill(gx, gy, gx + GW, gy + GW, soup.base());
        // 越靠锅壁越深
        c.fill(gx, gy, gx + GW, gy + 4, 0x30000000);
        c.fill(gx, gy + GW - 4, gx + GW, gy + GW, 0x22000000);
        c.fill(gx, gy, gx + 4, gy + GW, 0x26000000);
        c.fill(gx + GW - 4, gy, gx + GW, gy + GW, 0x1C000000);
        if (!handler.hasSoup()) {
            return;
        }
        double heat = heat();
        double cx = gx + GW / 2.0;
        double cy = gy + GW / 2.0;

        // 高光斑：淡入淡出
        for (int i = 0; i < 5; i++) {
            double life = 1.8 + hash(i, 11) * 1.2;
            int cyc = (int) Math.floor(t / life + hash(i, 5));
            double u = frac(t / life + hash(i, 5));
            double a = easeInOutSine(u < 0.5 ? u * 2 : 2 - u * 2);
            int x = gx + 8 + (int) Math.floor(hash(i, cyc * 3) * (GW - 30));
            int y = gy + 8 + (int) Math.floor(hash(i, cyc * 3 + 1) * (GW - 16));
            c.fill(x, y, x + 14, y + 2, alpha(0xFFFFF4D8, 0.13 * a));
            c.fill(x + 3, y + 2, x + 10, y + 3, alpha(0xFFFFF4D8, 0.09 * a));
        }
        // 配料随对流打转：越靠里转得越快，火越旺转得越快，叠一点慢摆动
        double spin = 0.1 + 0.6 * heat;
        int[] bits = soup.bits();
        for (int i = 0; i < 22; i++) {
            double r = 14 + hash(i, 1) * 66;
            double ang = hash(i, 2) * Math.PI * 2
                + t * spin * (0.6 + 0.8 * hash(i, 3)) * (i % 2 == 1 ? 1 : -1) * (30 / r)
                + 0.35 * Math.sin(t * 0.7 + i);
            int x = (int) Math.round(cx + Math.cos(ang) * r);
            int y = (int) Math.round(cy + Math.sin(ang) * r * 0.96);
            c.fill(x, y, x + 2, y + (i % 3 == 0 ? 2 : 1), bits[i % bits.length]);
        }
        // 气泡：整锅随机冒出一簇簇小泡，外加几个单独的大泡；
        // 每个泡鼓起→停留并随对流漂一点→很快缩没，溅出一两滴
        if (heat > 0) {
            int clusters = (int) Math.round(4 + 8 * heat);
            for (int k = 0; k < clusters; k++) {
                double period = (0.9 + 0.5 * hash(k, 21)) / (0.7 + 0.5 * heat);
                double tt = t / period + hash(k, 22);
                int cyc = (int) Math.floor(tt);
                double cc = frac(tt);
                double kx = gx + 5 + hash(k, cyc * 2) * (GW - 10);
                double ky = gy + 5 + hash(k, cyc * 2 + 1) * (GW - 10);
                int m = 3 + (int) Math.floor(hash(k, cyc * 3) * 4.99);
                for (int j = 0; j < m; j++) {
                    double s0 = 0.35 * hash(k * 7 + j, cyc + 31);
                    double life = 0.45 + 0.2 * hash(k * 7 + j, cyc + 32);
                    double u = (cc - s0) / life;
                    if (u < 0 || u > 1) {
                        continue;
                    }
                    double ang = hash(k * 7 + j, cyc + 33) * Math.PI * 2;
                    double dist = j == 0 ? 0 : 2 + 4 * hash(k * 7 + j, cyc + 34);
                    drawBoilBubble(c, gx, gy, kx + Math.cos(ang) * dist, ky + Math.sin(ang) * dist,
                        bubbleSize(hash(k * 7 + j, cyc + 35), false), u, soup, k * 31 + j);
                }
            }
            int lone = (int) Math.round(2 + 4 * heat);
            for (int i = 0; i < lone; i++) {
                double period = (0.7 + 0.4 * hash(i, 61)) / (0.7 + 0.5 * heat);
                double tt = t / period + hash(i, 62);
                int cyc = (int) Math.floor(tt);
                double u = frac(tt);
                double x = gx + 7 + hash(i, cyc * 2 + 70) * (GW - 14);
                double y = gy + 7 + hash(i, cyc * 2 + 71) * (GW - 14);
                drawBoilBubble(c, gx, gy, x, y, bubbleSize(hash(i, cyc + 72), true), u, soup, 500 + i);
            }
        }
    }

    /** 气泡半径：簇里多是 1~2 像素的小泡，单独的大泡 3~5 像素。 */
    private static int bubbleSize(double h, boolean big) {
        return big ? 3 + (int) Math.floor(h * 2.99) : 1 + (int) Math.floor(Math.pow(h, 2.2) * 3.99);
    }

    /** 一个泡的一生 u (0..1)：缓出鼓起 → 停留微颤并随对流漂 → 迅速缩没，溅出一两滴。 */
    private static void drawBoilBubble(DrawContext c, int gx, int gy, double x, double y, int rMax,
                                       double u, Soup soup, int seed) {
        double r;
        if (u < 0.35) {
            r = rMax * easeOutCubic(u / 0.35);
        } else if (u < 0.85) {
            r = rMax * (1 + 0.08 * Math.sin((u - 0.35) * 18));
        } else {
            double k = 1 - (u - 0.85) / 0.15;
            r = rMax * k * k;
        }
        double drift = 1.5 * easeOutCubic(u);
        double bx = x + drift * 0.6;
        double by = y - drift;
        if (r >= 0.5) {
            drawBubble(c, gx, gy, bx, by, r, soup);
        }
        if (u > 0.85) {
            double p = (u - 0.85) / 0.15;
            int color = alpha(soup.hi(), 0.9 * (1 - p));
            for (int j = 0; j < 1 + (seed & 1); j++) {
                double a = hash(seed, j + 90) * Math.PI * 2;
                double dd = rMax + 1 + 2 * p;
                int px = (int) Math.round(bx + Math.cos(a) * dd);
                int py = (int) Math.round(by + Math.sin(a) * dd);
                c.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    /** 俯视的圆泡：泡膜比汤稍亮，左上边缘透亮、右下边缘压暗，左上一点高光；不画出锅面。 */
    private static void drawBubble(DrawContext c, int gx, int gy, double x, double y, double r, Soup soup) {
        int rr = (int) Math.ceil(r + 0.5);
        int body = alpha(mix(mix(soup.base(), soup.lo(), 0.5), soup.hi(), 0.18), 0.92);
        int rimLight = mix(soup.lo(), soup.hi(), 0.55);
        int dark = shade(soup.base(), 0.68);
        for (int dy = -rr; dy <= rr; dy++) {
            for (int dx = -rr; dx <= rr; dx++) {
                double dist = Math.hypot(dx, dy);
                if (dist > r + 0.35) {
                    continue;
                }
                int px = (int) Math.round(x + dx);
                int py = (int) Math.round(y + dy);
                if (px < gx || py < gy || px >= gx + GW || py >= gy + GW) {
                    continue;
                }
                int color = body;
                if (dist > r - 0.65) {
                    color = dx + dy < 0 ? rimLight : dark;
                }
                c.fill(px, py, px + 1, py + 1, color);
            }
        }
        int hl = Math.max(1, (int) Math.round(r * 0.35));
        int hx = (int) Math.round(x - r * 0.4);
        int hy = (int) Math.round(y - r * 0.4);
        c.fill(hx, hy, hx + hl, hy + hl, soup.hi());
    }

    /** 半透明蒸汽团：缓出上飘、左右摆、逐渐变大后散掉。 */
    private void drawSteam(DrawContext c, int gx, int gy, double t) {
        int m = (int) Math.round(3 + 5 * heat());
        for (int i = 0; i < m; i++) {
            double life = 1.3 + hash(i, 41) * 0.8;
            int cyc = (int) Math.floor(t / life + hash(i, 42));
            double u = frac(t / life + hash(i, 42));
            int x = (int) Math.round(gx + 12 + hash(i, cyc * 7) * (GW - 24) + 3 * Math.sin(u * Math.PI * 2 + i));
            int y = (int) Math.round(gy + 30 + hash(i, cyc * 7 + 1) * (GW - 40) - 22 * easeOutQuad(u));
            int sz = 2 + (int) Math.round(4 * easeOutCubic(u));
            int color = alpha(0xFFFFFFFF, 0.2 * Math.pow(Math.sin(Math.PI * u), 1.5));
            c.fill(x - sz, y - (sz >> 1), x + sz, y + (sz >> 1), color);
            c.fill(x - (sz >> 1), y - sz, x + (sz >> 1), y + sz, color);
        }
    }

    /** 九宫格隔条：和锅身同一种金属，两条竖、两条横，交叉处一枚铆钉。 */
    private void drawDividers(DrawContext c, int gx, int gy) {
        Metal m = metal();
        int[] cuts = {3 * 18, 6 * 18 + HotPotScreenHandler.GAP};
        for (int cut : cuts) {
            int vx = gx + cut;
            c.fill(vx, gy, vx + 1, gy + GW, m.dark());
            c.fill(vx + 1, gy, vx + 3, gy + GW, m.mid());
            c.fill(vx + 3, gy, vx + 4, gy + GW, m.light());
            int hy = gy + cut;
            c.fill(gx, hy, gx + GW, hy + 1, m.dark());
            c.fill(gx, hy + 1, gx + GW, hy + 3, m.mid());
            c.fill(gx, hy + 3, gx + GW, hy + 4, m.light());
        }
        for (int a : cuts) {
            for (int b : cuts) {
                int cx = gx + a;
                int cy = gy + b;
                c.fill(cx - 2, cy - 2, cx + 6, cy + 6, m.dark());
                c.fill(cx - 1, cy - 1, cx + 5, cy + 5, m.mid());
                c.fill(cx, cy, cx + 4, cy + 4, m.light());
                c.fill(cx + 1, cy + 1, cx + 3, cy + 3, m.rivet());
            }
        }
    }

    // ------------------------------------------------------------- 前景

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        double t = Util.getMeasuringTimeMs() / 1000.0;
        Soup soup = soup(handler.baseId());
        SoupBase soupBase = SoupBase.byId(handler.baseId());

        // 铭牌：小铜锅图标 + 标题
        int ix = 9;
        int iy = 4;
        Metal m = metal();
        context.fill(ix + 2, iy + 2, ix + 12, iy + 10, m.dark());
        context.fill(ix + 1, iy + 1, ix + 13, iy + 4, m.mid());
        context.fill(ix + 1, iy + 1, ix + 13, iy + 2, m.light());
        context.fill(ix + 3, iy + 4, ix + 11, iy + 9, handler.hasSoup() ? soup.base() : 0xFF1A1A1E);
        context.fill(ix + 6, iy + 4, ix + 7, iy + 9, m.mid());
        context.fill(ix + 3, iy + 6, ix + 11, iy + 7, m.mid());
        context.fill(ix, iy + 2, ix + 1, iy + 5, m.dark());
        context.fill(ix + 13, iy + 2, ix + 14, iy + 5, m.dark());
        context.drawText(textRenderer, this.title, 28, 6, TITLE, true);

        int x = PANEL_X + 6;
        int right = W - 12;

        // 汤底
        context.fill(x, 26, x + 9, 35, RIM_LINE);
        context.fill(x + 1, 27, x + 8, 34, handler.hasSoup() ? soup.base() : 0xFF1B1C20);
        Text name = soupBase == null
            ? Text.translatable(HotPotBlockEntity.key("soup.none")).formatted(Formatting.GRAY)
            : Text.translatable(soupBase.nameKey()).formatted(Formatting.GOLD);
        context.drawText(textRenderer, name, x + 13, 27, 0xFFFFFFFF, true);
        // 灶火：跳动的小火苗 + 剩余燃料条，不显示数字
        drawFireIcon(context, x + 1, 39, handler.burnTicks() > 0, t);
        context.drawText(textRenderer, Text.translatable(HotPotBlockEntity.key("screen.fire")), x + 13, 41, LABEL, true);
        int bx = x + 40;
        context.fill(bx, 42, right, 48, RIM_LINE);
        context.fill(bx + 1, 43, right - 1, 47, 0xFF120B08);
        int bw = handler.burnTotal() > 0 ? Math.round((right - bx - 2) * burnRatio()) : 0;
        if (bw > 0) {
            context.fill(bx + 1, 43, bx + 1 + bw, 47, 0xFFE2582B);
            context.fill(bx + 1, 43, bx + 1 + bw, 45, 0xFFF0A23C);
        }

        // 食材计数
        int filled = 0;
        int cooked = 0;
        for (int i = 0; i < HotPotBlockEntity.SLOTS; i++) {
            if (handler.getSlot(i).hasStack()) {
                filled++;
                if (handler.isCooked(i)) {
                    cooked++;
                }
            }
        }
        int end = context.drawText(textRenderer,
            Text.translatable(HotPotBlockEntity.key("screen.filled"), filled + "/81"), x, 55, 0xFFE8C88A, true);
        if (cooked > 0) {
            context.drawText(textRenderer,
                Text.translatable(HotPotBlockEntity.key("screen.cooked"), cooked).formatted(Formatting.GREEN),
                end + 8, 55, 0xFFFFFFFF, true);
        }
        context.fill(x, 67, right, 68, 0x40FFF0C0);

        // 看情况给一句提示
        String hintKey;
        int hintColor;
        if (!handler.hasSoup()) {
            hintKey = "ui.need_soup";
            hintColor = 0xFFAAAAAA;
        } else if (handler.burnTicks() <= 0) {
            hintKey = "ui.no_fire";
            hintColor = 0xFFFF7A6A;
        } else if (cooked > 0) {
            hintKey = "screen.hint.collect";
            hintColor = 0xFF9AD87E;
        } else if (filled > 0) {
            hintKey = "screen.hint.cooking";
            hintColor = 0xFFAAAAAA;
        } else {
            hintKey = "screen.hint.add";
            hintColor = 0xFFAAAAAA;
        }
        List<OrderedText> lines = textRenderer.wrapLines(Text.translatable(HotPotBlockEntity.key(hintKey)), right - x);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            context.drawText(textRenderer, lines.get(i), x, 73 + i * 10, hintColor, false);
        }

        context.drawText(textRenderer, Text.translatable(HotPotBlockEntity.key("screen.inventory")),
            HotPotScreenHandler.INV_X, HotPotScreenHandler.INV_Y - 11, LABEL, true);
    }

    private static void drawFireIcon(DrawContext c, int x, int y, boolean lit, double t) {
        double f = lit ? 0.5 + 0.5 * Math.sin(t * 7.3) * Math.sin(t * 3.1) : 0;
        int outer = lit ? 0xFFE2582B : 0xFF4A3A30;
        int inner = lit ? mix(0xFFF0A23C, 0xFFFBE08A, f) : 0xFF5E4C3E;
        c.fill(x + 3, y, x + 5, y + 2, outer);
        c.fill(x + 2, y + 2, x + 7, y + 5, outer);
        c.fill(x + 1, y + 4, x + 8, y + 9, outer);
        c.fill(x + 3, y + 5, x + 6, y + 9, inner);
        c.fill(x + 4, y + 3, x + 5, y + 5, inner);
    }

    // ------------------------------------------------------------- 槽位

    @Override
    protected void drawSlot(DrawContext context, Slot slot) {
        super.drawSlot(context, slot);
        if (!handler.isPotSlot(slot)) {
            return;
        }
        // 已在 this.x/this.y 平移后的坐标系里；格子比物品大一圈
        int sx = slot.x - 1;
        int sy = slot.y - 1;
        context.fill(sx, sy, sx + 18, sy + 1, 0x40000000);
        context.fill(sx, sy, sx + 1, sy + 18, 0x40000000);
        context.fill(sx + 17, sy, sx + 18, sy + 18, 0x24FFF0C0);
        context.fill(sx, sy + 17, sx + 18, sy + 18, 0x24FFF0C0);
        if (!slot.hasStack()) {
            return;
        }
        if (handler.isCooked(slot.getIndex())) {
            // 熟了：绿框平滑呼吸 + 右上角一个小勾
            double t = Util.getMeasuringTimeMs() / 1000.0;
            double pulse = easeInOutSine(0.5 + 0.5 * Math.sin(t * Math.PI * 1.2));
            int glow = mix(0xFF4FAE3A, 0xFF9CFF7E, pulse);
            context.fill(sx, sy, sx + 18, sy + 1, glow);
            context.fill(sx, sy + 17, sx + 18, sy + 18, glow);
            context.fill(sx, sy, sx + 1, sy + 18, glow);
            context.fill(sx + 17, sy, sx + 18, sy + 18, glow);
            context.fill(sx + 11, sy + 4, sx + 12, sy + 5, 0xFFFFFFFF);
            context.fill(sx + 12, sy + 5, sx + 13, sy + 6, 0xFFFFFFFF);
            context.fill(sx + 13, sy + 4, sx + 14, sy + 5, 0xFFFFFFFF);
            context.fill(sx + 14, sy + 3, sx + 15, sy + 4, 0xFFFFFFFF);
            context.fill(sx + 15, sy + 2, sx + 16, sy + 3, 0xFFFFFFFF);
            return;
        }
        int d = handler.doneness(slot.getIndex());
        if (d <= 0) {
            return;
        }
        // 正在煮：整格叠暖色，底部进度条由橙渐变到绿
        float ratio = HotPotCooking.ratio(d);
        context.fill(sx + 1, sy + 1, sx + 17, sy + 17, alpha(0xFFE0A23C, 0.08 + 0.16 * ratio));
        context.fill(sx + 2, sy + 15, sx + 16, sy + 17, BAR_BG);
        int w = Math.round(14F * ratio);
        context.fill(sx + 2, sy + 15, sx + 2 + w, sy + 17,
            d >= HotPotCooking.FULL_DONE ? BAR_DONE : mix(0xFFE0A23C, 0xFF9AD04C, ratio));
    }

    /** 原版 HandledScreen.render 不画悬停提示，要子类自己调（漏了这步，格子里的东西指上去看不到名字和属性；2026-10-23 用户报，Claude 补）。 */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        Slot focused = this.focusedSlot;
        if (focused == null || !handler.isPotSlot(focused) || !focused.hasStack()) {
            super.drawMouseoverTooltip(context, mouseX, mouseY);
            return;
        }
        int index = focused.getIndex();
        ItemStack stack = focused.getStack();
        int cell = handler.bigCellOf(index);
        Text location = Text.translatable(HotPotBlockEntity.key("ui.cell"), cell / 3 + 1, cell % 3 + 1);
        Text status;
        if (!handler.hasSoup()) {
            status = Text.translatable(HotPotBlockEntity.key("ui.no_soup_yet")).formatted(Formatting.GRAY);
        } else if (handler.isCooked(index)) {
            status = Text.translatable(HotPotBlockEntity.key("ui.cooked")).formatted(Formatting.GREEN);
        } else {
            float seconds = handler.secondsLeft(index);
            if (Float.isInfinite(seconds)) {
                status = Text.translatable(HotPotBlockEntity.key("ui.no_fire")).formatted(Formatting.RED);
            } else {
                status = Text.translatable(HotPotBlockEntity.key("ui.wait"),
                    String.format(Locale.ROOT, "%.1f", seconds)).formatted(Formatting.YELLOW);
            }
        }
        List<Text> lines = new java.util.ArrayList<>(List.of(
            stack.getItem().getName().copy().formatted(Formatting.WHITE),
            location.copy().formatted(Formatting.GRAY),
            status));
        if (handler.isCooked(index) && handler.getCursorStack().isOf(HotPotContent.CHOPSTICKS)) {
            lines.add(Text.translatable(HotPotBlockEntity.key("screen.hint.chopsticks_eat")).formatted(Formatting.AQUA));
        }
        context.drawTooltip(textRenderer, lines, mouseX, mouseY);
    }
}
