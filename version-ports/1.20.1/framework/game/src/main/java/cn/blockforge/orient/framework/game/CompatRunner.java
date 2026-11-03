package cn.blockforge.orient.framework.game;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 兼容模块调度：模组不在场就跳过；单模块异常只跳过它自己，不影响本体。
 * 业务模块列表与加载器检测都由内容/平台注入。作者：mryt233
 */
public final class CompatRunner {
    private CompatRunner() {
    }

    public static int run(List<CompatModule> modules, Predicate<String> isModLoaded,
                          BiConsumer<String, Throwable> warn) {
        int loaded = 0;
        for (CompatModule module : modules) {
            String hit = module.modIds().stream().filter(isModLoaded).findFirst().orElse(null);
            if (hit == null) {
                continue;
            }
            try {
                module.register();
                loaded++;
            } catch (Throwable error) {
                warn.accept(module.name() + "（" + hit + "）", error);
            }
        }
        return loaded;
    }
}