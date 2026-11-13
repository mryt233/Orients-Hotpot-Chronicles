// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.ShrimpFishing;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;

/** Fabric 原生 modifyPools 保留鱼池的条目、条件和每次仅掉一件的语义。 */
public final class ShrimpLoot {
    /** 保留旧常量入口，数值的唯一定义在公共代码。 */
    public static final int WEIGHT = ShrimpFishing.WEIGHT;
    private static boolean registered;

    private ShrimpLoot() {
    }

    static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        LootTableEvents.MODIFY.register((resourceManager, lootManager, key, builder, source) -> {
            if (source.isBuiltin() && key.equals(ShrimpFishing.FISHING_FISH)) {
                builder.modifyPools(pool -> pool.add(ShrimpFishing.entry()));
            }
        });
    }
}
