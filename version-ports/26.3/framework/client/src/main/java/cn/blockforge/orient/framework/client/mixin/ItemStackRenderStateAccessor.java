// 作者：mryt233
package cn.blockforge.orient.framework.client.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 读取物品渲染层状态以便原地重映射烘焙四边形。作者：mryt233 */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int orient$activeLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] orient$layers();

    // mryt233 改：26.3 渲染层改为不可变 ItemQuads，读取后由平台重新分类。
    @Mixin(ItemStackRenderState.LayerRenderState.class)
    interface LayerAccessor {
        @Accessor("quads")
        net.minecraft.client.resources.model.geometry.ItemQuads orient$quads();
    }
}
