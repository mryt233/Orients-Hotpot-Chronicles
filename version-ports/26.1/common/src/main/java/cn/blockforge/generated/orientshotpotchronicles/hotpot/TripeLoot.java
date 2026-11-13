// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * 牛（和哞菇）被打死时多掉毛肚：数量规则和原版牛肉完全一样（用户要求）——每次 1～3 个，抢夺每级最多多 1 个，
 * 被火烧死直接掉熟毛肚。原版的幼年牛不掉东西，这里同样不掉。
 * 另外 25% 掉一块牛脊肉（切吊龙用；新增）：抢夺每级多 1 次机会掉，被火烧死直接掉熟牛脊肉。
 * mryt233 改（2026-11-06，r137）：野外的鱿鱼 / 发光鱿鱼掉 1～2 只生鱿鱼（砧板切鱿鱼卷用，规则同毛肚：
 * 抢夺每级多 0～1 个、被火烧死直接掉熟的）；睡莲 50% 掉 1 节莲藕（剪刀剪的不掉）；棕色 / 红色蘑菇 50% 掉 1 把金针菇。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin) {
                return;
            }
            // mryt233 改（2026-11-06，r137）：鱿鱼 / 发光鱿鱼掉生鱿鱼，睡莲掉莲藕，两种蘑菇掉金针菇。
            if (matches(key, EntityType.SQUID.getDefaultLootTable()) || matches(key, EntityType.GLOW_SQUID.getDefaultLootTable())) {
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(HotPotContent.RAW_SQUID)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))
                        .apply(HotPotLoot.smeltWhenBurning())
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1)))));
                return;
            }
            // mryt233 改（2026-11-13，4K）：莲藕、金针菇、牛脊肉概率改读配置；概率为 0 时仍保留池子仅条件不中，确保重载恢复稳定。
            if (matches(key, Blocks.LILY_PAD.getLootTable())) {
                float chance = clampChance(Config.lootLilyPadLotusRootChance());
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(HotPotLoot.withoutShears())
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(HotPotContent.LOTUS_ROOT)));
                return;
            }
            if (matches(key, Blocks.BROWN_MUSHROOM.getLootTable()) || matches(key, Blocks.RED_MUSHROOM.getLootTable())) {
                float chance = clampChance(Config.lootMushroomEnokiChance());
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(HotPotContent.ENOKI_MUSHROOM)));
                return;
            }
            // mryt233 改（26.1）：getDefaultLootTable() 返回 Optional 了，直接 equals 永远不相等（掉落会悄悄失效）
            if (!matches(key, EntityType.COW.getDefaultLootTable()) && !matches(key, EntityType.MOOSHROOM.getDefaultLootTable())) {
                return;
            }
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(HotPotContent.RAW_TRIPE)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))
                    .apply(HotPotLoot.smeltWhenBurning())
                    .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1)))));
            float beefLoinChance = clampChance(Config.lootCowBeefLoinChance());
            float beefLoinLootingBonus = clampChance(Config.lootCowBeefLoinLootingBonus());
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(HotPotContent.BEEF_LOIN)
                    .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(
                        registries, beefLoinChance, beefLoinLootingBonus))
                    .apply(HotPotLoot.smeltWhenBurning())));
        });
    }

    /** mryt233 改（26.1）：getDefaultLootTable() / getLootTable() 返回 Optional（Registry 键），直接 equals 恒不相等。 */
    private static boolean matches(ResourceKey<LootTable> key, Optional<ResourceKey<LootTable>> table) {
        return table.map(key::equals).orElse(false);
    }

    private static float clampChance(float chance) {
        return Math.max(0F, Math.min(1F, chance));
    }
}
