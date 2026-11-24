// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.fabric.mixin;

import cn.blockforge.orient.framework.platform.fabric.FabricLegacyModelBridge;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * 仅在未安装 {@code fabric-model-loading-api-v1}（例如 {@code fabric-api 0.83.0}）时，
 * 于模型烘焙收尾阶段接入换材质包装；安装新版本时直接让路，零开销。作者：mryt233
 */
@Mixin(ModelBakery.class)
public abstract class FabricLegacyModelBakeryMixin {
    @Shadow
    @Final
    private Map<ResourceLocation, BakedModel> bakedTopLevelModels;

    @Inject(method = "bakeModels", at = @At("RETURN"))
    private void orientsHotpot$wrapLegacyBakedModels(
        BiFunction<ResourceLocation, Material, TextureAtlasSprite> spriteGetter, CallbackInfo ci
    ) {
        FabricLegacyModelBridge.wrap(this.bakedTopLevelModels, spriteGetter);
    }
}
