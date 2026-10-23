// 1.21.1 NeoForge 移植：。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.EnchantedCountIncreaseLootFunction;
import net.minecraft.loot.function.FurnaceSmeltLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.entity.EntityFlagsPredicate;
import net.minecraft.predicate.entity.EntityPredicate;

/**
 * 牛（和哞菇）被打死时多掉毛肚：数量规则和原版牛肉完全一样（用户要求）——每次 1～3 个，抢夺每级最多多 1 个，
 * 被火烧死直接掉熟毛肚。原版的幼年牛不掉东西，这里同样不掉。
 *
 */
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TripeLoot::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        var key = event.getKey();
        if (!key.equals(EntityType.COW.getLootTableId()) && !key.equals(EntityType.MOOSHROOM.getLootTableId())) return;
        event.getTable().addPool(LootPool.builder()
            .rolls(ConstantLootNumberProvider.create(1))
            .with(ItemEntry.builder(HotPotContent.RAW_TRIPE)
                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                .apply(FurnaceSmeltLootFunction.builder().conditionally(
                    EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true)))))
                .apply(EnchantedCountIncreaseLootFunction.builder(event.getRegistries(), UniformLootNumberProvider.create(0, 1)))).build());
    }
}
