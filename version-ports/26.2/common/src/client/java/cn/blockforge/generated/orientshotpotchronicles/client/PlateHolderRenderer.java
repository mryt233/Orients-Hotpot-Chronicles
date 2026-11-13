// mryt233 改：26.2 RenderFrame / RenderItems / Identifier API。
package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（4H）：特殊桌面方块与整件叠加绘制改走 TableBlockRenderers 回调表。
import cn.blockforge.orient.framework.client.FrameBlockEntityRenderer;
import cn.blockforge.orient.framework.client.RenderFrame;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBowlItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 画一格里摆着的每个盘子和盘里的菜。盘子用它自己的方块模型，
 * 通过 {@link TableBlockRenderers} 登记的 {@link #renderChopstickRest} 画桌面竹筷与筷架。
 * 作者：mryt233
 */
public final class PlateHolderRenderer extends FrameBlockEntityRenderer<PlateHolderBlockEntity> {
    private final RenderItems itemRenderer;
    private final RenderItems blocks;

    public PlateHolderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = RenderItems.INSTANCE;
        this.blocks = RenderItems.INSTANCE;
    }

    /** 画哪个模型：见 {@link PlateHolderBlock#modelState}（挖掉时的碎屑也用它）。 */
    public static BlockState modelState(PlateHolderBlockEntity.Placed p) {
        return PlateHolderBlock.modelState(p);
    }

    @Override
    protected void extractFrame(PlateHolderBlockEntity be, float tickDelta, PoseStack matrices,
                                RenderFrame vertexConsumers, int light, int overlay) {
        BlockState holder = be.getBlockState();
        if (!(holder.getBlock() instanceof PlateHolderBlock)) {
            return;
        }
        double lift = PlateHolderBlock.lift(holder);
        int seed = (int) be.getBlockPos().asLong();
        for (PlateHolderBlockEntity.Placed p : be.plates()) {
            matrices.pushPose();
            matrices.translate((p.x - 8) / 16.0, lift / 16.0, (p.z - 8) / 16.0);
            TableBlockRenderers.BlockRenderer blockRenderer = TableBlockRenderers.blockRenderer(p.plate);
            if (blockRenderer != null) {
                blockRenderer.render(be, p, matrices, vertexConsumers, light, overlay, itemRenderer, seed);
                matrices.popPose();
                continue;
            }
            blocks.renderSingleBlock(modelState(p), matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
            TableBlockRenderers.SelfOverlayRenderer overlayRenderer =
                TableBlockRenderers.overlayRenderer(p.self.getItem());
            if (overlayRenderer != null) {
                overlayRenderer.render(p, matrices, vertexConsumers, light);
            } else if (p.self.getItem() instanceof SoupBowlItem bowl) {
                SoupDef def = bowl.soupDef(p.self);
                String raw = bowl.rawSoupId(p.self);
                if ((def != null && def.visual() == SoupVisual.OTHER) || (def == null && raw != null)) {
                    HotPotBlockEntityRenderer.renderBowlSurface(def, matrices, vertexConsumers, light);
                }
            }
            PlateFoodRenderer.render(p.plate.size(), p.axis, p.items, matrices, vertexConsumers, light,
                be.getLevel(), seed, itemRenderer);
            if (!p.chopsticks.isEmpty()) {
                renderChopsticks(p, matrices, vertexConsumers, light);
            }
            matrices.popPose();
        }
    }

    /** 直接摆在桌上的竹筷：迷你筷架 + 垫在上面的一双筷子全由代码画。 */
    public static void renderChopstickRest(PlateHolderBlockEntity be, PlateHolderBlockEntity.Placed p,
                                           PoseStack matrices, RenderFrame buffers,
                                           int light, int overlay, RenderItems itemRenderer, int seed) {
        matrices.pushPose();
        matrices.translate(0.5, 0, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(-90F - p.facing.toYRot()));
        matrices.translate(-0.5, 0, -0.5);
        renderMiniChopstickRest(itemRenderer, matrices, buffers, light);
        renderTableChopsticks(p, matrices, buffers, light);
        matrices.popPose();
    }

    private static final double LENGTH = 8.5, THICK = 0.55, TIP_THICK = 0.38, GAP = 1.0;
    private static final double HANDLE = LENGTH * 0.45;
    private static final double REST_LEN = 3.8, REST_DEPTH = 1.2, REST_H = 1.2, REST_LIP = 1.6;
    private static final double TILT = Math.atan(REST_H / (LENGTH * 0.88));
    private static final double TAIL_X = 8 - LENGTH * Math.cos(TILT) / 2;
    private static final double REST_X = TAIL_X + REST_H / Math.tan(TILT) + 0.55;
    private static final Identifier BAMBOO =
        Identifier.fromNamespaceAndPath("orients_hotpot_chronicles", "textures/block/resting_bamboo.png");

    private static void renderChopsticks(PlateHolderBlockEntity.Placed p, PoseStack matrices,
                                         RenderFrame vertexConsumers, int light) {
        matrices.pushPose();
        matrices.translate(0.5, PlateHolderBlockEntity.heightOf(p) / 16, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(-90F - p.chopsticksYaw));
        matrices.translate(-LENGTH / 16, 0, 0);
        renderBamboo(matrices, vertexConsumers, light, p.chopsticks);
        matrices.popPose();
    }

    private static void renderMiniChopstickRest(RenderItems blocks, PoseStack matrices,
                                                RenderFrame vertexConsumers, int light) {
        BlockState porcelain = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState wood = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        double halfL = REST_LEN / 2, halfD = REST_DEPTH / 2;
        miniBox(blocks, matrices, vertexConsumers, light, wood, REST_X-halfD-0.1, REST_X+halfD+0.1, 0, 0.35, 8-halfL-0.1, 8+halfL+0.1);
        miniBox(blocks, matrices, vertexConsumers, light, porcelain, REST_X-halfD+0.05, REST_X+halfD-0.05, 0.35, REST_H, 8-halfL+0.15, 8+halfL-0.15);
        miniBox(blocks, matrices, vertexConsumers, light, porcelain, REST_X-halfD+0.05, REST_X+halfD-0.05, 0.35, REST_LIP, 8-halfL, 8-halfL+0.4);
        miniBox(blocks, matrices, vertexConsumers, light, porcelain, REST_X-halfD+0.05, REST_X+halfD-0.05, 0.35, REST_LIP, 8+halfL-0.4, 8+halfL);
    }

    private static void miniBox(RenderItems blocks, PoseStack matrices, RenderFrame vertexConsumers,
                                int light, BlockState state,
                                double x0, double x1, double y0, double y1, double z0, double z1) {
        matrices.pushPose();
        matrices.translate(x0 / 16, y0 / 16, z0 / 16);
        matrices.scale((float)((x1-x0)/16), (float)((y1-y0)/16), (float)((z1-z0)/16));
        blocks.renderSingleBlock(state, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
        matrices.popPose();
    }

    private static void renderTableChopsticks(PlateHolderBlockEntity.Placed p, PoseStack matrices,
                                              RenderFrame vertexConsumers, int light) {
        matrices.pushPose();
        matrices.translate(TAIL_X / 16, 0, 0.5);
        matrices.mulPose(Axis.ZP.rotation((float)TILT));
        renderBamboo(matrices, vertexConsumers, light, p.self);
        matrices.popPose();
    }

    private static void renderBamboo(PoseStack matrices, RenderFrame vertexConsumers, int light, ItemStack stack) {
        var vc = vertexConsumers.getBuffer(RenderTypes.entityCutout(BAMBOO));
        for (int side : new int[]{-1, 1}) {
            double z = side * (GAP + THICK) / 2;
            bambooBox(matrices, vc, light, 0, HANDLE, THICK, z, 0.25F);
            bambooBox(matrices, vc, light, HANDLE, LENGTH, TIP_THICK, z, 0.75F);
        }
        ItemStack food = Chopsticks.held(stack);
        if (!food.isEmpty()) {
            matrices.pushPose();
            matrices.translate(LENGTH / 16, (TIP_THICK / 2 + 1.4) / 16, 0);
            matrices.scale(0.175F, 0.175F, 0.175F);
            vertexConsumers.item(food, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, null, 0);
            matrices.popPose();
        }
    }

    private static void bambooBox(PoseStack matrices, VertexConsumer vc, int light,
                                   double x0, double x1, double thick, double z, float u) {
        float a=(float)(x0/16), d=(float)(x1/16), b=0, f=(float)(thick/16);
        float c=(float)((z-thick/2)/16), g=(float)((z+thick/2)/16);
        var pose=matrices.last();
        bambooQuad(vc,pose,light,u,0,0,-1, d,f,c, a,f,c, a,b,c, d,b,c);
        bambooQuad(vc,pose,light,u,0,0,1, a,f,g, d,f,g, d,b,g, a,b,g);
        bambooQuad(vc,pose,light,u,-1,0,0, a,f,c, a,f,g, a,b,g, a,b,c);
        bambooQuad(vc,pose,light,u,1,0,0, d,f,g, d,f,c, d,b,c, d,b,g);
        bambooQuad(vc,pose,light,u,0,1,0, a,f,c, d,f,c, d,f,g, a,f,g);
        bambooQuad(vc,pose,light,u,0,-1,0, a,b,g, d,b,g, d,b,c, a,b,c);
    }

    private static void bambooQuad(VertexConsumer vc, PoseStack.Pose pose, int light,
                                    float u, float nx, float ny, float nz, float... xyz) {
        for (int i=0; i<12; i+=3) {
            vc.addVertex(pose,xyz[i],xyz[i+1],xyz[i+2]).setColor(255,255,255,255).setUv(u,0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
        }
    }
}
