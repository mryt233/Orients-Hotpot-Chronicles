// 作者：mryt233
// mryt233 改：26.3 姿态旋转接口。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableDecor;
import cn.blockforge.orient.framework.client.FrameBlockEntityRenderer;
import cn.blockforge.orient.framework.client.RenderFrame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 3 种备菜架（多层落地架、桌面备菜架、双态挂架）格内物品三维渲染器，
 * 与 {@link TableDecor#liftOf} 对齐桌面高度偏移，模型本体由烘焙模型包装承载。作者：mryt233
 */
public class PrepRackRenderer extends FrameBlockEntityRenderer<PrepRackBlockEntity> {
    public PrepRackRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    protected void extractFrame(PrepRackBlockEntity be, float tickDelta, PoseStack matrices, RenderFrame consumers,
                                int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof PrepRackBlock rack)) {
            return;
        }
        double lift = be.getLevel() == null ? 0.0 : TableDecor.liftOf(be.getLevel(), be.getBlockPos(), state);
        Direction facing = state.getValue(PrepRackBlock.FACING);
        PrepRackBlock.Mount mount = state.getValue(PrepRackBlock.MOUNT);

        matrices.pushPose();
        matrices.translate(0.5, lift, 0.5);
        matrices.rotate(Axis.YN.rotationDegrees(facing.toYRot()));

        int slots = rack.kind().slots();
        for (int i = 0; i < slots; i++) {
            ItemStack item = be.itemAt(i);
            if (item.isEmpty()) {
                continue;
            }
            renderSlot(rack.kind(), mount, i, item, be, matrices, consumers, light);
        }
        matrices.popPose();
    }

    private static void renderSlot(PrepRackBlock.Kind kind, PrepRackBlock.Mount mount, int slot,
                                   ItemStack stack, PrepRackBlockEntity be, PoseStack matrices,
                                   RenderFrame consumers, int light) {
        matrices.pushPose();
        switch (kind) {
            case MULTI_TIER -> {
                int tier = slot / 2;
                int col = slot % 2;
                double x = (col == 0 ? -3.2 : 3.2) / 16.0;
                double y = (switch (tier) {
                    case 0 -> 4.35;
                    case 1 -> 9.85;
                    default -> 15.35;
                }) / 16.0;
                double z = 1.2 / 16.0;
                matrices.translate(x, y, z);
                matrices.scale(0.30F, 0.30F, 0.30F);
                consumers.item(stack, ItemDisplayContext.FIXED, light,
                    OverlayTexture.NO_OVERLAY, matrices, be.getLevel(), slot);
            }
            case DESKTOP -> {
                int row = slot / 2;
                int col = slot % 2;
                double x = (col == 0 ? -3.0 : 3.0) / 16.0;
                double y = (row == 0 ? 3.8 : 8.5) / 16.0;
                double z = (row == 0 ? 2.8 : -2.1) / 16.0;
                matrices.translate(x, y, z);
                matrices.scale(0.28F, 0.28F, 0.28F);
                consumers.item(stack, ItemDisplayContext.FIXED, light,
                    OverlayTexture.NO_OVERLAY, matrices, be.getLevel(), slot);
            }
            case HANGING -> {
                double x = (-4.5 + slot * 3.0) / 16.0;
                boolean ceiling = mount == PrepRackBlock.Mount.CEILING;
                double y = (ceiling ? 6.4 : 7.8) / 16.0;
                double z = (ceiling ? 0.0 : -5.2) / 16.0;
                matrices.translate(x, y, z);
                matrices.rotate(Axis.ZP.rotationDegrees(180F));
                matrices.scale(0.30F, 0.30F, 0.30F);
                consumers.item(stack, ItemDisplayContext.FIXED, light,
                    OverlayTexture.NO_OVERLAY, matrices, be.getLevel(), slot);
            }
        }
        matrices.popPose();
    }
}
