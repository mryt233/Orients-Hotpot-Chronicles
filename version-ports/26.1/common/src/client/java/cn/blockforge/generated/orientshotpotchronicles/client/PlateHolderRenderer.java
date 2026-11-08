package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopstickRestBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import cn.blockforge.orient.framework.client.RenderFrame;
import cn.blockforge.generated.orientshotpotchronicles.client.RenderItems;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import cn.blockforge.generated.orientshotpotchronicles.client.RenderItems;
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
public final class PlateHolderRenderer extends cn.blockforge.orient.framework.client.FrameBlockEntityRenderer<PlateHolderBlockEntity> {
    private final RenderItems itemRenderer;
    private final RenderItems blocks;

    public PlateHolderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = RenderItems.INSTANCE;
        this.blocks = RenderItems.INSTANCE;
    }

    /** 画哪个模型：见 {@link cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock#modelState}（挖掉时的碎屑也用它）。 */
    public static BlockState modelState(PlateHolderBlockEntity.Placed p) {
        return cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.modelState(p);
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
            if (p.plate instanceof ChopstickRestBlock) {
                // 直接摆在桌上的竹筷：方块本身没有模型 json（r136b，2026-11-05 用户），
                // 迷你筷架 + 垫在上面的一双筷子全由代码画
                matrices.pushPose();
                matrices.translate(0.5, 0, 0.5);
                if (p.axis == Direction.Axis.Z) {
                    matrices.mulPose(Axis.YP.rotationDegrees(90F));   // 和盘子 blockstate 的 y: 90 一致
                }
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

    /** 筷子架在碗沿上占多大：物品贴图 16 像素 × 这个比例，斜着的两根筷子约 12 像素长，比碗宽一截（2026-10-27）。 */
    private static final float CHOPSTICKS_SCALE = 0.6F;
    // ---------------------------------------------------------------- 桌上的迷你筷架与竹筷

    /**
     * 迷你筷架（像素）：长边横跨在一双筷子下面，两端微翘，中间是搁筷尖的鞍谷（2026-11-05 用户：
     * 长约 2.5 像素、宽约 1.2 像素、高约 1.2 像素、两端微翘约 1.5 像素）。
     * 筷子经下面的变换后沿方块的 x 轴铺开，所以架子长边沿 z（横着托住两根筷子）。
     */
    private static final double REST_LEN = 2.5;     // 长（沿 z）：横跨一双筷子
    private static final double REST_DEPTH = 1.2;   // 宽（沿 x）：沿筷子方向
    private static final double REST_H = 1.2;       // 鞍谷高度，筷尖就搁在这上面
    private static final double REST_LIP = 1.5;     // 两端微翘的翘唇高度
    /** 垫底用的深色木料（白瓷鞍身坐在深色木托上，像宋式漆器筷架）。 */
    private static final net.minecraft.world.level.block.Block REST_BASE = net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS;

    /**
     * 桌上那双竹筷的大小：整张贴图 16 像素 × 0.34 ≈ 5.4 像素，一双筷子（贴图上沿对角线铺开）
     * 平躺下来约 5 像素长，正好落在借来的 5×5 像素小碟格子里（尾端允许略微探出一点，像真把筷子搁在筷架上）。
     */
    private static final float TABLE_CHOPSTICKS_SCALE = 0.34F;
    /** 尾端贴桌面、尖端架在迷你筷架上微翘的斜度（2026-11-05 用户：约 6°）。 */
    private static final float TABLE_CHOPSTICKS_TILT = 6F;

    /**
     * 架在碗沿 / 盘沿上的筷子（2026-10-27 用户，新增）：平躺在碗口上方一点，筷子尖在碗正中上方、另一头搭出碗沿；
     * 夹着菜时 HeldFoodItemRenderer 把菜画在筷子尖上，跟着一起躺平。
     */
    private void renderChopsticks(PlateHolderBlockEntity.Placed p, PoseStack matrices,
                                  RenderFrame vertexConsumers, int light) {
        matrices.pushPose();
        matrices.translate(0.5, (PlateHolderBlockEntity.heightOf(p) + 0.3) / 16.0, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees((p.axis == Direction.Axis.X ? 0 : 90) + 45));
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        matrices.scale(CHOPSTICKS_SCALE, CHOPSTICKS_SCALE, CHOPSTICKS_SCALE);
        // 筷子尖（夹着的菜在这儿）挪到碗正中上方，另一头搭在碗沿外（物品显示 fixed 转了 180°，尖在 +x、-y）
        matrices.translate(-0.26F, 0.3F, 0F);
        itemRenderer.renderStatic(p.chopsticks, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
            matrices, vertexConsumers, null, 0);
        matrices.popPose();
    }


    /**
     * 迷你筷架（r136b，2026-11-05 用户，新增）：一只白瓷质感的小鞍形架子——
     * 平滑石英做鞍身与两端微翘的翘唇，深色橡木做垫底。筷尖就搁在中间那道鞍谷上，两根筷子都垫离桌面。
     * 用 {@code renderSingleBlock} 配合缩放矩阵画出各个小方块（和 BenchLegsRenderer 画条凳腿同样的做法）。
     */
    private void renderMiniChopstickRest(PoseStack matrices, RenderFrame vertexConsumers, int light) {
        BlockState porcelain = net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState wood = REST_BASE.defaultBlockState();
        double cx = 8;   // 方块正中；筷子也摆在这一列上
        double cz = 8;
        double halfL = REST_LEN / 2;     // 沿 z：横跨一双筷子
        double halfD = REST_DEPTH / 2;   // 沿 x：沿筷子方向
        // 木托：比鞍身略宽一圈的深色木底座
        miniBox(matrices, vertexConsumers, light, wood,
            cx - halfD - 0.1, cx + halfD + 0.1, 0, 0.35, cz - halfL - 0.1, cz + halfL + 0.1);
        // 鞍身：中间那道让筷尖落进去的谷
        miniBox(matrices, vertexConsumers, light, porcelain,
            cx - halfD + 0.05, cx + halfD - 0.05, 0.35, REST_H, cz - halfL + 0.15, cz + halfL - 0.15);
        // 两端微翘的两片翘唇
        miniBox(matrices, vertexConsumers, light, porcelain,
            cx - halfD + 0.05, cx + halfD - 0.05, 0.35, REST_LIP, cz - halfL, cz - halfL + 0.4);
        miniBox(matrices, vertexConsumers, light, porcelain,
            cx - halfD + 0.05, cx + halfD - 0.05, 0.35, REST_LIP, cz + halfL - 0.4, cz + halfL);
    }

    /** 用原版方块模型画一个小方盒（像素坐标）——整块立方体按 (x1-x0, y1-y0, z1-z0) 缩放。 */
    private void miniBox(PoseStack matrices, RenderFrame vertexConsumers, int light, BlockState state,
                         double x0, double x1, double y0, double y1, double z0, double z1) {
        matrices.pushPose();
        matrices.translate(x0 / 16.0, y0 / 16.0, z0 / 16.0);
        matrices.scale((float) ((x1 - x0) / 16.0), (float) ((y1 - y0) / 16.0), (float) ((z1 - z0) / 16.0));
        blocks.renderSingleBlock(state, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
        matrices.popPose();
    }

    /**
     * 垫在迷你筷架上的这双竹筷（r136b，2026-11-05 用户，新增）：平躺在桌面上方约 1.2 像素处，
     * 绕垂直于筷子的水平轴微倾约 6°——筷尖落在鞍谷上、尾端贴向桌面。
     * 夹着菜时 HeldFoodItemRenderer 会把菜画在筷尖上（尖在迷你筷架这一头）。
     */
    private void renderTableChopsticks(PlateHolderBlockEntity.Placed p, PoseStack matrices,
                                       RenderFrame vertexConsumers, int light) {
        matrices.pushPose();
        // 原点放在迷你筷架的鞍心：筷尖就搁在这儿，另一头朝 -x 铺开、落到桌面上
        matrices.translate(0.5, REST_H / 16.0, 0.5);
        // 绕横跨方块的 z 轴微倾：尾端（-x 那头）落到桌面，尖端（原点这头）被迷你筷架垫起
        matrices.mulPose(Axis.ZP.rotationDegrees(TABLE_CHOPSTICKS_TILT));
        // 压平这双筷子（立着的贴图躺下来），再转成沿方块的 x 轴铺开
        matrices.mulPose(Axis.YP.rotationDegrees(-45F));
        matrices.mulPose(Axis.XP.rotationDegrees(90F));
        matrices.scale(TABLE_CHOPSTICKS_SCALE, TABLE_CHOPSTICKS_SCALE, TABLE_CHOPSTICKS_SCALE);
        // 与架在碗沿上同一套算法：贴图里的筷子尖（夹着的菜画在这一头）挪到变换原点上，正落在迷你筷架
        matrices.translate(-0.26F, 0.3F, 0F);
        itemRenderer.renderStatic(p.self, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
            matrices, vertexConsumers, null, 0);
        matrices.popPose();
    }
}
