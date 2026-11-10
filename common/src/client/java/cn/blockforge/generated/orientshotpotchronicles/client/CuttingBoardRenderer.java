package cn.blockforge.generated.orientshotpotchronicles.client;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Pounding;
import net.minecraft.core.Direction;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.CuttingBoardBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.CuttingBoardBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

/**
 * 砧板上放着的东西（2026-10-23）：平躺在板面中间，半格大小，像原版物品展示框里的样子；
 * 捣丸子捣到一半时换成捶碎 / 肉浆的样子（用户：每一下都要有变化，而且要和原版食材放上去一样是立体的）。
 * 网页预览 viewer/animations.js 的 cutting_board 场景与本类对应。
 * 作者：mryt233 */
public class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity> {
    public CuttingBoardRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(CuttingBoardBlockEntity be, float tickDelta, PoseStack matrices, MultiBufferSource consumers,
                       int light, int overlay) {
        ItemStack stack = be.item();
        if (stack.isEmpty()) {
            return;
        }
        // mryt233 改（2026-11-11）：中间物品栈从当前世界配方读取，原有立体绘制保持。
        stack = Pounding.display(be.getLevel(), stack, be.cuts());
        matrices.pushPose();
        matrices.translate(0.5, CuttingBoardBlock.TOP + 0.02, 0.5);
        // 物品的「上方」朝远离放砧板的人那边，站在放的位置看是正的（2026-10-23 用户：板上的东西要朝着玩家）
        Direction away = be.itemFacing().getOpposite();   // 食物自己的朝向，和砧板分开
        matrices.mulPose(Axis.YP.rotationDegrees(-away.toYRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(90));
        matrices.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
            OverlayTexture.NO_OVERLAY, matrices, consumers, be.getLevel(), 0);
        matrices.popPose();
    }
}
