// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.forge;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ShrimpFishing;
import cn.blockforge.generated.orientshotpotchronicles.mixin.LootPoolAccessor;
import cn.blockforge.generated.orientshotpotchronicles.mixin.LootTableAccessor;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import java.util.ArrayList;
import java.util.Arrays;

/** 原生事件只提供建好的表，虾必须修改既有鱼池，保留访问器适配。通用追加池已由框架消费。 */
public final class ForgeLootListeners {
    private static boolean installed;

    private ForgeLootListeners() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        MinecraftForge.EVENT_BUS.addListener(ForgeLootListeners::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ShrimpFishing.FISHING_FISH.equals(event.getName())) {
            return;
        }
        var pools = ((LootTableAccessor) event.getTable()).hotpot$getPools();
        if (pools.isEmpty()) {
            return;
        }
        var pool = (LootPoolAccessor) pools.get(0);
        var entries = new ArrayList<>(Arrays.asList(pool.hotpot$getEntries()));
        entries.add(ShrimpFishing.entry().build());
        pool.hotpot$setEntries(entries.toArray(LootPoolEntryContainer[]::new));
    }
}
