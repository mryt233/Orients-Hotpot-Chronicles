package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopstickRestBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;

/**
 * 画一格里摆着的每个盘子和盘里的菜。盘子用它自己的方块模型（中心在方块中央、按 AXIS 选方向），
 * 平移到它的位置；菜怎么摆见 {@link PlateFoodRenderer}（手里 / 物品栏里的盘子也用它）。
 * <p>
 * 直接摆在桌面上的竹筷（{@link ChopstickRestBlock}，r136b 新增，2026-11-05 用户）不画方块模型，
 * 由这里用代码画一个迷你筷架，再把筷子垫在架子上——方块本身没有模型 json。
 */
public final class PlateHolderRenderer implements BlockEntityRenderer<PlateHolderBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blocks;

    public PlateHolderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.blocks = Minecraft.getInstance().getBlockRenderer();
    }

    /** 画哪个模型：见 {@link cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock#modelState}（挖掉时的碎屑也用它）。 */
    public static BlockState modelState(PlateHolderBlockEntity.Placed p) {
        return cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.modelState(p);
    }

    @Override
    public void render(PlateHolderBlockEntity be, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        BlockState holder = be.getBlockState();
        if (!(holder.getBlock() instanceof PlateHolderBlock)) {
            return;
        }
        double lift = PlateHolderBlock.lift(holder);
        int seed = (int) be.getBlockPos().asLong();
        for (PlateHolderBlockEntity.Placed p : be.plates()) {
            matrices.pushPose();
            matrices.translate((p.x - 8) / 16.0, lift / 16.0, (p.z - 8) / 16.0);
            if (p.plate instanceof ChopstickRestBlock) {
                // 直接摆在桌上的竹筷：方块本身没有模型 json（r136b，2026-11-05 用户），
                // 迷你筷架 + 垫在上面的一双筷子全由代码画
                matrices.pushPose();
                matrices.translate(0.5, 0, 0.5);
                // mryt233 改：局部 +x 是筷尖，依照完整四向旋转。
                matrices.mulPose(Axis.YP.rotationDegrees(-90F - p.facing.toYRot()));
                matrices.translate(-0.5, 0, -0.5);
                renderMiniChopstickRest(matrices, vertexConsumers, light);
                renderTableChopsticks(p, matrices, vertexConsumers, light);
                matrices.popPose();
                matrices.popPose();
                continue;
            }
            blocks.renderSingleBlock(modelState(p), matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
            if (p.self.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem) {
                SauceRender.surface(cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem.contents(p.self).mask(), matrices, vertexConsumers, light);
            }
            PlateFoodRenderer.render(p.plate.size(), p.axis, p.items, matrices, vertexConsumers, light,
                be.getLevel(), seed, itemRenderer);
            if (!p.chopsticks.isEmpty()) {
                renderChopsticks(p, matrices, vertexConsumers, light);
            }
            matrices.popPose();
        }
    }

    // mryt233 改（r146e）：与 viewer/animations.js 共用同一套像素尺寸和变换顺序。
    private static final double LENGTH = 8.5, THICK = 0.55, TIP_THICK = 0.38, GAP = 1.0;
    private static final double HANDLE = LENGTH * 0.45;
    private static final double REST_LEN = 3.8, REST_DEPTH = 1.2, REST_H = 1.2, REST_LIP = 1.6;
    // 支点在全长 88%；鞍谷靠尾端的边缘接触竹条下表面，向前的部分不会穿入竹条。
    private static final double TILT = Math.atan(REST_H / (LENGTH * 0.88));
    private static final double TAIL_X = 8 - LENGTH * Math.cos(TILT) / 2;
    private static final double REST_X = TAIL_X + REST_H / Math.tan(TILT) + 0.55;
    private static final net.minecraft.resources.ResourceLocation BAMBOO =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orients_hotpot_chronicles", "textures/block/resting_bamboo.png");

    private void renderChopsticks(PlateHolderBlockEntity.Placed p, PoseStack matrices,
                                  MultiBufferSource buffers, int light) {
        matrices.pushPose();
        matrices.translate(0.5, PlateHolderBlockEntity.heightOf(p) / 16, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(-90F - p.chopsticksYaw));
        // 尖端位于碗心；后段横搭近侧碗沿，尾端自然探出。
        matrices.translate(-LENGTH / 16, 0, 0);
        renderBamboo(matrices, buffers, light, p.chopsticks);
        matrices.popPose();
    }

    private void renderMiniChopstickRest(PoseStack matrices, MultiBufferSource buffers, int light) {
        BlockState porcelain = net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState wood = net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS.defaultBlockState();
        double halfL = REST_LEN / 2, halfD = REST_DEPTH / 2;
        miniBox(matrices, buffers, light, wood, REST_X-halfD-0.1, REST_X+halfD+0.1, 0, 0.35, 8-halfL-0.1, 8+halfL+0.1);
        miniBox(matrices, buffers, light, porcelain, REST_X-halfD+0.05, REST_X+halfD-0.05, 0.35, REST_H, 8-halfL+0.15, 8+halfL-0.15);
        miniBox(matrices, buffers, light, porcelain, REST_X-halfD+0.05, REST_X+halfD-0.05, 0.35, REST_LIP, 8-halfL, 8-halfL+0.4);
        miniBox(matrices, buffers, light, porcelain, REST_X-halfD+0.05, REST_X+halfD-0.05, 0.35, REST_LIP, 8+halfL-0.4, 8+halfL);
    }

    private void miniBox(PoseStack matrices, MultiBufferSource buffers, int light, BlockState state,
                         double x0, double x1, double y0, double y1, double z0, double z1) {
        matrices.pushPose();
        matrices.translate(x0 / 16, y0 / 16, z0 / 16);
        matrices.scale((float)((x1-x0)/16), (float)((y1-y0)/16), (float)((z1-z0)/16));
        blocks.renderSingleBlock(state, matrices, buffers, light, OverlayTexture.NO_OVERLAY);
        matrices.popPose();
    }

    private void renderTableChopsticks(PlateHolderBlockEntity.Placed p, PoseStack matrices,
                                       MultiBufferSource buffers, int light) {
        matrices.pushPose();
        matrices.translate(TAIL_X / 16, 0, 0.5);
        matrices.mulPose(Axis.ZP.rotation((float)TILT));
        renderBamboo(matrices, buffers, light, p.self);
        matrices.popPose();
    }

    private void renderBamboo(PoseStack matrices, MultiBufferSource buffers, int light, ItemStack stack) {
        var vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(BAMBOO));
        for (int side : new int[]{-1, 1}) {
            double z = side * (GAP + THICK) / 2;
            bambooBox(matrices, vc, light, 0, HANDLE, THICK, z, 0.25F);
            bambooBox(matrices, vc, light, HANDLE, LENGTH, TIP_THICK, z, 0.75F);
        }
        ItemStack food = cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks.held(stack);
        if (!food.isEmpty()) {
            matrices.pushPose();
            // 菜的厚度垂直，两根筷尖插到菜的近侧边缘；整段跟随桌面竹筷的倾斜。
            matrices.translate(LENGTH / 16, (TIP_THICK / 2 + 1.4) / 16, 0);
            matrices.scale(0.175F, 0.175F, 0.175F);
            itemRenderer.renderStatic(food, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                matrices, buffers, null, 0);
            matrices.popPose();
        }
    }

    private static void bambooBox(PoseStack matrices, com.mojang.blaze3d.vertex.VertexConsumer vc, int light,
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

    private static void bambooQuad(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose, int light,
                                    float u, float nx, float ny, float nz, float... xyz) {
        for (int i=0; i<12; i+=3) {
            vc.addVertex(pose,xyz[i],xyz[i+1],xyz[i+2]).setColor(255,255,255,255).setUv(u,0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
        }
    }
}
