package cn.blockforge.orient.framework.loader;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import java.nio.file.Path;

/** 平台能力契约：只描述「怎么接入 Minecraft」，不含任何内容业务。作者：mryt233 */
public interface Platform {
    RegistrationApi registration();

    default RuntimeEvents events() {
        throw new UnsupportedOperationException("runtime events require Runtime 0.1.5");
    }

    NetworkApi network();

    MenuOpener menus();

    /** 必须返回 {@link Framework#lootEvents()} 的同一实例。 */
    LootEventBridge lootEvents();

    Path configDir();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** mryt233 改（2026-11-10）：区分专用服与客户端进程，供初始化配置使用。 */
    default boolean isDedicatedServer() { return false; }
}