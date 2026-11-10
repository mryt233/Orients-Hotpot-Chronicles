package cn.blockforge.generated.orientshotpotchronicles.neoforge;

// mryt233 改（2026-11-10）：第三批 3C 平台接线。

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
 * <p>「原版表」的判定：Fabric 版看 source.isBuiltin()；NeoForge 没有这个信息，
 * 这里按命名空间判断（minecraft: 的表算原版）。四套掉落针对的都是原版表，结果一致。
 * 作者：mryt233 */
public final class NeoForgeLootListeners {
    /** 加进鱼池的虾权重，和 Fabric 侧 {@code ShrimpLoot.WEIGHT} 是同一个数（mryt233 改 2026-11-07）。 */
    private static final int SHRIMP_WEIGHT = 20;
    /** 原版「钓鱼 · 鱼」表。 */
    private static final net.minecraft.resources.ResourceLocation FISHING_FISH =
        net.minecraft.resources.ResourceLocation.withDefaultNamespace("gameplay/fishing/fish");

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

    /**
     * 把核心登记的修改落到刚建好的这张表上。
     *
     * <p>mryt233 改（2026-11-07）：本方法每次都是对<b>刚由 JSON 新建的</b> {@code LootTable} 操作
     * （{@code LootDataType.deserialize} 先建表、再发本事件），所以每次重载加进的都是新表的池子，
     * <b>不会跨重载累积</b>——不要误以为需要在池子里做去重。</p>
     *
     * <p>但也请勿在此缓存 LootTable，也不要对同一张表重复调用本方法：
     * {@link PoolCollector} 里 {@code build()} 出来的池子 {@code getName()} 是 null，
     * NeoForge {@code addPool} 的判重会因此跳过，重复调用就会静默叠池。</p>
     */
    private static void onLootTableLoad(LootTableLoadEvent event) {
        boolean builtin = "minecraft".equals(event.getKey().location().getNamespace());
        // 钓鱼出虾：原版鱼池追加权重 20 的虾，保留全部原版条目与池条件。
        // 这条不能新建池（新建池等于每次多掉一件东西），只能改既有池，所以用 accessor。
        // mryt233 改（2026-11-07）：这是绕开官方扩展点的临时做法——NeoForge 官方只给了 addPool，
        // 没有「改既有池」的 API；等框架把登记接口换成窄接口 LootSink（withPool / modifyPools 两路）后删掉这里。
        if (FISHING_FISH.equals(event.getName())) {
            var pools = ((cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor) event.getTable())
                .hotpot$getPools();
            if (!pools.isEmpty()) {
                var pool = (cn.blockforge.generated.orientshotpotchronicles.mixin.LootPoolAccessor) pools.getFirst();
                // 传可变列表进去：以前是 List.copyOf(...)，等于给池子塞了一份不可变表，
                // 现在 addRandomItems 只读不写所以安全，但那是埋雷——以后任何一方想往这个池子 add 就会当场抛异常。
                var entries = new ArrayList<>(pool.hotpot$getEntries());
                entries.add(net.minecraft.world.level.storage.loot.entries.LootItem
                    .lootTableItem(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.SHRIMP)
                    .setWeight(SHRIMP_WEIGHT).build());
                pool.hotpot$setEntries(entries);
            }
        }
        if (Framework.lootEvents().isPlatformManaged()) {
            return;
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
