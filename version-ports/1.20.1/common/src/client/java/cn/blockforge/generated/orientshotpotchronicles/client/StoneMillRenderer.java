// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillMotion;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.WoodenVatBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import static cn.blockforge.generated.orientshotpotchronicles.client.StoneMillGeometry.*;

/** 小石磨的转动上盘与槽内液流；固定下盘仍由方块模型绘制（1.20.1 适配）。 */
public class StoneMillRenderer implements BlockEntityRenderer<StoneMillBlockEntity> {
    private static final ResourceLocation TEX_STONE = new ResourceLocation("minecraft", "block/cobblestone");
    private static final ResourceLocation TEX_WOOD = HotPot.id("block/stone_mill_wood");
    private static final ResourceLocation TEX_LIQUID = HotPot.id("block/soymilk_still");
    private static final ResourceLocation TEX_WATER = new ResourceLocation("minecraft", "block/water_still");
    private static final float A = OUTER_LEFT + .03F, B = INNER_LEFT - .03F;
    private static final float C = INNER_RIGHT + .03F, D = OUTER_RIGHT - .03F;
    private static final float LEFT = MOUTH_LEFT + .02F, RIGHT = MOUTH_RIGHT - .02F;
    private static final float SIDE_TRACK = (OUTER_LEFT + INNER_LEFT) * .5F;
    private static final float FRONT_TRACK = (OUTER_RIGHT + INNER_RIGHT) * .5F;
    private static final float[][] SEAM_POINTS = {
        {5, INNER_RIGHT + .26F}, {10.5F, INNER_RIGHT + .26F},
        {INNER_LEFT - .26F, 7}, {INNER_RIGHT + .26F, 9}, {8, INNER_LEFT - .26F},
    };

    public StoneMillRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(StoneMillBlockEntity be, float tickDelta, PoseStack pose, MultiBufferSource consumers,
                       int light, int overlay) {
        // mryt233 改：只响应已同步的成功推磨；转动、渗流及出浆共用游戏刻时钟。
        double now = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + (double) tickDelta;
        StoneMillMotion motion = be.clientMotion();
        motion.update(be.quarterTurns(), now);
        float age = motion.age(now);
        boolean pending = !be.troughResult().isEmpty();
        boolean finish = be.renderGrindSpill();
        Direction facing = be.getBlockState().hasProperty(StoneMillBlock.FACING)
            ? be.getBlockState().getValue(StoneMillBlock.FACING) : Direction.SOUTH;
        boolean intoVat = be.getLevel() != null
            && be.getLevel().getBlockState(be.getBlockPos().relative(facing)).getBlock() instanceof WoodenVatBlock;
        boolean autoPouring = !pending && finish && intoVat && age < StoneMillMotion.SPILL_TICKS;
        boolean renderPending = pending || autoPouring;
        float wet = StoneMillMotion.wetIntensity(age, be.renderGrindWet(), finish, renderPending);
        float spill = StoneMillMotion.spillIntensity(age, finish, renderPending);

        pose.pushPose();
        rotateAroundCenter(pose, facing.toYRot());
        if (wet > 0 || renderPending || be.waterUses() > 0) {
            boolean milk = wet > 0 || renderPending;
            int tint = pending ? be.troughColor() | 0xFF000000
                : milk ? be.renderGrindColor() | 0xFF000000 : 0xCC3F76E4;
            TextureAtlasSprite sprite = RenderSprites.sprite(milk ? TEX_LIQUID : TEX_WATER);
            VertexConsumer liquid = consumers.getBuffer(milk ? RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)
                : RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
            drawLiquid(pose, liquid, sprite, light, tint, age, renderPending, wet, spill, autoPouring);
        }

        rotateAroundCenter(pose, (float) ((motion.quarterAt(now) % 4.0) * 90.0));
        TextureAtlasSprite stone = RenderSprites.sprite(TEX_STONE);
        TextureAtlasSprite wood = RenderSprites.sprite(TEX_WOOD);
        // mryt233 改：与静态下盘模型一致采用实心背面剔除着色层。
        VertexConsumer solid = consumers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        for (int[] box : TOP) {
            RendererBoxes.modelBox(pose, solid, box[6] == 0 ? stone : wood, light,
                box[0], box[1], box[2], box[3], box[4], box[5], 0xFFFFFFFF);
        }

        ItemStack input = be.input();
        if (!input.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, (SEAM + 4.4) / 16.0, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.scale(0.24F, 0.24F, 0.24F);
            Minecraft.getInstance().getItemRenderer().renderStatic(input, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, consumers, be.getLevel(), 0);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void rotateAroundCenter(PoseStack pose, float degrees) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YN.rotationDegrees(degrees));
        pose.translate(-0.5, 0, -0.5);
    }

    private static void drawLiquid(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite, int light,
                                   int tint, float age, boolean pending, float wet, float spill, boolean pourIntoVat) {
        float y = FLOOR + (pending ? .8F : .18F) + wet * .18F;
        RendererBoxes.topQuad(pose, out, sprite, light, A, y, A, D, B, tint);
        RendererBoxes.topQuad(pose, out, sprite, light, A, y, B, B, D, tint);
        RendererBoxes.topQuad(pose, out, sprite, light, C, y, B, D, D, tint);
        RendererBoxes.topQuad(pose, out, sprite, light, B, y, C, LEFT, D, tint);
        RendererBoxes.topQuad(pose, out, sprite, light, RIGHT, y, C, C, D, tint);
        float reach = spill > .02F ? MOUTH_Z : D + (pending ? .65F : wet * .5F);
        RendererBoxes.topQuad(pose, out, sprite, light, LEFT, y, C, RIGHT, reach, tint);

        if (wet > .02F) {
            for (int i = 0; i < SEAM_POINTS.length; i++) {
                float x = SEAM_POINTS[i][0], z = SEAM_POINTS[i][1];
                float p = frac(age * .16F + i * .21F);
                float top = SEAM - p * (SEAM - y);
                float radius = .11F + .13F * wet;
                RendererBoxes.box(pose, out, sprite, light, x - radius, Math.max(y, top - .55F), z - radius,
                    x + radius, top, z + radius, tint);
            }
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    float p = frac(age * .06F + i / 3F + side * .07F);
                    float x0 = side < 0 ? SIDE_TRACK : 16 - SIDE_TRACK;
                    float x1 = 8 + side * .55F;
                    boolean fromSide = p < .55F;
                    float x = fromSide ? x0 : x0 + (x1 - x0) * (p - .55F) / .45F;
                    float z = fromSide ? SIDE_TRACK + (FRONT_TRACK - SIDE_TRACK) * p / .55F : FRONT_TRACK;
                    float w = fromSide ? .15F : .35F, d = fromSide ? .35F : .15F;
                    RendererBoxes.topQuad(pose, out, sprite, light, x - w, y + .025F, z - d, x + w, z + d, 0xFFFFFFFF);
                }
            }
        }
        if (spill > .02F) {
            // mryt233 改：若前方紧邻大木桶，越过 14 px 桶沿并直落桶内；否则只在嘴口短促悬停收口，不生成地面液体。
            float w = .18F + .61F * spill;
            if (pourIntoVat) {
                float jetBottom = Math.max(14.18F, y - .35F);
                RendererBoxes.box(pose, out, sprite, light, 8 - w, jetBottom, MOUTH_Z - .15F,
                    8 + w, y + .02F, MOUTH_Z + 3.15F, tint);
                RendererBoxes.box(pose, out, sprite, light, 8 - w * .82F, 14.2F - 2.65F * spill, MOUTH_Z + 2.65F,
                    8 + w * .82F, Math.max(14.22F, y - .15F), MOUTH_Z + 3.15F, tint);
            } else {
                RendererBoxes.box(pose, out, sprite, light, 8 - w, y - .23F, MOUTH_Z - .22F,
                    8 + w, y + .02F, MOUTH_Z + .36F, tint);
                RendererBoxes.box(pose, out, sprite, light, 8 - w * .82F, y - 1.1F - 3.6F * spill, MOUTH_Z + .04F,
                    8 + w * .82F, y - .16F, MOUTH_Z + .36F, tint);
            }
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
