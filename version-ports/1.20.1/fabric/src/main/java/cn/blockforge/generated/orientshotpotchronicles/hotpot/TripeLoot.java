// 0.1.2 的 1.20.1 Fabric 接口迁移：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
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
 * 作者：Claude（1.20.1 移植：Claude 改，换回 Fabric 掉落表 v2 事件和原版抢夺函数）
 */
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, key, tableBuilder, source) -> {
            if (!source.isBuiltin()) {
                return;
            }
            if (!key.equals(EntityType.COW.getLootTableId()) && !key.equals(EntityType.MOOSHROOM.getLootTableId())) {
                return;
            }
            tableBuilder.pool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .with(ItemEntry.builder(HotPotContent.RAW_TRIPE)
                    .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                    .apply(FurnaceSmeltLootFunction.builder().conditionally(
                        EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true).build()))))
                    .apply(LootingEnchantLootFunction.builder(UniformLootNumberProvider.create(0, 1)))));
        });
    }
}
