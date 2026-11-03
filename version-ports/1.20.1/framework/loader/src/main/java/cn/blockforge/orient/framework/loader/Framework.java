package cn.blockforge.orient.framework.loader;

/** 框架入口：平台在初始化最早期注入且只注入一次。作者：mryt233 */
public final class Framework {
    private static final LootEventBridge LOOT_EVENTS = new LootEventBridge();
    private static volatile Platform platform;

    private Framework() {
    }

    /** 规范战利品桥实例：内容登记与平台消费都必须用它。 */
    public static LootEventBridge lootEvents() {
        return LOOT_EVENTS;
    }

    public static void install(Platform implementation) {
        if (platform != null) {
            throw new IllegalStateException("Platform already installed");
        }
        platform = implementation;
        RuntimeStats.get().platformInstalled(implementation.getClass().getSimpleName());
    }

    public static Platform platform() {
        Platform current = platform;
        if (current == null) {
            throw new IllegalStateException("Platform not installed");
        }
        return current;
    }

    /** 开发期查询，不抛异常。 */
    public static boolean isInstalled() {
        return platform != null;
    }
}