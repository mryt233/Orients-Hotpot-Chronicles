package cn.blockforge.orient.framework.platform.fabric;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric 框架 Runtime 入口（mod id orient_framework）：只安装平台一次。
 * 内容模组通过依赖顺序后加载，只使用 Framework API。作者：mryt233
 */
public final class FabricFrameworkRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricFrameworkPlatform.install();
    }
}
