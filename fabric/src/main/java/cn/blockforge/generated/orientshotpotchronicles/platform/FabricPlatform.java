// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.platform.fabric.FabricFrameworkPlatform;
import net.fabricmc.loader.api.FabricLoader;

/** 开发运行没有独立前置入口时安装同一平台；正式包依赖独立前置。 */
public final class FabricPlatform {
    private FabricPlatform() { }
    public static void install() {
        if (Framework.isInstalled()) return;
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            throw new IllegalStateException("orient_framework runtime is required in production");
        }
        FabricFrameworkPlatform.install();
    }
    /** Fabric 的原生 modifyPools 与另一端不同，专属适配保留。 */
    public static void afterContentRegistration() { ShrimpLoot.register(); }
}
