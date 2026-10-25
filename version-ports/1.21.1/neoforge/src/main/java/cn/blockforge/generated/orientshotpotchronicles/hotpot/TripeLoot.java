package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.RandomChanceWithEnchantedBonusLootCondition;
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
 * 另外 25% 掉一块牛脊肉（切吊龙用；新增）：抢夺每级多 1 次机会掉，被火烧死直接掉熟牛脊肉。
 * 作者：
 */
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TripeLoot::onLootTableLoad);
    }

    // 调整：包括数据包覆盖的牛掉落表，沿用已定 NeoForge 行为。
    private static void onLootTableLoad(LootTableLoadEvent event) {
            var key = event.getKey();
            var registries = event.getRegistries();
            if (!key.equals(EntityType.COW.getLootTableId()) && !key.equals(EntityType.MOOSHROOM.getLootTableId())) {
                return;
            }
            event.getTable().addPool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .with(ItemEntry.builder(HotPotContent.RAW_TRIPE)
                    .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                    .apply(FurnaceSmeltLootFunction.builder().conditionally(
                        EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true)))))
                    .apply(EnchantedCountIncreaseLootFunction.builder(registries, UniformLootNumberProvider.create(0, 1)))).build());
            event.getTable().addPool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .with(ItemEntry.builder(HotPotContent.BEEF_LOIN)
                    .conditionally(RandomChanceWithEnchantedBonusLootCondition.builder(registries, 0.25F, 0.1F))
                    .apply(FurnaceSmeltLootFunction.builder().conditionally(
                        EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true)))))).build());
    }
}
