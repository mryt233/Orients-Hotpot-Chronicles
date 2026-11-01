package exp.runtime;

import cn.blockforge.orient.framework.loader.*;
import java.nio.file.Path;

/** EXP-PKG 的最小平台实现：验证初始化一次与共享实例。 */
public final class ExpRuntime implements Platform {
    private static int inits;

    public static void init(String loader) {
        inits++;
        if (inits > 1) {
            throw new IllegalStateException("framework initialized twice: " + inits);
        }
        Framework.install(new ExpRuntime());
        System.out.println("[EXP-PKG] framework-init loader=" + loader + " count=" + inits
            + " installed=" + Framework.isInstalled()
            + " loot=" + Integer.toHexString(System.identityHashCode(Framework.lootEvents())));
    }

    @Override public RegistrationApi registration() { throw new UnsupportedOperationException("exp"); }
    @Override public NetworkApi network() { throw new UnsupportedOperationException("exp"); }
    @Override public MenuOpener menus() { return (player, menu, pos) -> { }; }
    @Override public LootEventBridge lootEvents() { return Framework.lootEvents(); }
    @Override public Path configDir() { return Path.of("."); }
    @Override public boolean isModLoaded(String modId) { return false; }
    @Override public boolean isDevelopmentEnvironment() { return true; }
}
