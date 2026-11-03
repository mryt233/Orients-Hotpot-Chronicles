package cn.blockforge.orient.framework.game;

import java.util.List;

/** 第三方兼容模块契约（具体模块与业务留在内容层）。作者：mryt233 */
public interface CompatModule {
    /** 默认用第一个 mod id 作为显示名。 */
    default String name() {
        return modIds().isEmpty() ? "compat" : modIds().get(0);
    }

    List<String> modIds();

    void register();
}