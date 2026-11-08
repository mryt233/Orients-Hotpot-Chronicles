package cn.blockforge.orient.framework.loader;

/**
 * 框架版本号的唯一来源（预发布；1.0.0 只在 P3 打包实验通过后发布）。作者：mryt233
 * mryt233 改（2026-11-01，前置检查 W4）：删掉没人用的 COMPAT_RANGE；加 {@link #RUNTIME}，
 * 打包脚本 tools/build_candidates.py 从这里读，不再各写一份。
 */
public final class ApiVersion {
    /** 框架 API 版本。 */
    public static final String CURRENT = "0.1.0-pre";
    /**
     * 前置 Runtime 版本：递增数字，不用 hotfix（用户 2026-11-01 定）。0.1.x 之内只加不删（向后兼容），
     * 破坏性变更进 0.2.0。内容包要求的范围由打包脚本按此算出：[本版本, 下一个次版本)。
     */
    public static final String RUNTIME = "0.1.3";

    private ApiVersion() {
    }
}
