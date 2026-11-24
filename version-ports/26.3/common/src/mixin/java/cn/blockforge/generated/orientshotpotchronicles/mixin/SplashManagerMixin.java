// 作者：mryt233
// mryt233 改（2026-11-09）：26.2 原版 SplashManager 的 splashes 已改成「private List<Component> splashes = List.of();」，
// apply 里写的是 this.splashes = List.copyOf(preparations)，拿到的是不可变列表，旧写法直接 add 会崩；
// 这里对齐 26.1 的修法：RETURN 后换成可写副本，按文本查重，补一条黄色 Component。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 主菜单标题旁的黄色闪烁标语：原版从 splashes.txt 读到的那些照旧保留，这里在原版 apply 读完（RETURN）之后，
 * 把本模组自己的标语补进同一份列表，和原版标语一起参加随机抽取；圣诞 / 新年 / 生日这类特殊日期标语由
 * getSplash 单独判断，先于随机列表返回，所以不受影响。
 * 以后加新标语直接往 {@link #HOTPOT_SPLASHES} 里添（mryt233，2026-11-05）。
 * 作者：mryt233 */
@Mixin(SplashManager.class)
abstract class SplashManagerMixin {
    /** 原版标语用的黄色（和 SplashManager.DEFAULT_STYLE 一致：0xFFFFFF00 取低 8 位为 -256）。 */
    @Unique
    private static final int HOTPOT_SPLASH_COLOR = -256;

    /** 本模组的主菜单标语；先放一条，以后往这个列表里添即可。 */
    @Unique
    private static final List<String> HOTPOT_SPLASHES = List.of("火锅现已加入 Minecraft");

    @Shadow
    private List<Component> splashes;

    @Inject(method = "apply(Ljava/util/List;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("RETURN"))
    private void orientsHotpot$addHotpotSplashes(List<Component> list, ResourceManager resourceManager,
                                                 ProfilerFiller profiler, CallbackInfo ci) {
        // mryt233 改（2026-11-09）：原版 apply 是「this.splashes = List.copyOf(list)」，拿到的是不可变列表，
        // 直接 add 会抛 UnsupportedOperationException，所以在 RETURN 后换成一份可写副本再补本模组标语；
        // 资源包重载（F3+T）会再走一遍 apply，所以按文本查重，免得同一条标语叠出好几份。
        List<Component> merged = new ArrayList<>(this.splashes);
        Set<String> texts = new HashSet<>();
        for (Component splash : merged) {
            texts.add(splash.getString());
        }
        for (String splash : HOTPOT_SPLASHES) {
            if (texts.add(splash)) {
                merged.add(Component.literal(splash).setStyle(Style.EMPTY.withColor(HOTPOT_SPLASH_COLOR)));
            }
        }
        this.splashes = merged;
    }
}