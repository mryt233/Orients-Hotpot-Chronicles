package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.orient.framework.client.ClientItemRenderers;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/**
 * 一碗蘸料的样子（r103）：原版碗的 b 形模型（摆在桌上、拿在手里一样），碗里一层有光泽的酱面，颜色是放进去的几种调料的平均色，
 * 面上按放了哪几种撒对应颜色的小点（2026-10-23 改，原来是一层平涂 + 葱香菜的绿点）。摆在桌上由 PlateHolderRenderer 调，物品由 builtin/entity 模型交给这里画。
 * 坐标：盘子格里一个摆放位置的局部坐标（0..1，碗中心在 0.5、碗底在 y=0），和 tools/gen_bowls.py 的 b 形一致：
 * 汤面在 2.5 像素高，半宽 2.5 像素。
 * 作者：mryt233 */
public final class SauceRender {
    private static final ResourceLocation WHITE = HotPot.id("textures/misc/white.png");
    private static final float SURFACE_Y = 2.5F / 16F;
    private static final float HALF = 2.5F / 16F;

    private SauceRender() {
    }

    public static void register() {
        ClientItemRenderers.register(HotPotContent.DIPPING_SAUCE,
            (stack, mode, matrices, consumers, light, overlay) -> {
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    HotPotContent.BOWL_BLOCK.defaultBlockState(), matrices, consumers, light, overlay);
                surface(DippingSauceItem.contents(stack).ids(), matrices, consumers, light);
            });
    }

    /** 酱面贴图（灰度，按调料平均色染色；tools/gen_sauce_station.py 的 bowl_surface_texture）。 */
    private static final ResourceLocation SURFACE = HotPot.id("textures/misc/sauce_surface.png");
    /**
     * 碗里的调料面（mask 为空就不画）。2026-10-23 用户说不像蘸料（原来是一层平涂的色），改成：
     * 有光泽的酱面（贴图按几种调料的平均色染色）+ 每种放进去的调料撒两三个它自己颜色的小点。
     * mryt233 改（4C）：内置点位由定义持有，新增调料按 ID 生成点位。
     */
    @Deprecated public static void surface(int mask, PoseStack matrices, MultiBufferSource consumers, int light) {
        surface(SauceIds.fromMask(mask), matrices, consumers, light);
    }

    public static void surface(java.util.List<String> ids, PoseStack matrices, MultiBufferSource consumers, int light) {
        int color = Seasoning.mixColor(ids);
        if (color == 0) {
            return;
        }
        PoseStack.Pose e = matrices.last();
        float a = 0.5F - HALF, b = 0.5F + HALF, y = SURFACE_Y;
        quad(consumers.getBuffer(RenderType.entityCutoutNoCull(SURFACE)), e, light, color, a, y, a, b, y, b);
        VertexConsumer vc = consumers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for (SeasoningDef s : SauceIds.defs(ids)) {
            int dot = darkenIfPale(s.color());
            float size = s.dotSize() / 16F;
            for (int i = 0; i < s.dotCount(); i++) {
                float x = s.dotX(i) / 16F, z = s.dotZ(i) / 16F;
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

    private static void quad(VertexConsumer vc, PoseStack.Pose e, int light, int argb,
                             float x0, float y, float z0, float x1, float y1, float z1) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        vertex(vc, e, light, x0, y, z1, 0, 1, r, g, b);
        vertex(vc, e, light, x1, y, z1, 1, 1, r, g, b);
        vertex(vc, e, light, x1, y, z0, 1, 0, r, g, b);
        vertex(vc, e, light, x0, y, z0, 0, 0, r, g, b);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose e, int light, float x, float y, float z,
                               float u, float v, int r, int g, int b) {
        vc.addVertex(e, x, y, z).setColor(r, g, b, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(e, 0, 1, 0);
    }
}
