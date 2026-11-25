// 作者：mryt233
package cn.blockforge.orient.framework.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 读取物品渲染层的特殊渲染器和实际变换，供有界展示测量。 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("specialRenderer")
    SpecialModelRenderer<?> orient$specialRenderer();

    @Accessor("usesBlockLight")
    boolean orient$usesBlockLight();

    @Invoker("applyTransform")
    void orient$applyTransform(PoseStack.Pose pose);
}
