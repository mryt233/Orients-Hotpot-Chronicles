package cn.blockforge.orient.framework.loader;

import java.nio.file.Path;

/** 平台能力契约：只描述「怎么接入 Minecraft」，不含任何内容业务。作者：mryt233 */
public interface Platform {
    RegistrationApi registration();

    NetworkApi network();

    MenuOpener menus();

    /** 必须返回 {@link Framework#lootEvents()} 的同一实例。 */
    LootEventBridge lootEvents();

    Path configDir();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();
}