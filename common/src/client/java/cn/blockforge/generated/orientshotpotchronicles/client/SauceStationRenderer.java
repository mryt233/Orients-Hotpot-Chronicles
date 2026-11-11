package cn.blockforge.generated.orientshotpotchronicles.client;

import static cn.blockforge.generated.orientshotpotchronicles.client.RenderSprites.sprite;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;

import java.util.HashSet;
import java.util.Set;

/**
 * 蘸料台小碗里的调料面（2026-10-23 用户改成碗不固定后，新增）：每个碗装什么由方块实体决定，方块状态表示不了，
 * 所以台身和空碗是方块模型，碗里的调料面在这里画。几何和 tools/gen_sauce_station.py 的物品模型（九种都装满）一致：
 * 液体调料一张面（碗口下 0.5 像素），葱花 / 香菜碎末四层错开的碎末（碗口下 1.3 / 0.95 / 0.6 / 0.25）。
 * 网页预览 viewer/animations.js 的 sauce_station 场景与本类对应。
 * 作者：mryt233 */
public class SauceStationRenderer implements BlockEntityRenderer<SauceStationBlockEntity> {
    /** 葱花 / 香菜碎末每一层：高度（碗口往下）、贴图取的范围、贴图转几个 90°。 */
    private static final float[][] GARNISH_LAYERS = {
        {1.3F, 2, 2, 14, 14, 0}, {0.95F, 0, 4, 12, 16, 1}, {0.6F, 4, 0, 16, 12, 2}, {0.25F, 3, 3, 13, 13, 3}};

    public SauceStationRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(SauceStationBlockEntity be, float tickDelta, PoseStack matrices, MultiBufferSource consumers,
                       int light, int overlay) {
        Direction facing = be.getBlockState().getValue(SauceStationBlock.FACING);
        VertexConsumer vc = consumers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        // 缺图兜底那条路用白色独立贴图（misc/white.png 不在方块图集里，得单开一个 RenderType）
        VertexConsumer white = null;
        matrices.pushPose();
        matrices.translate(0.5, 0, 0.5);
        // 和方块状态一样转：模型正面朝北，facing 东 / 南 / 西 = 绕 y 顺时针 90 / 180 / 270°
        matrices.mulPose(Axis.YP.rotationDegrees(-modelRotation(facing)));
        matrices.translate(-0.5, 0, -0.5);
        PoseStack.Pose e = matrices.last();
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
            // null = 这张贴图不在图集里（新调料忘了生成 sauce_<id>）：退回白贴图 + 顶点色，别露出粉黑格子
            TextureAtlasSprite sprite = surfaceSprite(s, garnish);
            if (sprite == null && white == null) {
                white = consumers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            }
            VertexConsumer into = sprite == null ? white : vc;
            // 顶点色只有 RGB，alpha 统一 255；白贴图那条路靠它才有调料色（正常贴图传纯白）
            int rgb = sprite == null ? s.color() & 0xFFFFFF : 0xFFFFFF;
            if (garnish) {
                for (float[] l : GARNISH_LAYERS) {
                    quad(into, e, light, sprite, cx - half, rim - l[0], cz - half, cx + half, cz + half, l[1], l[2], l[3], l[4], (int) l[5], rgb);
                }
            } else {
                quad(into, e, light, sprite, cx - half, rim - 0.5F, cz - half, cx + half, cz + half, 2, 2, 14, 14, 0, rgb);
            }
        }
        matrices.popPose();
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

    /** 缺图时退回的白贴图：和原版粉黑格子一样是 16×16 纯白，靠顶点色染成调料色。 */
    private static final ResourceLocation WHITE = HotPot.id("textures/misc/white.png");

    /** 缺图已经提醒过一遍的调料（只在渲染线程用；每帧都刷一条日志会把 latest.log 淹掉）。 */
    private static final Set<String> WARNED = new HashSet<>();

    /**
     * 台上小碗的调料面精灵；这张贴图不在图集里时返回 null，由调用方退回白贴图 + 顶点色。
     * mryt233 改：r137 / r139 的新调料当初就没有 sauce_&lt;id&gt;，台上 5 个碗是粉黑格子。
     * 原版图集对缺图统一返回同一个缺失精灵实例（{@code getOrDefault(id, missingSprite)}），用它来判定。
     */
    private static TextureAtlasSprite surfaceSprite(Seasoning s, boolean garnish) {
        TextureAtlasSprite sp = sprite(HotPot.id(garnish ? "block/" + s.id() : "block/sauce_" + s.id()));
        TextureAtlasSprite missing = sprite(MissingTextureAtlasSprite.getLocation());
        if (sp.contents() == missing.contents()) {
            if (WARNED.add(s.id())) {
                HotPot.LOGGER.warn("sauce station: no texture for seasoning {}（补跑 tools/gen_sauce_station.py）", s.id());
            }
            return null;
        }
        return sp;
    }

    /**
     * 一张朝上的面（模型像素坐标），贴图取精灵的 [u0,v0]..[u1,v1]（0..16），转 rot 个 90°；rgb 是顶点色。
     * sp 为 null 时按白贴图那条路画：UV 直接取 0..1（整张纯白，取哪块都一样）。
     */
    private static void quad(VertexConsumer vc, PoseStack.Pose e, int light, TextureAtlasSprite sp, float x0, float y, float z0,
                             float x1, float z1, float u0, float v0, float u1, float v1, int rot, int rgb) {
        float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};   // 西北、东北、东南、西南四个角
        float[][] pos = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        int[] order = {3, 2, 1, 0};   // 从上往下看逆时针：西南、东南、东北、西北
        for (int k : order) {
            float[] t = uv[(k + rot) % 4];
            float tu = sp == null ? t[0] / 16F : sp.getU(t[0] / 16F);
            float tv = sp == null ? t[1] / 16F : sp.getV(t[1] / 16F);
            vc.addVertex(e, pos[k][0] / 16F, y / 16F, pos[k][1] / 16F).setColor(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255)
                .setUv(tu, tv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(e, 0, 1, 0);
        }
    }
}
