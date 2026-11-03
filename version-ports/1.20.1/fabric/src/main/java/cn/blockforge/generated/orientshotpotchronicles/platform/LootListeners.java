package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.loader.LootEventBridge;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderLookup;

/**
 * 战利品事件的适配层。
 * <p>
 * 打草掉落、树叶掉橙子、钓鱼出虾、牛掉毛肚这四套逻辑本身只描述「往哪张表加什么」，
 * 不该知道用的是哪个加载器。它们统一往这里登记一个回调；真正挂到
 * Fabric 的 {@code LootTableEvents} 上只发生在 {@link #install()} 里。
 * 以后做 common / 平台分层时，这一段（连同 install）就是平台层要替换的部分。
 * 作者：mryt233 */
public final class LootListeners {
    private static boolean installed;

    private LootListeners() {
    }

    /** 只调用一次：把登记过的回调统一挂到加载器的战利品事件上。 */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        LootTableEvents.MODIFY.register((resourceManager, lootManager, key, builder, source) -> {
            boolean builtin = source.isBuiltin();
            for (LootEventBridge.Modification modification : Framework.lootEvents().all()) {
                modification.apply(key, builder, builtin);
            }
        });
    }
}
