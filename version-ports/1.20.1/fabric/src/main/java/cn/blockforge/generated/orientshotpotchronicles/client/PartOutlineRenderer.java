package cn.blockforge.generated.orientshotpotchronicles.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Fabric 的选中框事件接线，算法由两个平台共用。作者：mryt233 */
public final class PartOutlineRenderer {
    private PartOutlineRenderer() {
    }

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register((context, outline) ->
            !cn.blockforge.orient.framework.client.PartOutlineDrawing.render(outline.blockState(), outline.blockPos(),
                outline.cameraX(), outline.cameraY(), outline.cameraZ(),
                context.matrixStack(), outline.vertexConsumer()));
    }
}
