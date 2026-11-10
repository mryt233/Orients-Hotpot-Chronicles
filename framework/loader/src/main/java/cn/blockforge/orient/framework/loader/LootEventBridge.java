package cn.blockforge.orient.framework.loader;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通用战利品修改桥（实例能力；内容登记与平台消费必须用同一个实例）。
 * 规范实例由 {@link Framework#lootEvents()} 提供；平台实现的 {@link Platform#lootEvents()} 必须返回它。
 * 作者：mryt233
 */
public final class LootEventBridge {
    @FunctionalInterface
    public interface Modification {
        void apply(ResourceKey<LootTable> key, LootTable.Builder builder, boolean builtin,
                   HolderLookup.Provider registries);
    }

    private volatile boolean platformManaged;

    /** 平台安装加载器事件后标记；旧内容侧接线据此跳过通用消费段。 */
    public boolean isPlatformManaged() {
        return platformManaged;
    }

    public void markPlatformManaged() {
        platformManaged = true;
    }

    private final List<Modification> modifications = new CopyOnWriteArrayList<>();

    public LootEventBridge() {
    }

    public void register(Modification modification) {
        modifications.add(modification);
    }

    /** 平台侧遍历用。 */
    public List<Modification> all() {
        return modifications;
    }
}