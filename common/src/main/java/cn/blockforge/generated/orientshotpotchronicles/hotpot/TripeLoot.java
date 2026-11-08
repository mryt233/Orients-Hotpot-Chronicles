package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;

/**
 * 牛（和哞菇）被打死时多掉毛肚：数量规则和原版牛肉完全一样（用户要求）——每次 1～3 个，抢夺每级最多多 1 个，
 * 被火烧死直接掉熟毛肚。原版的幼年牛不掉东西，这里同样不掉。
 * 另外 25% 掉一块牛脊肉（切吊龙用；新增）：抢夺每级多 1 次机会掉，被火烧死直接掉熟牛脊肉。
 * mryt233 改（2026-11-06，r137）：野外的鱿鱼 / 发光鱿鱼掉 1～2 只生鱿鱼（砧板切鱿鱼卷用，规则同毛肚：
 * 抢夺每级多 0～1 个、被火烧死直接掉熟的）；睡莲 50% 掉 1 节莲藕（剪刀剪的不掉）；棕色 / 红色蘑菇 50% 掉 1 把金针菇。
 * 作者：mryt233 */
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin) {
                return;
            }
            // mryt233 改（2026-11-06，r137）：鱿鱼 / 发光鱿鱼掉生鱿鱼，睡莲掉莲藕，两种蘑菇掉金针菇。
            if (key.equals(EntityType.SQUID.getDefaultLootTable()) || key.equals(EntityType.GLOW_SQUID.getDefaultLootTable())) {
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(HotPotContent.RAW_SQUID)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))
                        .apply(SmeltItemFunction.smelted().when(
                            LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                                EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true)))))
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1)))));
                return;
            }
            if (key.equals(Blocks.LILY_PAD.getLootTable())) {
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(MatchTool.toolMatches(net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(Items.SHEARS)).invert())
                    .when(LootItemRandomChanceCondition.randomChance(0.5F))
                    .add(LootItem.lootTableItem(HotPotContent.LOTUS_ROOT)));
                return;
            }
            if (key.equals(Blocks.BROWN_MUSHROOM.getLootTable()) || key.equals(Blocks.RED_MUSHROOM.getLootTable())) {
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(0.5F))
                    .add(LootItem.lootTableItem(HotPotContent.ENOKI_MUSHROOM)));
                return;
            }
            if (!key.equals(EntityType.COW.getDefaultLootTable()) && !key.equals(EntityType.MOOSHROOM.getDefaultLootTable())) {
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