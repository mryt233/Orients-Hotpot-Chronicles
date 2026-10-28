package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.util.Identifier;

/**
 * 钓鱼钓到虾（2026-10-26 用户：加虾，目前靠钓鱼获得）：往原版「钓鱼 · 鱼」那张表里唯一的池子加一项虾，权重 20
 * （同池原版：鳕鱼 60、鲑鱼 25、河豚 13、热带鱼 2），钓到鱼的时候约 1/6 是虾。不额外多掉东西。
 *
 */
public final class ShrimpLoot {
    public static final int WEIGHT = 20;
    private static final Identifier FISHING_FISH = Identifier.ofVanilla("gameplay/fishing/fish");

    private ShrimpLoot() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin() || !key.getValue().equals(FISHING_FISH)) {
                return;
            }
            tableBuilder.modifyPools(pool -> pool.with(ItemEntry.builder(HotPotContent.SHRIMP).weight(WEIGHT)));
        });
    }
}
