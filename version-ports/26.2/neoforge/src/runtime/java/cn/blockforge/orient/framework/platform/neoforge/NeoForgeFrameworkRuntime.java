package cn.blockforge.orient.framework.platform.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge 框架 Runtime 入口（mod id orient_framework）：只安装平台一次，
 * 负责注册窗口与网络窗口的统一落地。内容模组通过依赖顺序后加载。作者：mryt233
 *
 * <p>本文件单独放在 {@code neoforge/src/runtime/java}，不参与开发环境的 mod 扫描
 * （否则 FML 会把 @Mod("orient_framework") 当成 orients_hotpot_chronicles 的悬空入口点），
 * 只由 {@code tools/build_candidates.py} 编译进候选 Runtime JAR。</p>
 */
@Mod("orient_framework")
public final class NeoForgeFrameworkRuntime {
    public NeoForgeFrameworkRuntime(IEventBus modBus, ModContainer container) {
        NeoForgeFrameworkPlatform.install(modBus);
    }
}
