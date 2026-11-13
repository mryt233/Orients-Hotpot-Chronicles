// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.Platform;
import net.fabricmc.loader.api.FabricLoader;

/** 开发运行没有独立前置入口时安装同一平台；正式包依赖独立前置。 */
public final class FabricPlatform {
    private FabricPlatform() {
    }

    public static void install() {
        if (Framework.isInstalled()) return;
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            throw new IllegalStateException("orient_framework Runtime must be installed in production");
        }
        try {
            Platform platform = (Platform) Class.forName(
                "cn.blockforge.orient.framework.platform.fabric.FabricFrameworkPlatform")
                .getMethod("install").invoke(null);
            if (!Framework.isInstalled()) Framework.install(platform);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to install development framework platform", e);
        }
    }

    /** Fabric 的原生 modifyPools 与另一端不同，专属适配保留。 */
    public static void afterContentRegistration() {
        ShrimpLoot.register();
    }
}
