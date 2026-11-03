// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.LootEventBridge;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 版的战利品适配层。
 * <p>
 * 打草掉小葱、树叶掉橙子、钓鱼出虾、牛掉毛肚这四套逻辑只描述「往哪张表加什么」，
 * 由核心的 {@link LootEventBridge} 登记；这里把它们挂到 NeoForge 的 {@link LootTableLoadEvent} 上。
 *
 * <p>和 Fabric 的差别：Fabric 的 {@code LootTableEvents.MODIFY} 直接给你一个
 * {@code LootTable.Builder}，NeoForge 的 {@code LootTableLoadEvent} 给你的是**已经建好的**
 * {@code LootTable}（只能 addPool）。为了不改核心的登记接口，这里用一个只记录不改写的
 * {@link LootTable.Builder} 子类接住核心塞进来的池子，再逐个 addPool 到真正那张表上
 * （不用反射，也不改核心）。
 *
 * <p>「原版表」的判定：Fabric 版看 source.isCustomRenderer()；NeoForge 没有这个信息，
 * 这里按命名空间判断（minecraft: 的表算原版）。四套掉落针对的都是原版表，结果一致。
 * 作者：mryt233 */
public final class NeoForgeLootListeners {
    private static boolean installed;

    private NeoForgeLootListeners() {
    }

    /** 只调用一次：把登记过的回调统一挂到 NeoForge 的战利品事件上。 */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        NeoForge.EVENT_BUS.addListener(NeoForgeLootListeners::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        boolean builtin = "minecraft".equals(event.getKey().identifier().getNamespace());
        // 新增：在原版鱼类池追加权重 20 的虾，保留全部原版条目与池条件。
        if (event.getName().equals(net.minecraft.resources.Identifier.withDefaultNamespace("gameplay/fishing/fish"))) {
            var pools = ((cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor) event.getTable()).hotpot$getPools();
            if (!pools.isEmpty()) {
                var pool = (cn.blockforge.generated.orientshotpotchronicles.mixin.LootPoolAccessor) pools.getFirst();
                var entries = new ArrayList<>(pool.hotpot$getEntries());
                entries.add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.SHRIMP).setWeight(20).build());
                pool.hotpot$setEntries(List.copyOf(entries));
            }
        }
        List<LootEventBridge.Modification> modifications = Framework.lootEvents().all();
        if (modifications.isEmpty()) {
            return;
        }
        PoolCollector collector = new PoolCollector();
        for (LootEventBridge.Modification modification : modifications) {
            modification.apply(event.getKey(), collector, builtin, event.getRegistries());
        }
        for (LootPool.Builder pool : collector.pools) {
            event.getTable().addPool(pool.build());
        }
    }

    /** 只接 withPool：核心的四套掉落都只用 withPool 加池子（表级 apply 没用）。 */
    private static final class PoolCollector extends LootTable.Builder {
        private final List<LootPool.Builder> pools = new ArrayList<>();

        @Override
        public LootTable.Builder withPool(LootPool.Builder pool) {
            this.pools.add(pool);
            return this;
        }
    }
}
