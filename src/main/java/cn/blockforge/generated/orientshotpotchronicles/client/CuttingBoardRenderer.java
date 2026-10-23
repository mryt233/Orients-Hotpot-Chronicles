package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.CuttingBoardBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.CuttingBoardBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * 砧板上放着的东西（2026-10-23）：平躺在板面中间，半格大小，像原版物品展示框里的样子；
 * 捣丸子捣到一半时换成捶碎 / 肉浆的样子（用户：每一下都要有变化，而且要和原版食材放上去一样是立体的）。
 * 网页预览 viewer/animations.js 的 cutting_board 场景与本类对应。
 *
 */
public class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity> {
    public CuttingBoardRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(CuttingBoardBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers,
                       int light, int overlay) {
        ItemStack stack = be.item();
        if (stack.isEmpty()) {
            return;
        }
        // 捣丸子的中间阶段（捶碎 / 肉浆）：换成对应样子的物品，和生肉一样按物品画，是立体的
        stack = cn.blockforge.generated.orientshotpotchronicles.hotpot.Pounding.display(stack, be.cuts());
        matrices.push();
        matrices.translate(0.5, CuttingBoardBlock.TOP + 0.02, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrices.scale(0.5F, 0.5F, 0.5F);
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light,
            OverlayTexture.DEFAULT_UV, matrices, consumers, be.getWorld(), 0);
        matrices.pop();
    }
}
