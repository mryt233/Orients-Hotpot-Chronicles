package cn.blockforge.orient.framework.pure;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

/**
 * 注册 id 预检（EXP-REG 结论：Fabric 会静默覆盖、NeoForge 会崩启动，必须在注册前统一拦截）。
 * 作者：mryt233
 */
public final class RegistrationGuard {
    private static final ThreadLocal<String> OWNER = new ThreadLocal<>();

    public static String owner(String fallback) {
        String owner = OWNER.get();
        return owner == null ? fallback : owner;
    }

    public static void withOwner(String owner, Runnable action) {
        String previous = OWNER.get();
        OWNER.set(java.util.Objects.requireNonNull(owner));
        try {
            action.run();
        } finally {
            if (previous == null) OWNER.remove();
            else OWNER.set(previous);
        }
    }

    public static IllegalStateException failure(String registry, String id, String owner,
                                                 String phase, Throwable cause) {
        return new IllegalStateException("registration failed: registry=" + registry + ", id=" + id
            + ", owner=" + owner + ", phase=" + phase
            + ("closed".equals(phase) ? " (registration window already closed)" : ""), cause);
    }

    private RegistrationGuard() {
    }

    /** 目标 id 已存在时立即抛出带归属信息的异常；核心注册失败必须终止加载。 */
    public static void requireAbsent(boolean present, String registryKey, String id, String ownerModule) {
        if (present) {
            throw new IllegalStateException("duplicate registration: registry=" + registryKey
                + ", id=" + id + ", owner=" + ownerModule);
        }
    }
}