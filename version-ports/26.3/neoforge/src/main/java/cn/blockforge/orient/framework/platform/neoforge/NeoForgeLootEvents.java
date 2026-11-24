// 作者：mryt233
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import java.util.ArrayList;
import java.util.List;

/** 沿用既有附加池语义；钓鱼修改既有池子的业务仍由内容侧负责。 */
final class NeoForgeLootEvents {
    static void install() {
        NeoForge.EVENT_BUS.addListener(NeoForgeLootEvents::load);
        Framework.lootEvents().markPlatformManaged();
    }

    private static void load(LootTableLoadEvent event) {
        PoolCollector collector = new PoolCollector();
        boolean builtin = "minecraft".equals(event.getName().getNamespace());
        for (var modification : Framework.lootEvents().all()) {
            modification.apply(event.getKey(), collector, builtin, event.getRegistries());
        }
        for (LootPool.Builder pool : collector.pools) {
            event.getTable().addPool(pool.build());
        }
    }

    private static final class PoolCollector extends LootTable.Builder {
        private final List<LootPool.Builder> pools = new ArrayList<>();

        @Override
        public LootTable.Builder withPool(LootPool.Builder pool) {
            pools.add(pool);
            return this;
        }
    }

    private NeoForgeLootEvents() { }
}
