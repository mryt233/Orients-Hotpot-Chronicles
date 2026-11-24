// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.BiFunction;

/** 暴露给模组 Fabric 兼容性回退 Mixin 的受控桥梁。作者：mryt233 */
public final class FabricLegacyModelBridge {
    private FabricLegacyModelBridge() { }

    public static void wrap(Map<ResourceLocation, BakedModel> bakedModels,
                            BiFunction<ResourceLocation, Material, TextureAtlasSprite> spriteGetter) {
        FabricClientContent.applyLegacyModelWraps(bakedModels, spriteGetter);
    }
}
