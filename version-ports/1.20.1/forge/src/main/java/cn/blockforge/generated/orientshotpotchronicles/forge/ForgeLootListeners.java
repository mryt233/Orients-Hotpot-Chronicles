package cn.blockforge.generated.orientshotpotchronicles.forge;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.LootEventBridge;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 版的战利品适配层。
 * <p>
 * 打草掉小葱、树叶掉橙子、钓鱼出虾、牛掉毛肚这四套逻辑只描述「往哪张表加什么」，
 * 由核心的 {@link LootEventBridge} 登记；这里把它们挂到 Forge 的 {@link LootTableLoadEvent} 上。
 *
 * <p>和 Fabric 的差别：Fabric 的 {@code LootTableEvents.MODIFY} 直接给你一个
 * {@code LootTable.Builder}，Forge 的 {@code LootTableLoadEvent} 给你的是**已经建好的**
 * {@code LootTable}（只能往池子列表里追加）。为了不改核心的登记接口，这里用一个只记录不改写的
 * {@link LootTable.Builder} 子类接住核心塞进来的池子，再逐个追加到真正那张表的池子列表上
 * （不用反射，也不改核心）。
 *
 * <p>「原版表」的判定：Fabric 版看 source.isCustomRenderer()；Forge 没有这个信息，
 * 这里按命名空间判断（minecraft: 的表算原版）。四套掉落针对的都是原版表，结果一致。
 * 作者：mryt233 */
public final class ForgeLootListeners {
    private static boolean installed;

    private ForgeLootListeners() {
    }

    /** 只调用一次：把登记过的回调统一挂到 Forge 的战利品事件上。 */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(ForgeLootListeners::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        boolean builtin = "minecraft".equals(event.getName().getNamespace());
        // 新增：在原版鱼类池追加权重 20 的虾，保留全部原版条目与池条件。
        if (event.getName().equals(new net.minecraft.resources.ResourceLocation("gameplay/fishing/fish"))) {
            var pools = ((cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor) event.getTable()).hotpot$getPools();
            if (!pools.isEmpty()) {
                var pool = (cn.blockforge.generated.orientshotpotchronicles.mixin.LootPoolAccessor) pools.get(0);
                var entries = new ArrayList<>(java.util.Arrays.asList(pool.hotpot$getEntries()));
                entries.add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.SHRIMP).setWeight(20).build());
                pool.hotpot$setEntries(entries.toArray(net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[]::new));
            }
        }
        List<LootEventBridge.Modification> modifications = Framework.lootEvents().all();
        if (modifications.isEmpty()) {
            return;
        }
        PoolCollector collector = new PoolCollector();
        for (LootEventBridge.Modification modification : modifications) {
            modification.apply(event.getName(), collector, builtin);
        }
        // mryt233 改（2026-11-02）：不用 LootTable.addPool——那是 Forge 47.1 以后才补进来的方法，47.0.0 没有
        // （NoSuchMethodError）。Forge 从一开始就把 pools 改成了可变的 List，经访问器直接追加，效果相同。
        var tablePools = ((cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor) event.getTable()).hotpot$getPools();
        for (LootPool.Builder pool : collector.pools) {
            tablePools.add(pool.build());
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
