package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * 画一格里摆着的每个盘子和盘里的菜。盘子用它自己的方块模型（中心在方块中央、按 AXIS 选方向），
 * 平移到它的位置；菜怎么摆见 {@link PlateFoodRenderer}（手里 / 物品栏里的盘子也用它）。
 */
public final class PlateHolderRenderer implements BlockEntityRenderer<PlateHolderBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final BlockRenderManager blocks;

    public PlateHolderRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.blocks = MinecraftClient.getInstance().getBlockRenderManager();
    }

    /** 画哪个模型：见 {@link cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock#modelState}（挖掉时的碎屑也用它）。 */
    public static BlockState modelState(PlateHolderBlockEntity.Placed p) {
        return cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.modelState(p);
    }

    @Override
    public void render(PlateHolderBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState holder = be.getCachedState();
        if (!(holder.getBlock() instanceof PlateHolderBlock)) {
            return;
        }
        double lift = PlateHolderBlock.lift(holder);
        int seed = (int) be.getPos().asLong();
        for (PlateHolderBlockEntity.Placed p : be.plates()) {
            matrices.push();
            matrices.translate((p.x - 8) / 16.0, lift / 16.0, (p.z - 8) / 16.0);
            blocks.renderBlockAsEntity(modelState(p), matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            if (p.self.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem) {
                SauceRender.surface(cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem.contents(p.self).mask(), matrices, vertexConsumers, light);
            }
            PlateFoodRenderer.render(p.plate.size(), p.axis, p.items, matrices, vertexConsumers, light,
                be.getWorld(), seed, itemRenderer);
            if (!p.chopsticks.isEmpty()) {
                renderChopsticks(p, matrices, vertexConsumers, light);
            }
            matrices.pop();
        }
    }

    /** 筷子架在碗沿上占多大：物品贴图 16 像素 × 这个比例，斜着的两根筷子约 12 像素长，比碗宽一截（2026-10-27）。 */
    private static final float CHOPSTICKS_SCALE = 0.6F;

    /**
     * 架在碗沿 / 盘沿上的筷子（2026-10-27 用户，）：平躺在碗口上方一点，筷子尖在碗正中上方、另一头搭出碗沿；
     * 夹着菜时 HeldFoodItemRenderer 把菜画在筷子尖上，跟着一起躺平。
     */
    private void renderChopsticks(PlateHolderBlockEntity.Placed p, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.5, (PlateHolderBlockEntity.heightOf(p) + 0.3) / 16.0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((p.axis == Direction.Axis.X ? 0 : 90) + 45));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrices.scale(CHOPSTICKS_SCALE, CHOPSTICKS_SCALE, CHOPSTICKS_SCALE);
        // 筷子尖（夹着的菜在这儿）挪到碗正中上方，另一头搭在碗沿外（物品显示 fixed 转了 180°，尖在 +x、-y）
        matrices.translate(-0.26F, 0.3F, 0F);
        itemRenderer.renderItem(p.chopsticks, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
            matrices, vertexConsumers, null, 0);
        matrices.pop();
    }
}
