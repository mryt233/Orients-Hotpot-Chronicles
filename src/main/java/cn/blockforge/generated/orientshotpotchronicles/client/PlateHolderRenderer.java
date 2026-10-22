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
            blocks.renderBlockAsEntity(p.plate.getDefaultState().with(PlateBlock.AXIS, p.axis),
                matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            PlateFoodRenderer.render(p.plate.size(), p.axis, p.items, matrices, vertexConsumers, light,
                be.getWorld(), seed, itemRenderer);
            matrices.pop();
        }
    }
}
