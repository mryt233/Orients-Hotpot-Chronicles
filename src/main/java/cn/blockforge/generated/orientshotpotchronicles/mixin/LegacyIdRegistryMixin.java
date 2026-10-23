package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.LegacyIds;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 旧存档兼容（2026-10-23，）：方块 / 物品注册表按 ID 查东西时，本模组的旧 ID 换成新 ID（对照见 LegacyIds）。
 * 读档（区块里的方块、物品栏 / 箱子里的物品）都是走这几个查询，所以旧存档的灶台、盘子、凳子等不会再消失。
 */
@Mixin(SimpleRegistry.class)
public abstract class LegacyIdRegistryMixin {
    @ModifyVariable(method = "get(Lnet/minecraft/util/Identifier;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private Identifier hotpot$legacyGet(Identifier id) {
        return LegacyIds.remap((Registry<?>) this, id);
    }

    @ModifyVariable(method = "getEntry(Lnet/minecraft/util/Identifier;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
    private Identifier hotpot$legacyGetEntry(Identifier id) {
        return LegacyIds.remap((Registry<?>) this, id);
    }

    @ModifyVariable(method = "containsId", at = @At("HEAD"), argsOnly = true)
    private Identifier hotpot$legacyContains(Identifier id) {
        return LegacyIds.remap((Registry<?>) this, id);
    }

    @ModifyVariable(method = "get(Lnet/minecraft/registry/RegistryKey;)Ljava/lang/Object;", at = @At("HEAD"), argsOnly = true)
    private RegistryKey<?> hotpot$legacyGetKey(RegistryKey<?> key) {
        return LegacyIds.remap((Registry<?>) this, key);
    }

    @ModifyVariable(method = "getEntry(Lnet/minecraft/registry/RegistryKey;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
    private RegistryKey<?> hotpot$legacyGetEntryKey(RegistryKey<?> key) {
        return LegacyIds.remap((Registry<?>) this, key);
    }
}
