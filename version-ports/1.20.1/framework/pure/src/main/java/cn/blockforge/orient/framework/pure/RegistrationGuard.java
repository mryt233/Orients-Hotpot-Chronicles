package cn.blockforge.orient.framework.pure;

/**
 * 注册 id 预检（EXP-REG 结论：Fabric 会静默覆盖、NeoForge 会崩启动，必须在注册前统一拦截）。
 * 作者：mryt233
 */
public final class RegistrationGuard {
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