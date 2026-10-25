// 作者：；1.20.1 Forge 客户端动态物品渲染。
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 所有自定义物品与原版碗共用一个延迟创建的 BEWLR，构造物品时不访问 Minecraft。 */
public final class ForgeItemRenderers {
    private static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        private BuiltinModelItemRenderer renderer;

        @Override
        public BuiltinModelItemRenderer getCustomRenderer() {
            if (renderer == null) {
                MinecraftClient client = MinecraftClient.getInstance();
                renderer = new BuiltinModelItemRenderer(client.getBlockEntityRenderDispatcher(), client.getEntityModelLoader()) {
                    @Override
                    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                       VertexConsumerProvider consumers, int light, int overlay) {
                        if (stack.isOf(Items.BOWL)) {
                            HeldFoodItemRenderer.renderBowl(stack, mode, matrices, consumers, light, overlay);
                        } else if (stack.isOf(HotPotContent.DIPPING_SAUCE)) {
                            client.getBlockRenderManager().renderBlockAsEntity(
                                HotPotContent.BOWL_BLOCK.getDefaultState(), matrices, consumers, light, overlay);
                            SauceRender.surface(cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem.contents(stack).mask(), matrices, consumers, light);
                        } else if (stack.isOf(HotPotContent.CHOPSTICKS)) {
                            HeldFoodItemRenderer.renderChopsticks(stack, mode, matrices, consumers, light, overlay);
                        } else if (stack.getItem() instanceof BlockItem potItem
                            && potItem.getBlock() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock pot
                            && pot.material().layout().multiSoup()) {
                            client.getBlockRenderManager().renderBlockAsEntity(
                                pot.getDefaultState(), matrices, consumers, light, overlay);
                            var data = cn.blockforge.generated.orientshotpotchronicles.hotpot.ItemData.potData(stack);
                            if (data != null) HotPotBlockEntityRenderer.renderItemSoups(
                                pot.material(), data, matrices, consumers, light);
                        } else if (stack.getItem() instanceof PlateItem) {
                            HeldFoodItemRenderer.renderPlate(stack, mode, matrices, consumers, light, overlay);
                        } else if (stack.getItem() instanceof BlockItem item && HotPotContent.TALL_BENCH_ITEMS.contains(item)) {
                            var state = item.getBlock().getDefaultState();
                            client.getBlockRenderManager().renderBlockAsEntity(state, matrices, consumers, light, overlay);
                            BenchLegsRenderer.renderLegs(state, matrices, consumers, light);
                        }
                    }
                };
            }
            return renderer;
        }
    };

    private ForgeItemRenderers() {}

    public static IClientItemExtensions extension() { return EXTENSION; }
}
