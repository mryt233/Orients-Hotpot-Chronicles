// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.resources.Identifier;

/**
 * 钓鱼钓到虾（2026-10-26 用户：加虾，目前靠钓鱼获得）：往原版「钓鱼 · 鱼」那张表里唯一的池子加一项虾，权重 20
 * （同池原版：鳕鱼 60、鲑鱼 25、河豚 13、热带鱼 2），钓到鱼的时候约 1/6 是虾。不额外多掉东西。
 *
 * <p>mryt233 改（2026-11-07）：这条掉落是本模组唯一「不能新建池、只能改既有池子」的一条，
 * 所以两端写法天然不同——Fabric 用官方 {@code modifyPools}，NeoForge 侧由
 * {@code NeoForgeLootListeners} 手写改池子（那边的事件只给已建好的表）。这里把「权重」和
 * 「哪张表」都收成常量，改动时两边照同一个数走，别再各写一个字面量。</p>
 * 作者：mryt233 */
public final class ShrimpLoot {
    /** 加进鱼池的权重；NeoForge 侧必须用同一个数。 */
    public static final int WEIGHT = 20;
    /** 原版「钓鱼 · 鱼」表。 */
    private static final Identifier FISHING_FISH = Identifier.withDefaultNamespace("gameplay/fishing/fish");
    /** 只登记一次（重复调用不会叠一层监听）。 */
    private static boolean registered;

    private ShrimpLoot() {
    }

    /** 只调用一次：把虾挂到 Fabric 的战利品事件上。 */
    static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            boolean builtin = source.isBuiltin();
            if (!builtin || !key.identifier().equals(FISHING_FISH)) {
                return;
            }
            // 只往既有池里加条目、不新建池：钓到鱼时才可能变成虾，不会每次多掉一件
            tableBuilder.modifyPools(pool -> pool.add(LootItem.lootTableItem(HotPotContent.SHRIMP).setWeight(WEIGHT)));
        });
    }
}
