// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;

/** 部件描边在提取阶段替换形状，其余状态交给原版管线。 */
public final class PartOutlineRenderer {
    private PartOutlineRenderer() { }
    public static void register() {
        LevelRenderEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register((context, hit) -> {
            var old = context.levelState().blockOutlineRenderState;
            if (old == null) { return; }
            var shape = cn.blockforge.orient.framework.client.PartOutlineDrawing.extract(
                context.level().getBlockState(old.pos()), old.pos(), context.level(), Minecraft.getInstance().player);
            if (shape != null) {
                context.levelState().blockOutlineRenderState = new BlockOutlineRenderState(old.pos(),
                    old.isTranslucent(), old.highContrast(), shape, old.collisionShape(), old.occlusionShape(), old.interactionShape());
            }
        });
    }
}
