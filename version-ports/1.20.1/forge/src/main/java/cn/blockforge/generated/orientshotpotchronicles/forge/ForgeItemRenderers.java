package cn.blockforge.generated.orientshotpotchronicles.forge;

import cn.blockforge.orient.framework.client.ClientItemRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.IdentityHashMap;
import java.util.Map;

/** 包括原版碗在内的物品绘制扩展。作者：mryt233 */
public final class ForgeItemRenderers {
    private static final Map<Item, IClientItemExtensions> EXTENSIONS = new IdentityHashMap<>();
    private ForgeItemRenderers() { }
    public static IClientItemExtensions extension(Item item) { return EXTENSIONS.get(item); }
    public static void register(Item item, ClientItemRenderers.Renderer renderer) {
        EXTENSIONS.put(item, new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer custom;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (custom == null) {
                    Minecraft mc = Minecraft.getInstance();
                    custom = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,
                                net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack pose,
                                net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
                            renderer.render(stack, context, pose, buffers, light, overlay);
                        }
                    };
                }
                return custom;
            }
        });
    }
}
