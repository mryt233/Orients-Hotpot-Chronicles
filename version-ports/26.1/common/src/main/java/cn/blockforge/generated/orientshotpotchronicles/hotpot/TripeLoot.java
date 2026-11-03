// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.advancements.criterion.EntityFlagsPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;

/**
 * 牛（和哞菇）被打死时多掉毛肚：数量规则和原版牛肉完全一样（用户要求）——每次 1～3 个，抢夺每级最多多 1 个，
 * 被火烧死直接掉熟毛肚。原版的幼年牛不掉东西，这里同样不掉。
 * 另外 25% 掉一块牛脊肉（切吊龙用；新增）：抢夺每级多 1 次机会掉，被火烧死直接掉熟牛脊肉。
 * 作者：mryt233 */
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin) {
                return;
            }
            // mryt233 改（26.1）：getLootTable() / getDefaultLootTable() 返回 Optional 了，直接 equals 永远不相等（掉落会悄悄失效）
            if (!EntityType.COW.getDefaultLootTable().map(key::equals).orElse(false) && !EntityType.MOOSHROOM.getDefaultLootTable().map(key::equals).orElse(false)) {
                return;
            }
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(HotPotContent.RAW_TRIPE)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))
                    .apply(SmeltItemFunction.smelted().when(
                        LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true)))))
                    .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1)))));
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(HotPotContent.BEEF_LOIN)
                    .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, 0.25F, 0.1F))
                    .apply(SmeltItemFunction.smelted().when(
                        LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true)))))));
        });
    }
}