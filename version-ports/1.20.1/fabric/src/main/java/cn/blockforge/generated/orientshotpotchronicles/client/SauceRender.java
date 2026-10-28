package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * 一碗蘸料的样子（r103）：原版碗的 b 形模型（摆在桌上、拿在手里一样），碗里一层有光泽的酱面，颜色是放进去的几种调料的平均色，
 * 面上按放了哪几种撒对应颜色的小点（2026-10-23 改，原来是一层平涂 + 葱香菜的绿点）。摆在桌上由 PlateHolderRenderer 调，物品由 builtin/entity 模型交给这里画。
 * 坐标：盘子格里一个摆放位置的局部坐标（0..1，碗中心在 0.5、碗底在 y=0），和 tools/gen_bowls.py 的 b 形一致：
 * 汤面在 2.5 像素高，半宽 2.5 像素。
 *
 */
public final class SauceRender {
    private static final Identifier WHITE = GeneratedMod.id("textures/misc/white.png");
    private static final float SURFACE_Y = 2.5F / 16F;
    private static final float HALF = 2.5F / 16F;

    private SauceRender() {
    }

    public static void register() {
        BuiltinItemRendererRegistry.INSTANCE.register(HotPotContent.DIPPING_SAUCE,
            (stack, mode, matrices, consumers, light, overlay) -> {
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                    HotPotContent.BOWL_BLOCK.getDefaultState(), matrices, consumers, light, overlay);
                surface(DippingSauceItem.contents(stack).mask(), matrices, consumers, light);
            });
    }

    /** 酱面贴图（灰度，按调料平均色染色；tools/gen_sauce_station.py 的 bowl_surface_texture）。 */
    private static final Identifier SURFACE = GeneratedMod.id("textures/misc/sauce_surface.png");
    /** 每种调料撒在面上的两个小点（像素坐标，碗面 5.5..10.5），顺序同 Seasoning。 */
    private static final float[][][] DOTS = {
        {{6.2F, 6.6F}, {9.4F, 9.0F}}, {{7.4F, 6.0F}, {8.8F, 7.6F}}, {{6.0F, 8.6F}, {9.6F, 6.4F}},
        {{7.8F, 9.4F}, {6.6F, 7.6F}}, {{8.4F, 6.8F}, {7.0F, 9.6F}}, {{9.2F, 8.2F}, {6.4F, 9.0F}},
        {{7.6F, 7.0F}, {8.6F, 9.6F}}, {{6.8F, 6.2F}, {9.0F, 7.0F}, {7.4F, 8.8F}}, {{8.2F, 8.4F}, {6.0F, 7.2F}, {9.6F, 9.4F}},
        {{7.0F, 8.0F}, {9.0F, 6.0F}}};   // 蒜泥（2026-10-25）

    /**
     * 碗里的调料面（mask 为空就不画）。2026-10-23 用户说不像蘸料（原来是一层平涂的色），改成：
     * 有光泽的酱面（贴图按几种调料的平均色染色）+ 每种放进去的调料撒两三个它自己颜色的小点。
     */
    public static void surface(int mask, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        int color = Seasoning.mixColor(mask);
        if (color == 0) {
            return;
        }
        MatrixStack.Entry e = matrices.peek();
        float a = 0.5F - HALF, b = 0.5F + HALF, y = SURFACE_Y;
        quad(consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SURFACE)), e, light, color, a, y, a, b, y, b);
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
        for (Seasoning s : Seasoning.ALL) {
            if ((mask & s.bit()) == 0) {
                continue;
            }
            int dot = darkenIfPale(s.color());
            float size = (s == Seasoning.CHOPPED_SCALLION || s == Seasoning.MINCED_CILANTRO ? 0.7F : 0.5F) / 16F;
            for (float[] d : DOTS[s.ordinal()]) {
                float x = d[0] / 16F, z = d[1] / 16F;
                quad(vc, e, light, dot, x, y + 0.001F, z, x + size, y + 0.001F, z + size);
            }
        }
    }

    /** 白糖、味精这种白色的点在浅色酱面上看不见，压暗一点当成晶粒的阴影。 */
    private static int darkenIfPale(int argb) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        if (r + g + b > 600) {
            return 0xFF000000 | (r * 3 / 4) << 16 | (g * 3 / 4) << 8 | (b * 3 / 4);
        }
        return argb;
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, int light, int argb,
                             float x0, float y, float z0, float x1, float y1, float z1) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        vertex(vc, e, light, x0, y, z1, 0, 1, r, g, b);
        vertex(vc, e, light, x1, y, z1, 1, 1, r, g, b);
        vertex(vc, e, light, x1, y, z0, 1, 0, r, g, b);
        vertex(vc, e, light, x0, y, z0, 0, 0, r, g, b);
    }

    private static void vertex(VertexConsumer vc, MatrixStack.Entry e, int light, float x, float y, float z,
                               float u, float v, int r, int g, int b) {
        vc.vertex(e.getPositionMatrix(), x, y, z).color(r, g, b, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(e.getNormalMatrix(), 0, 1, 0).next();
    }
}
