// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.platform.neoforge.NeoForgeFrameworkPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

/** 开发运行没有独立前置入口时安装同一平台；正式包依赖独立前置。 */
public final class NeoForgePlatform {
    private NeoForgePlatform() { }
    public static void install(IEventBus modBus) {
        if (Framework.isInstalled()) return;
        if (FMLEnvironment.production) {
            throw new IllegalStateException("orient_framework runtime is required in production");
        }
        NeoForgeFrameworkPlatform.install(modBus);
    }
    /** NeoForge 只给建好的鱼池，专属访问器适配保留。 */
    public static void afterContentRegistration() { NeoForgeLootListeners.install(); }
}
