package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.client.resources.SplashManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 主菜单标题旁的黄色闪烁标语：原版从 splashes.txt 读到的那些照旧保留，这里在原版 apply 读完（RETURN）之后，
 * 把本模组自己的标语补进同一份列表，和原版标语一起参加随机抽取；圣诞 / 新年 / 生日这类特殊日期标语由
 * getSplash 单独判断，先于随机列表返回，所以不受影响。
 * 以后加新标语直接往 {@link #HOTPOT_SPLASHES} 里添（mryt233，2026-11-05）。
 * 作者：mryt233 */
@Mixin(SplashManager.class)
abstract class SplashManagerMixin {
    /** 本模组的主菜单标语；先放一条，以后往这个列表里添即可。 */
    @Unique
    private static final List<String> HOTPOT_SPLASHES = List.of("火锅现已加入 Minecraft");

    @Shadow
    @Final
    private List<String> splashes;

    @Inject(method = "apply(Ljava/util/List;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("RETURN"))
    private void orientsHotpot$addHotpotSplashes(List<String> list, ResourceManager resourceManager,
                                                 ProfilerFiller profiler, CallbackInfo ci) {
        // 原版 apply 是「先 clear 再 addAll(list)」，而 splashes 在构造时用 Lists.newArrayList() 建立，本身可写；
        // 资源包重载（F3+T）会再走一遍 apply，所以先查重，免得同一条标语叠出好几份。
        for (String splash : HOTPOT_SPLASHES) {
            if (!this.splashes.contains(splash)) {
                this.splashes.add(splash);
            }
        }
    }
}
