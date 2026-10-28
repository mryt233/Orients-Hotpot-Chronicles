package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * 蘸料台小碗里的调料面（2026-10-23 用户改成碗不固定后，）：每个碗装什么由方块实体决定，方块状态表示不了，
 * 所以台身和空碗是方块模型，碗里的调料面在这里画。几何和 tools/gen_sauce_station.py 的物品模型（九种都装满）一致：
 * 液体调料一张面（碗口下 0.5 像素），葱花 / 香菜碎末四层错开的碎末（碗口下 1.3 / 0.95 / 0.6 / 0.25）。
 * 网页预览 viewer/animations.js 的 sauce_station 场景与本类对应。
 *
 */
public class SauceStationRenderer implements BlockEntityRenderer<SauceStationBlockEntity> {
    /** 葱花 / 香菜碎末每一层：高度（碗口往下）、贴图取的范围、贴图转几个 90°。 */
    private static final float[][] GARNISH_LAYERS = {
        {1.3F, 2, 2, 14, 14, 0}, {0.95F, 0, 4, 12, 16, 1}, {0.6F, 4, 0, 16, 12, 2}, {0.25F, 3, 3, 13, 13, 3}};

    public SauceStationRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(SauceStationBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers,
                       int light, int overlay) {
        Direction facing = be.getCachedState().get(SauceStationBlock.FACING);
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutout(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        // 和方块状态一样转：模型正面朝北，facing 东 / 南 / 西 = 绕 y 顺时针 90 / 180 / 270°
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-modelRotation(facing)));
        matrices.translate(-0.5, 0, -0.5);
        MatrixStack.Entry e = matrices.peek();
        for (int i = 0; i < SauceStationBlockEntity.BOWLS; i++) {
            Seasoning s = be.kind(i);
            if (s == null || be.available(i) == 0) {
                continue;
            }
            int tier = 2 - i / 3, col = i % 3;
            float cx = 16 - 16F / 3 * (col + 0.5F), cz = 16F / 3 * (tier + 0.5F);
            float rim = (float) (SauceStationBlock.TIER_TOPS[tier] + SauceStationBlock.BOWL_H);
            float half = (float) (SauceStationBlock.BOWL / 2 - SauceStationBlock.WALL);
            boolean garnish = s == Seasoning.CHOPPED_SCALLION || s == Seasoning.MINCED_CILANTRO;
            Sprite sprite = sprite(garnish ? "block/" + s.id() : "block/sauce_" + s.id());
            if (garnish) {
                for (float[] l : GARNISH_LAYERS) {
                    quad(vc, e, light, sprite, cx - half, rim - l[0], cz - half, cx + half, cz + half, l[1], l[2], l[3], l[4], (int) l[5]);
                }
            } else {
                quad(vc, e, light, sprite, cx - half, rim - 0.5F, cz - half, cx + half, cz + half, 2, 2, 14, 14, 0);
            }
        }
        matrices.pop();
    }

    /** 方块状态里 facing 对应的模型 y 旋转（north 0 / east 90 / south 180 / west 270，同 gen_sauce_station.py 的 ROT）。 */
    private static float modelRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90F;
            case SOUTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
    }

    private static Sprite sprite(String path) {
        return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(GeneratedMod.id(path));
    }

    /** 一张朝上的面（模型像素坐标），贴图取 sprite 的 [u0,v0]..[u1,v1]（0..16），转 rot 个 90°。 */
    private static void quad(VertexConsumer vc, MatrixStack.Entry e, int light, Sprite sp, float x0, float y, float z0,
                             float x1, float z1, float u0, float v0, float u1, float v1, int rot) {
        float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};   // 西北、东北、东南、西南四个角
        float[][] pos = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        int[] order = {3, 2, 1, 0};   // 从上往下看逆时针：西南、东南、东北、西北
        for (int k : order) {
            float[] t = uv[(k + rot) % 4];
            vc.vertex(e, pos[k][0] / 16F, y / 16F, pos[k][1] / 16F).color(255, 255, 255, 255)
                .texture(sp.getFrameU(t[0] / 16F), sp.getFrameV(t[1] / 16F)).overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(e, 0, 1, 0);
        }
    }
}
