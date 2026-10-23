package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.LegacyIds;
import net.minecraft.registry.Registry;
import net.minecraft.registry.SimpleDefaultedRegistry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 同 LegacyIdRegistryMixin：方块 / 物品注册表是「带默认值」的那种，它自己重写了按 ID 查的方法，这里也换一下。 */
@Mixin(SimpleDefaultedRegistry.class)
public abstract class LegacyIdDefaultedRegistryMixin {
    @ModifyVariable(method = "get(Lnet/minecraft/util/Identifier;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private Identifier hotpot$legacyGet(Identifier id) {
        return LegacyIds.remap((Registry<?>) this, id);
    }

    @ModifyVariable(method = "getOrEmpty", at = @At("HEAD"), argsOnly = true)
    private Identifier hotpot$legacyGetOrEmpty(Identifier id) {
        return LegacyIds.remap((Registry<?>) this, id);
    }
}
