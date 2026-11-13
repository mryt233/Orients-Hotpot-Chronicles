// 作者：mryt233
package cn.blockforge.orient.framework.platform.forge;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** 框架自己的战利品消费端；Forge 47.0.0 没有 LootTable.addPool，自动回退写 pools 列表。 */
final class ForgeLootEvents {
    private static final Method ADD_POOL = findAddPool();
    private static final Field POOLS_FIELD = ADD_POOL == null ? findPoolsField() : null;

    static void install() {
        MinecraftForge.EVENT_BUS.addListener(ForgeLootEvents::onLootTableLoad);
        Framework.lootEvents().markPlatformManaged();
    }

    private static Method findAddPool() {
        try {
            return LootTable.class.getMethod("addPool", LootPool.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Field findPoolsField() {
        // mryt233 改（2026-11-13）：Forge 47.0.0 正式运行期未提供 LootTable#addPool，需同时兼容 SRG 字段名 f_79109_
        for (String name : new String[]{"f_79109_", "pools"}) {
            try {
                Field field = LootTable.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("LootTable#pools not found");
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        var modifications = Framework.lootEvents().all();
        if (modifications.isEmpty()) return;
        boolean builtin = "minecraft".equals(event.getName().getNamespace());
        PoolCollector collector = new PoolCollector();
        for (var modification : modifications) {
            modification.apply(event.getName(), collector, builtin);
        }
        if (collector.pools.isEmpty()) return;
        LootTable table = event.getTable();
        for (LootPool.Builder pool : collector.pools) {
            appendPool(table, pool.build());
        }
    }

    @SuppressWarnings("unchecked")
    private static void appendPool(LootTable table, LootPool pool) {
        try {
            if (ADD_POOL != null) {
                ADD_POOL.invoke(table, pool);
            } else {
                ((List<LootPool>) POOLS_FIELD.get(table)).add(pool);
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Failed to append loot pool", error);
        }
    }

    private static final class PoolCollector extends LootTable.Builder {
        private final List<LootPool.Builder> pools = new ArrayList<>();

        @Override
        public LootTable.Builder withPool(LootPool.Builder pool) {
            this.pools.add(pool);
            return this;
        }
    }

    private ForgeLootEvents() { }
}
