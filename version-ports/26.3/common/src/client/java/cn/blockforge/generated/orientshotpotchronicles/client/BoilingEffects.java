// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiPredicate;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.blockforge.orient.framework.client.RenderFrame;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;

import static cn.blockforge.generated.orientshotpotchronicles.client.GuiKit.*;

/**
 * 汤面上的方块气泡：正方体从汤里慢慢长大、离开汤面，从中间破成四个小正方体弹开，
 * 再落回锅里沉下去，落点荡开一圈淡淡的涟漪。
 *
 * <p>节奏参照真实沸腾放慢：一个冒泡点零点几秒冒一个泡，泡膜破裂几乎是瞬间，碎块很快落回。
 * 全部是时间的纯函数（同 GUI 的 hash/缓动），不保存逐帧状态；火越旺气泡越多、越快。
 * 每口锅用方块坐标打散相位，相邻的锅不会同步。用白色贴图 + 顶点色上色，方块面自带明暗。
 * 网页预览 viewer/animations.js 与本类一一对应，改这里要同步改那边。</p>
 */
public final class BoilingEffects {
    private static final Identifier WHITE = HotPot.id("textures/misc/white.png");
    private static final float PX = 1F / 16F;
    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
    /** 满火时的气泡数（热度 0→1 时 3→8 个逐个长出来）。 */
    private static final int MAX_BUBBLES = 8;

    private record Palette(int body, int hi) {
    }

    /** 清水锅的气泡配色（子模组画水锅时用）。 */
    private static final Palette WATER = new Palette(0x5A8FD8, 0xC8E2FF);
    /** 未知汤底（如卸载数据包/子模组后的残余汤面）使用的中性气泡配色。 */
    private static final Palette UNKNOWN = new Palette(0xFFB8AEC8, 0xFFEAE4F2);

    private static final Map<SoupVisual, Palette> PALETTES = buildPalettes();

    private static Map<SoupVisual, Palette> buildPalettes() {
        EnumMap<SoupVisual, Palette> map = new EnumMap<>(SoupVisual.class);
        for (SoupDef def : Soups.builtins()) {
            map.put(def.visual(), new Palette(def.bubbleBody(), def.bubbleHi()));
        }
        return Collections.unmodifiableMap(map);
    }

    private static Palette palette(SoupVisual soup) {
        if (soup == SoupVisual.OTHER) {
            return UNKNOWN;
        }
        Palette palette = PALETTES.get(soup);
        if (palette == null) {
            throw new IllegalStateException("汤面 " + soup + " 没有气泡配色；加汤底时要在汤底定义（SoupBase 构造参数 / SoupDef）上补气泡配色。");
        }
        return palette;
    }

    private BoilingEffects() {
    }

    /** 按汤底定义直接渲染气泡（4F-3：支持非内置 SoupVisual.OTHER 汤底及未知汤底兜底）。 */
    static void render(SoupDef soup, float surfaceY, float innerMin, float innerSize, float heat,
                       double seconds, int salt, PoseStack matrices, RenderFrame consumers, int light) {
        render(soup, surfaceY, innerMin, innerSize, heat, seconds, salt, matrices, consumers, light, null);
    }

    /** 按汤底定义与区域过滤渲染气泡。 */
    static void render(SoupDef soup, float surfaceY, float innerMin, float innerSize, float heat,
                       double seconds, int salt, PoseStack matrices, RenderFrame consumers, int light,
                       BiPredicate<Double, Double> keepAt) {
        Palette pal = soup == null ? UNKNOWN : new Palette(soup.bubbleBody(), soup.bubbleHi());
        renderWithPalette(pal, surfaceY, innerMin, innerSize, heat, seconds, salt,
            matrices, consumers, light, keepAt);
    }

    /**
     * @param surfaceY 汤面高度（方块坐标）
     * @param innerMin 锅腔起点，innerSize 锅腔边长（方块坐标）
     * @param heat     0..1，随灶火平滑变化
     * @param seconds  连续时间（秒）
     * @param salt     每口锅不同的相位种子
     */
    static void render(SoupVisual soup, float surfaceY, float innerMin, float innerSize, float heat,
                       double seconds, int salt, PoseStack matrices, RenderFrame consumers, int light) {
        render(soup, surfaceY, innerMin, innerSize, heat, seconds, salt, matrices, consumers, light, null);
    }

    /**
     * @param keepAt 鸳鸯锅：只在这一边冒泡（参数是方块坐标 x、z），落在别处的气泡这一轮不画；null = 整锅。新增。
     */
    static void render(SoupVisual soup, float surfaceY, float innerMin, float innerSize, float heat,
                       double seconds, int salt, PoseStack matrices, RenderFrame consumers, int light,
                       BiPredicate<Double, Double> keepAt) {
        if (!soup.hasSoup()) {
            return;
        }
        renderWithPalette(palette(soup), surfaceY, innerMin, innerSize, heat, seconds, salt,
            matrices, consumers, light, keepAt);
    }

    /**
     * 清水锅的沸腾气泡（子模组画水锅时调用）：
     * 配色固定成淡蓝，其余和汤面气泡完全一样——这样这块能力留在火锅这边，子模组不用自己改一份。
     */
    public static void renderWater(float surfaceY, float innerMin, float innerSize, float heat,
                                   double seconds, int salt, PoseStack matrices,
                                   RenderFrame consumers, int light) {
        // renderWater 的调用方多传「真实秒」：乘 1.2 保持 heat=1 时与旧速度一致
        renderWithPalette(WATER, surfaceY, innerMin, innerSize, heat, seconds * 1.2, salt,
            matrices, consumers, light, null);
    }

    private static void renderWithPalette(Palette pal, float surfaceY, float innerMin, float innerSize, float heat,
                                          double seconds, int salt, PoseStack matrices,
                                          RenderFrame consumers, int light,
                                          BiPredicate<Double, Double> keepAt) {
        if (heat < 0.05F) {
            return;
        }
        VertexConsumer vc = consumers.getBuffer(RenderTypes.entityTranslucent(WHITE));
        PoseStack.Pose e = matrices.last();
        double t = seconds;
        double y = surfaceY;

        // 火越旺气泡越多：第 i 个气泡随火候从小慢慢长出来，不再是数量取整后突然冒出一个长到一半的（代码检查 W10）
        for (int i = 0; i < MAX_BUBBLES; i++) {
            double grow = Math.max(0, Math.min(1, 3 + 5 * heat - i));
            if (grow <= 0) {
                continue;
            }
            double period = 0.7 + hash(i, 21) * 0.45; // 热度已由调用方累计进 t，这里不能再除热度
            double off = hash(i, 22 + salt);
            int cyc = (int) Math.floor(t / period + off);
            double u = frac(t / period + off);
            // 每轮换个位置，整锅随机
            double x0 = innerMin + innerSize * hash(i, cyc * 2 + salt);
            double z0 = innerMin + innerSize * hash(i, cyc * 2 + 1 + salt);
            // 越靠锅边气泡越小：离边 3 像素以上是正常大小，贴边约三分之一
            double edge = Math.min(Math.min(x0 - innerMin, innerMin + innerSize - x0),
                Math.min(z0 - innerMin, innerMin + innerSize - z0)) / PX;
            double near = Math.max(0.35, Math.min(1, 0.35 + 0.65 * edge / 3));
            double sMax = PX * (0.7 + 0.6 * hash(i, cyc * 5 + salt)) * (0.85 + 0.15 * heat) * near * grow; // 约 0.25~1.3 像素
            // 气泡和破开的碎块都不碰锅壁
            double keep = sMax * 1.3 + 0.3 * PX;
            double x = Math.min(innerMin + innerSize - keep, Math.max(innerMin + keep, x0));
            double z = Math.min(innerMin + innerSize - keep, Math.max(innerMin + keep, z0));
            if (keepAt != null && !keepAt.test(x, z)) {
                continue;
            }
            double rise = PX * (0.3 + 0.4 * hash(i, cyc * 5 + 1)); // 刚好离开汤面
            double pop = PX * 0.25;

            if (u < 0.45) {
                // 1. 从汤里慢慢长大，下半截还泡在汤里
                double s = sMax * easeOutCubic(u / 0.45);
                if (s > 0.15 * PX) {
                    cube(vc, e, light, x, y - s * 0.35, z, s, pal, 240);
                }
            } else if (u < 0.6) {
                // 2. 离开汤面
                double f = easeInOutSine((u - 0.45) / 0.15);
                cube(vc, e, light, x, y - sMax * 0.35 * (1 - f) + rise * f, z, sMax, pal, 240);
            } else if (u < 0.68) {
                // 3. 从中间破开：几乎瞬间，像真实的泡膜破裂
                double p = easeOutCubic((u - 0.6) / 0.08);
                double q = sMax / 2;
                double spread = q / 2 + sMax * 0.55 * p;
                double yb = y + rise + pop * p;
                for (int[] c : CORNERS) {
                    cube(vc, e, light, x + c[0] * spread, yb, z + c[1] * spread, q, pal, 240);
                }
            } else {
                // 4. 碎块受重力落回、变小、沉下去
                double p = (u - 0.68) / 0.32;
                double q = sMax / 2 * (1 - 0.7 * p);
                double spread = sMax / 4 + sMax * 0.55 + sMax * 0.25 * p;
                double yb = y + (rise + pop) * (1 - p * p) - q * 0.5 * p;
                for (int[] c : CORNERS) {
                    cube(vc, e, light, x + c[0] * spread, yb, z + c[1] * spread, q, pal, 240);
                }
                if (p > 0.6) {
                    // 落点一圈淡淡的涟漪
                    double r = sMax * (0.8 + 1.2 * (p - 0.6) / 0.4);
                    int a = (int) (200 * (1 - (p - 0.6) / 0.4));
                    double d = 0.18 * PX;
                    for (int j = 0; j < 8; j++) {
                        double ang = j / 8.0 * Math.PI * 2;
                        double rx = x + Math.cos(ang) * r;
                        double rz = z + Math.sin(ang) * r;
                        box(vc, e, light, rx - d, y, rz - d, rx + d, y + 0.1 * PX, rz + d, pal.hi(), pal.hi(), a);
                    }
                }
            }
        }
    }

    /** 底面中心在 (x, yb, z) 的正方体，顶面亮色、侧面汤色。 */
    private static void cube(VertexConsumer vc, PoseStack.Pose e, int light,
                             double x, double yb, double z, double s, Palette pal, int alpha) {
        box(vc, e, light, x - s / 2, yb, z - s / 2, x + s / 2, yb + s, z + s / 2, pal.hi(), pal.body(), alpha);
    }

    /** 顶面 + 四个侧面（底面贴着汤面，不画）。 */
    private static void box(VertexConsumer vc, PoseStack.Pose e, int light,
                            double x0, double y0, double z0, double x1, double y1, double z1,
                            int top, int side, int alpha) {
        float a0 = (float) x0, b0 = (float) y0, c0 = (float) z0, a1 = (float) x1, b1 = (float) y1, c1 = (float) z1;
        quad(vc, e, light, top, alpha, 0, 1, 0, a0, b1, c1, a1, b1, c1, a1, b1, c0, a0, b1, c0);
        quad(vc, e, light, side, alpha, 0, 0, -1, a0, b0, c0, a0, b1, c0, a1, b1, c0, a1, b0, c0);
        quad(vc, e, light, side, alpha, 0, 0, 1, a1, b0, c1, a1, b1, c1, a0, b1, c1, a0, b0, c1);
        quad(vc, e, light, side, alpha, -1, 0, 0, a0, b0, c1, a0, b1, c1, a0, b1, c0, a0, b0, c0);
        quad(vc, e, light, side, alpha, 1, 0, 0, a1, b0, c0, a1, b1, c0, a1, b1, c1, a1, b0, c1);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose e, int light, int rgb, int alpha,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        vertex(vc, e, light, x0, y0, z0, 0, 1, r, g, b, alpha, nx, ny, nz);
        vertex(vc, e, light, x1, y1, z1, 1, 1, r, g, b, alpha, nx, ny, nz);
        vertex(vc, e, light, x2, y2, z2, 1, 0, r, g, b, alpha, nx, ny, nz);
        vertex(vc, e, light, x3, y3, z3, 0, 0, r, g, b, alpha, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose e, int light, float x, float y, float z,
                               float u, float v, int r, int g, int b, int a, float nx, float ny, float nz) {
        vc.addVertex(e, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(e, nx, ny, nz);
    }
}
