package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.resources.ResourceLocation;

/**
 * 钓鱼钓到虾（2026-10-26 用户：加虾，目前靠钓鱼获得）：往原版「钓鱼 · 鱼」那张表里唯一的池子加一项虾，权重 20
 * （同池原版：鳕鱼 60、鲑鱼 25、河豚 13、热带鱼 2），钓到鱼的时候约 1/6 是虾。不额外多掉东西。
 * 作者：mryt233 */
public final class ShrimpLoot {
    public static final int WEIGHT = 20;
    private static final ResourceLocation FISHING_FISH = ResourceLocation.withDefaultNamespace("gameplay/fishing/fish");

    private ShrimpLoot() {
    }

    static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            boolean builtin = source.isBuiltin();
            if (!builtin || !key.location().equals(FISHING_FISH)) {
                return;
            }
            tableBuilder.modifyPools(pool -> pool.add(LootItem.lootTableItem(HotPotContent.SHRIMP).setWeight(WEIGHT)));
        });
    }
}
