package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;

/**
 * 物品上带着菜时的样子（手里、物品栏、掉在地上都一样）：
 * <ul>
 *   <li>筷子夹着菜：先画原来的筷子，再在筷子尖（贴图左下角）画一小块夹着的菜。</li>
 *   <li>装了菜的盘子：先画原来的盘子，再按 {@link PlateFoodRenderer} 把菜摆在盘子上。</li>
 * </ul>
 * 有菜的物品带自定义模型数据 1（PlateItem.syncFoodModel），模型是 builtin/entity、不带显示变换，交给这里画；
 * 这里去掉自定义模型数据再画一遍就是原来的模型（不会递归），菜用原来模型的显示变换对齐。
 * 作者：
 */
public final class HeldFoodItemRenderer {
    /** 筷子尖在物品模型里的位置（0..1，贴图左下角那两根筷子的尖）和夹着的菜的大小。 */
    private static final float TIP_X = 0.24F;
    private static final float TIP_Y = 0.2F;
    private static final float HELD_SCALE = 0.42F;

    private HeldFoodItemRenderer() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        NeoItemRenderers.register(event, HotPotContent.CHOPSTICKS, HeldFoodItemRenderer::renderChopsticks);
        for (PlateItem item : HotPotContent.PLATE_ITEMS) {
            NeoItemRenderers.register(event, item, HeldFoodItemRenderer::renderPlate);
        }
        // 装着菜的原版碗（r99）：资源包里给 minecraft:item/bowl 加了自定义模型数据 1 → builtin/entity
        NeoItemRenderers.register(event, net.minecraft.item.Items.BOWL, HeldFoodItemRenderer::renderBowl);
    }

    /** 去掉自定义模型数据的复制品 = 原来的模型。 */
    private static ItemStack plain(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
        return copy;
    }

    /** 先按原来的模型画一遍，再把矩阵摆到原来模型的显示变换下（物品坐标 0..1），交给 overlayDraw 画菜。 */
    private static void withBase(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                 VertexConsumerProvider consumers, int light, int overlay, Runnable overlayDraw) {
        ItemRenderer items = MinecraftClient.getInstance().getItemRenderer();
        ItemStack base = plain(stack);
        // builtin 渲染前原版已经平移了 -0.5，这里挪回去，让原来的模型按自己的显示变换画
        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        items.renderItem(base, mode, light, overlay, matrices, consumers, null, 0);
        BakedModel model = items.getModel(base, null, null, 0);
        model.getTransformation().getTransformation(mode).apply(false, matrices);
        matrices.translate(-0.5F, -0.5F, -0.5F);
        overlayDraw.run();
        matrices.pop();
    }

    /** 拿在手上时夹着的菜放在筷子尖（筷子转了 180°，尖在贴图右上角），大小同原版手里拿这个食物。 */
    private static final float HAND_TIP_X = 0.74F;
    private static final float HAND_TIP_Y = 0.78F;

    private static void renderChopsticks(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                         VertexConsumerProvider consumers, int light, int overlay) {
        ItemStack food = Chopsticks.held(stack);
        boolean inHand = mode.isFirstPerson() || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
            || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        if (inHand && !food.isEmpty()) {
            // 手里拿着、夹着菜：贴图左下角是握在手里的那头，筷子尖在那儿菜会被手挡住（r92 用户截图）。
            // 所以筷子转 180°（手握筷子头、尖朝前上方），菜按原版手持大小画在筷子尖上。
            ItemRenderer items = MinecraftClient.getInstance().getItemRenderer();
            ItemStack base = plain(stack);
            matrices.push();
            matrices.translate(0.5F, 0.5F, 0.5F);
            items.getModel(base, null, null, 0).getTransformation().getTransformation(mode).apply(false, matrices);
            matrices.push();
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180F));
            items.renderItem(base, ModelTransformationMode.NONE, light, overlay, matrices, consumers, null, 0);
            matrices.pop();
            matrices.translate(HAND_TIP_X - 0.5F, HAND_TIP_Y - 0.5F, 0.03F);
            items.renderItem(food, ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, consumers, null, 0);
            matrices.pop();
            return;
        }
        withBase(stack, mode, matrices, consumers, light, overlay, () -> {
            if (food.isEmpty()) {
                return;
            }
            matrices.push();
            matrices.translate(TIP_X, TIP_Y, 0.5F + 0.04F);
            matrices.scale(HELD_SCALE, HELD_SCALE, HELD_SCALE);
            MinecraftClient.getInstance().getItemRenderer().renderItem(food, ModelTransformationMode.NONE,
                light, OverlayTexture.DEFAULT_UV, matrices, consumers, null, 0);
            matrices.pop();
        });
    }

    /** 装着菜的碗：画立体的碗（和摆在桌上的同一个模型），再把菜叠在碗里；显示变换在 item/bowl_with_food.json。 */
    private static void renderBowl(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                   VertexConsumerProvider consumers, int light, int overlay) {
        // 调整：无装菜组件时回到原版碗；清除模型覆盖以防递归。
        if (!stack.contains(DataComponentTypes.CONTAINER)
            || stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).streamNonEmpty().findAny().isEmpty()) {
            withBase(stack, mode, matrices, consumers, light, overlay, () -> {});
            return;
        }
        DefaultedList<ItemStack> foods = DefaultedList.ofSize(
            cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize.BOWL.capacity(), ItemStack.EMPTY);
        stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(foods);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
            HotPotContent.BOWL_BLOCK.getDefaultState(), matrices, consumers, light, overlay);
        PlateFoodRenderer.render(cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateSize.BOWL,
            Direction.Axis.X, foods, matrices, consumers, light, null, 0, MinecraftClient.getInstance().getItemRenderer());
    }

    private static void renderPlate(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                    VertexConsumerProvider consumers, int light, int overlay) {
        if (!(stack.getItem() instanceof PlateItem item)) {
            return;
        }
        DefaultedList<ItemStack> foods = DefaultedList.ofSize(item.plate().size().capacity(), ItemStack.EMPTY);
        stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(foods);
        withBase(stack, mode, matrices, consumers, light, overlay, () ->
            PlateFoodRenderer.render(item.plate().size(), Direction.Axis.X, foods, matrices, consumers, light,
                null, 0, MinecraftClient.getInstance().getItemRenderer()));
    }
}
