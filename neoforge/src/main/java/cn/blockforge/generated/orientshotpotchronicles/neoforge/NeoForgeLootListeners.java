// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.neoforge;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ShrimpFishing;
import cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor;
import cn.blockforge.generated.orientshotpotchronicles.mixin.LootPoolAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import java.util.ArrayList;

/** 原生事件只提供建好的表，虾必须修改既有鱼池，保留访问器适配。通用追加池已由框架消费。 */
public final class NeoForgeLootListeners {
    private static boolean installed;
    private NeoForgeLootListeners() { }
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(NeoForgeLootListeners::onLootTableLoad);
    }
    private static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ShrimpFishing.FISHING_FISH.equals(event.getName())) return;
        var pools = ((LootTableAccessor) event.getTable()).hotpot$getPools();
        if (pools.isEmpty()) return;
        var pool = (LootPoolAccessor) pools.getFirst();
        var entries = new ArrayList<>(pool.hotpot$getEntries());
        entries.add(ShrimpFishing.entry().build());
        pool.hotpot$setEntries(entries);
    }
}
