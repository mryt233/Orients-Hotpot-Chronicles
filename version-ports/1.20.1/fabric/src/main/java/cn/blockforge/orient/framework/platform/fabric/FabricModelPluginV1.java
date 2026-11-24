// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.client.SwappableModelRule;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Function;

/**
 * 隔离引用 {@code fabric-model-loading-api-v1}（0.84.0+），确保最低地板 {@code fabric-api 0.83.0} 下不触发类加载失败。作者：mryt233
 */
final class FabricModelPluginV1 {
    private FabricModelPluginV1() { }

    @FunctionalInterface
    interface WrapperFactory {
        BakedModel wrap(BakedModel model, SwappableModelRule rule, Function<ResourceLocation, TextureAtlasSprite> atlas);
    }

    static void register(SwappableModelRule rule, WrapperFactory factory) {
        ModelLoadingPlugin.register(pluginCtx -> pluginCtx.modifyModelAfterBake().register((model, context) -> {
            ResourceLocation rawId = context.id();
            if (model == null || rawId == null) {
                return model;
            }
            // 1.20.1 中 ModelResourceLocation 继承 ResourceLocation 且覆盖 equals/hashCode 包含 variant，须先提取基础 ID。
            ResourceLocation baseId = rawId instanceof ModelResourceLocation
                ? ResourceLocation.tryBuild(rawId.getNamespace(), rawId.getPath())
                : rawId;
            if (!rule.matches(baseId)) {
                return model;
            }
            return factory.wrap(model, rule, atlasLookup(context));
        }));
    }

    /** 烘培回调的 context 会被复用于后续模型，提前取出 textureGetter 防止后续覆盖。 */
    static Function<ResourceLocation, TextureAtlasSprite> atlasLookup(ModelModifier.AfterBake.Context context) {
        Function<Material, TextureAtlasSprite> textureGetter = Objects.requireNonNull(
            Objects.requireNonNull(context, "context").textureGetter(), "textureGetter");
        return tex -> textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
    }
}
