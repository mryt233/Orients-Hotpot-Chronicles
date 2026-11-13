// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

/** 开发运行没有独立前置入口时安装同一平台；正式包依赖独立前置。 */
public final class NeoForgePlatform {
    private NeoForgePlatform() {
    }

    public static void install(IEventBus modBus) {
        if (Framework.isInstalled()) return;
        if (FMLEnvironment.isProduction()) {
            throw new IllegalStateException("orient_framework Runtime must be installed in production");
        }
        try {
            Platform platform = (Platform) Class.forName(
                "cn.blockforge.orient.framework.platform.neoforge.NeoForgeFrameworkPlatform")
                .getMethod("install", IEventBus.class).invoke(null, modBus);
            if (!Framework.isInstalled()) Framework.install(platform);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to install development framework platform", e);
        }
    }

    /** NeoForge 原生事件只给已建好的鱼池，专属访问器适配保留。 */
    public static void afterContentRegistration() {
        NeoForgeLootListeners.install();
    }
}
