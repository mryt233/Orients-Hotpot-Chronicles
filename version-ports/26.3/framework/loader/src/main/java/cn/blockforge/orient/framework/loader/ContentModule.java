package cn.blockforge.orient.framework.loader;

/**
 * 内容模块生命周期（能力模型）：
 * 声明 → 注册（各注册表窗口，由平台负责）→ 运行时 → 客户端 → 数据/资源重载（可重复）。
 * 作者：mryt233
 */
public interface ContentModule {
    ModuleDeclaration declaration();

    /** 注册阶段：内容只调用分域 Registrar；平台按注册表窗口落地。 */
    void register(RegistrationApi registration);

    default void runtime(RuntimeHooks hooks) {
    }

    default void client(ClientHooks hooks) {
    }

    /** 数据/资源重载；可被任意次调用，处理器不得保留一次性状态。 */
    default void onReload() {
    }
}