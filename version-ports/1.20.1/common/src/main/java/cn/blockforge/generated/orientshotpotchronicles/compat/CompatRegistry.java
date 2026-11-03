package cn.blockforge.generated.orientshotpotchronicles.compat;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.compat.modules.FarmersDelightCompat;
import cn.blockforge.generated.orientshotpotchronicles.compat.modules.FloraCompat;
import cn.blockforge.orient.framework.game.CompatModule;
import cn.blockforge.orient.framework.game.CompatRunner;

import java.util.List;

/**
 * 火锅兼容模块清单：调度与异常隔离由框架 {@link CompatRunner} 负责（P1 第四批）。
 * 加新兼容：在 {@code compat/modules/} 下写一个 {@link CompatModule}，然后往列表加一行。
 * 作者：mryt233 */
public final class CompatRegistry {
    private static final List<CompatModule> MODULES = List.of(
        new FarmersDelightCompat(),
        new FloraCompat()
    );

    private CompatRegistry() {
    }

    public static void init() {
        int loaded = CompatRunner.run(MODULES, HotPotCompat::isModLoaded,
            (name, error) -> HotPot.LOGGER.warn("[hotpot] 兼容模块 {} 执行失败，已跳过", name, error));
        if (loaded > 0) {
            HotPot.LOGGER.info("[hotpot] 已加载 {} 个兼容模块", loaded);
        }
    }
}