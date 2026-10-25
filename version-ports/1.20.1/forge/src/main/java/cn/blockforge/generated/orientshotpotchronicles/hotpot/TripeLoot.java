package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.RandomChanceWithLootingLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootingEnchantLootFunction;
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
        MinecraftForge.EVENT_BUS.addListener(TripeLoot::onLootTableLoad);
    }

    // 调整：按计划，不对数据包来源作判断。
    private static void onLootTableLoad(LootTableLoadEvent event) {
            var key = event.getName();
            if (!key.equals(EntityType.COW.getLootTableId()) && !key.equals(EntityType.MOOSHROOM.getLootTableId())) {
                return;
            }
            event.getTable().addPool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .with(ItemEntry.builder(HotPotContent.RAW_TRIPE)
                    .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                    .apply(FurnaceSmeltLootFunction.builder().conditionally(
                        EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true).build()))))
                    .apply(LootingEnchantLootFunction.builder( UniformLootNumberProvider.create(0, 1)))).build());
            event.getTable().addPool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .with(ItemEntry.builder(HotPotContent.BEEF_LOIN)
                    .conditionally(RandomChanceWithLootingLootCondition.builder( 0.25F, 0.1F))
                    .apply(FurnaceSmeltLootFunction.builder().conditionally(
                        EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true).build()))))).build());
    }
}
