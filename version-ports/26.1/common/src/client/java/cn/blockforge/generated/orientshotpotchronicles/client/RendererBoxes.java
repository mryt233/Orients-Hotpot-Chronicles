// mryt233 改：26.1 同代移植同步。
// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * 方块实体自定义长方体 / 液面绘制公共原语（0..16 像素坐标，统一 UV 与亮度法线）。
 * 作者：mryt233
 */
final class RendererBoxes {
    private RendererBoxes() {
    }

    static void box(PoseStack matrices, VertexConsumer vc, TextureAtlasSprite sprite, int light,
                    float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        drawBox(matrices, vc, sprite, light, x0, y0, z0, x1, y1, z1, argb, false);
    }

    // mryt233 改：复用原版 BlockElement 的默认裁图坐标，让运行时上盘和 JSON 同一格对齐。
    static void modelBox(PoseStack matrices, VertexConsumer vc, TextureAtlasSprite sprite, int light,
                         float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        drawBox(matrices, vc, sprite, light, x0, y0, z0, x1, y1, z1, argb, true);
    }

    private static void drawBox(PoseStack matrices, VertexConsumer vc, TextureAtlasSprite sprite, int light,
                                float x0, float y0, float z0, float x1, float y1, float z1, int argb, boolean modelUv) {
        PoseStack.Pose e = matrices.last();
        float a = x0 / 16F, b = y0 / 16F, c = z0 / 16F;
        float d = x1 / 16F, f = y1 / 16F, g = z1 / 16F;
        int r = (argb >> 16) & 0xFF;
        int gr = (argb >> 8) & 0xFF;
        int bl = argb & 0xFF;
        int al = (argb >> 24) & 0xFF;
        float u0x = Math.max(0F, Math.min(16F, x0)) / 16F;
        float u1x = Math.max(0F, Math.min(16F, x1)) / 16F;
        float u0z = Math.max(0F, Math.min(16F, z0)) / 16F;
        float u1z = Math.max(0F, Math.min(16F, z1)) / 16F;
        float vy0;
        float vy1;
        if (y1 > 16F && (y1 - y0) <= 16F) {
            float h = y1 - y0;
            vy0 = ((16F - y1) % 16F + 16F) % 16F;
            if (vy0 + h > 16F) {
                vy0 = Math.max(0F, 16F - h);
            }
            vy1 = Math.min(16F, vy0 + h);
        } else {
            vy0 = Math.max(0F, Math.min(16F, 16F - y1));
            vy1 = Math.max(0F, Math.min(16F, 16F - y0));
        }
        float v0y = vy0 / 16F;
        float v1y = vy1 / 16F;

        quad(vc, e, sprite, light, r, gr, bl, al, 0, 0, -1,
            modelUv ? 1F - u1x : u0x, v0y, modelUv ? 1F - u0x : u1x, v1y,
            d, f, c, a, f, c, a, b, c, d, b, c);
        quad(vc, e, sprite, light, r, gr, bl, al, 0, 0, 1, u0x, v0y, u1x, v1y,
            a, f, g, d, f, g, d, b, g, a, b, g);
        quad(vc, e, sprite, light, r, gr, bl, al, -1, 0, 0, u0z, v0y, u1z, v1y,
            a, f, c, a, f, g, a, b, g, a, b, c);
        quad(vc, e, sprite, light, r, gr, bl, al, 1, 0, 0,
            modelUv ? 1F - u1z : u0z, v0y, modelUv ? 1F - u0z : u1z, v1y,
            d, f, g, d, f, c, d, b, c, d, b, g);
        quad(vc, e, sprite, light, r, gr, bl, al, 0, 1, 0, u0x, u0z, u1x, u1z,
            a, f, c, d, f, c, d, f, g, a, f, g);
        quad(vc, e, sprite, light, r, gr, bl, al, 0, -1, 0,
            u0x, modelUv ? 1F - u1z : u0z, u1x, modelUv ? 1F - u0z : u1z,
            a, b, g, d, b, g, d, b, c, a, b, c);
    }

    static void topQuad(PoseStack matrices, VertexConsumer vc, TextureAtlasSprite sprite, int light,
                        float x0, float y, float z0, float x1, float z1, int argb) {
        PoseStack.Pose e = matrices.last();
        int r = (argb >> 16) & 0xFF;
        int gr = (argb >> 8) & 0xFF;
        int bl = argb & 0xFF;
        int al = (argb >> 24) & 0xFF;
        float u0 = Math.max(0F, Math.min(16F, x0)) / 16F;
        float u1 = Math.max(0F, Math.min(16F, x1)) / 16F;
        float v0 = Math.max(0F, Math.min(16F, z0)) / 16F;
        float v1 = Math.max(0F, Math.min(16F, z1)) / 16F;
        float ax = x0 / 16F, bx = x1 / 16F, fy = y / 16F, cz = z0 / 16F, dz = z1 / 16F;
        quad(vc, e, sprite, light, r, gr, bl, al, 0, 1, 0, u0, v0, u1, v1,
            ax, fy, cz, bx, fy, cz, bx, fy, dz, ax, fy, dz);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose e, TextureAtlasSprite s, int light,
                             int r, int g, int b, int a, float nx, float ny, float nz,
                             float uMin, float vMin, float uMax, float vMax,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        float u0 = s.getU(uMin);
        float u1 = s.getU(uMax);
        float v0 = s.getV(vMin);
        float v1 = s.getV(vMax);
        // mryt233 改：统一按 OpenGL / 原版 FaceBakery 逆时针正面（CCW）绕序发射顶点，避免光影下法线反相或背面剔除。
        vertex(vc, e, x0, y0, z0, u0, v0, r, g, b, a, light, nx, ny, nz);
        vertex(vc, e, x3, y3, z3, u0, v1, r, g, b, a, light, nx, ny, nz);
        vertex(vc, e, x2, y2, z2, u1, v1, r, g, b, a, light, nx, ny, nz);
        vertex(vc, e, x1, y1, z1, u1, v0, r, g, b, a, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose e, float x, float y, float z, float u, float v,
                               int r, int g, int b, int a, int light, float nx, float ny, float nz) {
        vc.addVertex(e, x, y, z).setColor(r, g, b, a).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(e, nx, ny, nz);
    }
}
