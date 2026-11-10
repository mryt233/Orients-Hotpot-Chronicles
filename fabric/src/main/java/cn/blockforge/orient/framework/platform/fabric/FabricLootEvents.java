// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.loader.Framework;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/** 框架自己的战利品消费端，不依赖任何内容模组。 */
final class FabricLootEvents {
    static void install() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            for (var modification : Framework.lootEvents().all()) {
                modification.apply(key, builder, source.isBuiltin(), registries);
            }
        });
        Framework.lootEvents().markPlatformManaged();
    }

    private FabricLootEvents() { }
}
