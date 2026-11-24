// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.WoodenVatBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 开口大木桶渲染器：按豆浆液量实时渲染桶内液面高度与着色、浮沉点卤物品与随压制进度下沉的 3D 木压板。
 * 与 viewer/animations.js 中的 wooden_vat 场景一比一对应。
 * 作者：mryt233
 */
public class WoodenVatRenderer implements BlockEntityRenderer<WoodenVatBlockEntity> {
    private static final ResourceLocation TEX_LID = HotPot.id("block/wooden_vat_lid");
    private static final ResourceLocation TEX_WOOD = HotPot.id("block/wooden_vat_planks");
    private static final ResourceLocation TEX_HANDLE = HotPot.id("block/stone_mill_wood");
    private static final ResourceLocation TEX_SOYMILK = HotPot.id("block/soymilk_still");

    public WoodenVatRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(WoodenVatBlockEntity be, float tickDelta, PoseStack matrices, MultiBufferSource consumers,
                       int light, int overlay) {
        float fill = be.fillFraction();
        boolean hasOutput = !be.output().isEmpty();
        float press = be.progressRatio(tickDelta);
        float baseLidY = WoodenVatBlockEntity.lidBottomY(hasOutput, press);
        boolean hasLiquid = ((!be.fluid().isEmpty() && be.fluidUses() > 0) || hasOutput) && fill > 0F;
        float liquidY = WoodenVatBlockEntity.effectiveLiquidY(fill, be.hasLid(), hasOutput, press);

        if (hasLiquid) {
            TextureAtlasSprite liq = RenderSprites.sprite(TEX_SOYMILK);
            int argb = be.color() | 0xFF000000;
            VertexConsumer tvc = consumers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            RendererBoxes.topQuad(matrices, tvc, liq, light, 2.5F, liquidY, 2.5F, 13.5F, 13.5F, argb);
        }

        ItemStack shown = hasOutput ? be.output() : be.input();
        if (!shown.isEmpty() && !be.hasLid()) {
            double y = hasOutput
                ? (hasLiquid ? (liquidY + 0.35) / 16.0 : 4.1 / 16.0)
                : (hasLiquid ? (liquidY + 0.25) / 16.0 : 4.1 / 16.0);
            matrices.pushPose();
            matrices.translate(0.5, y, 0.5);
            matrices.mulPose(Axis.XP.rotationDegrees(90F));
            matrices.scale(0.45F, 0.45F, 0.45F);
            Minecraft.getInstance().getItemRenderer().renderStatic(shown, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, matrices, consumers, be.getLevel(), 0);
            matrices.popPose();
        }

        if (be.hasLid()) {
            TextureAtlasSprite lidSprite = RenderSprites.sprite(TEX_LID);
            TextureAtlasSprite woodSprite = RenderSprites.sprite(TEX_WOOD);
            TextureAtlasSprite handleSprite = RenderSprites.sprite(TEX_HANDLE);
            VertexConsumer vc = consumers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
            matrices.pushPose();
            matrices.translate(0.0, baseLidY / 16.0, 0.0);
            RendererBoxes.modelBox(matrices, vc, lidSprite, light, 3.0F, 0.0F, 4.0F, 13.0F, 1.5F, 12.0F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, lidSprite, light, 4.0F, 0.0F, 3.0F, 12.0F, 1.5F, 4.0F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, lidSprite, light, 4.0F, 0.0F, 12.0F, 12.0F, 1.5F, 13.0F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, woodSprite, light, 3.5F, 1.5F, 4.75F, 12.5F, 2.5F, 6.25F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, woodSprite, light, 3.5F, 1.5F, 9.75F, 12.5F, 2.5F, 11.25F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, handleSprite, light, 7.25F, 2.5F, 4.9F, 8.75F, 3.5F, 6.1F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, handleSprite, light, 7.25F, 2.5F, 9.9F, 8.75F, 3.5F, 11.1F, 0xFFFFFFFF);
            RendererBoxes.modelBox(matrices, vc, handleSprite, light, 7.0F, 3.5F, 4.75F, 9.0F, 4.5F, 11.25F, 0xFFFFFFFF);
            matrices.popPose();
        }
    }
}
