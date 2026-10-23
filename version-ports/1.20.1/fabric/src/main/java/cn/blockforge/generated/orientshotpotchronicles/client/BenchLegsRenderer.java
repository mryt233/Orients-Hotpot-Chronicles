// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.BenchBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.BenchBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * 条凳的四条斜腿：每头两条，上端在凳面下（y=14、z 6.5 / 9.5），到地面各往前后岔开 2 像素，像「A」字。
 * 单独一张腿在两头；连成长凳时两头的腿往里收 3 像素，中间没有腿（同凳面模型的规则）。
 * 坐标按朝北的模型算，再按 FACING 转（和 blockstate 的 y 旋转一致）。查看器 animations.js 的 benchLegs 是同一份算法。
 */
public final class BenchLegsRenderer implements BlockEntityRenderer<BenchBlockEntity> {
    private static final float LEG_W = 2F;
    private static final float LEG_T = 1.5F;

    public BenchLegsRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(BenchBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        renderLegs(be.getCachedState(), matrices, vertexConsumers, light);
    }

    public static void renderLegs(BlockState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!(state.getBlock() instanceof BenchBlock bench)) {
            return;
        }
        Sprite sprite = MinecraftClient.getInstance().getBlockRenderManager().getModels()
            .getModelParticleSprite(bench.planks().getDefaultState());
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        boolean left = state.get(BenchBlock.LEFT);
        boolean right = state.get(BenchBlock.RIGHT);
        boolean single = !left && !right;
        float angle = (float) Math.atan2(BenchBlock.LEG_SPREAD, BenchBlock.LEG_TOP);
        float len = (float) Math.hypot(BenchBlock.LEG_SPREAD, BenchBlock.LEG_TOP);
        float yRot = switch (state.get(BenchBlock.FACING)) {
            case EAST -> 90F;
            case SOUTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(yRot));
        matrices.translate(-0.5, 0, -0.5);
        float[] ends = new float[2];
        int n = 0;
        if (!left) {
            ends[n++] = single ? 1.5F : 4.5F;
        }
        if (!right) {
            ends[n++] = single ? 12.5F : 9.5F;
        }
        for (int i = 0; i < n; i++) {
            for (int side = 0; side < 2; side++) {
                float zc = side == 0 ? 6.5F : 9.5F;
                float sign = side == 0 ? 1F : -1F;
                matrices.push();
                matrices.translate(ends[i] / 16F, BenchBlock.LEG_TOP / 16F, zc / 16F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotation(sign * angle));
                box(matrices, vc, sprite, light, 0, -len, -LEG_T / 2, LEG_W, 0, LEG_T / 2);
                matrices.pop();
            }
        }
        matrices.pop();
    }

    /** 一个贴木板贴图的长方体（像素坐标），每个面按自己的尺寸取贴图。 */
    private static void box(MatrixStack matrices, VertexConsumer vc, Sprite s, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        MatrixStack.Entry e = matrices.peek();
        float a = x0 / 16F, b = y0 / 16F, c = z0 / 16F, d = x1 / 16F, f = y1 / 16F, g = z1 / 16F;
        float w = x1 - x0, h = y1 - y0, t = z1 - z0;
        quad(vc, e, s, light, 0, 0, -1, w, h, d, f, c, a, f, c, a, b, c, d, b, c);   // north
        quad(vc, e, s, light, 0, 0, 1, w, h, a, f, g, d, f, g, d, b, g, a, b, g);    // south
        quad(vc, e, s, light, -1, 0, 0, t, h, a, f, c, a, f, g, a, b, g, a, b, c);   // west
        quad(vc, e, s, light, 1, 0, 0, t, h, d, f, g, d, f, c, d, b, c, d, b, g);    // east
        quad(vc, e, s, light, 0, 1, 0, w, t, a, f, c, d, f, c, d, f, g, a, f, g);    // up
        quad(vc, e, s, light, 0, -1, 0, w, t, a, b, g, d, b, g, d, b, c, a, b, c);   // down
    }

    /** 四个角按 左上、右上、右下、左下（从外面看）给出；贴图取 sprite 左上角 uw × vh 像素。 */
    private static void quad(VertexConsumer vc, MatrixStack.Entry e, Sprite s, int light,
                             float nx, float ny, float nz, float uw, float vh,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        float u0 = s.getFrameU(0F), u1 = s.getFrameU(Math.min(1F, uw / 16F));
        float v0 = s.getFrameV(0F), v1 = s.getFrameV(Math.min(1F, vh / 16F));
        vertex(vc, e, x0, y0, z0, u0, v0, light, nx, ny, nz);
        vertex(vc, e, x1, y1, z1, u1, v0, light, nx, ny, nz);
        vertex(vc, e, x2, y2, z2, u1, v1, light, nx, ny, nz);
        vertex(vc, e, x3, y3, z3, u0, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        vc.vertex(e.getPositionMatrix(), x, y, z).color(255, 255, 255, 255).texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e.getNormalMatrix(), nx, ny, nz).next();   // 1.20.1 移植（Claude 改）
    }
}
