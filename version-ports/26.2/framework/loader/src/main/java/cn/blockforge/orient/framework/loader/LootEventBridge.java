package cn.blockforge.orient.framework.loader;

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