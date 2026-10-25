// 作者：；1.21.1 NeoForge 物品渲染扩展桥接。
package cn.blockforge.generated.orientshotpotchronicles.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** 将原版渲染回调交给 NeoForge，支持原版 Items.BOWL。 */
final class NeoItemRenderers {
    @FunctionalInterface
    interface Renderer {
        void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                    VertexConsumerProvider consumers, int light, int overlay);
    }

    private NeoItemRenderers() {}

    static void register(RegisterClientExtensionsEvent event, Item item, Renderer renderer) {
        event.registerItem(new IClientItemExtensions() {
            private BuiltinModelItemRenderer instance;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (instance == null) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    instance = new BuiltinModelItemRenderer(client.getBlockEntityRenderDispatcher(), client.getEntityModelLoader()) {
                        @Override
                        public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                           VertexConsumerProvider consumers, int light, int overlay) {
                            renderer.render(stack, mode, matrices, consumers, light, overlay);
                        }
                    };
                }
                return instance;
            }
        }, item);
    }
}
