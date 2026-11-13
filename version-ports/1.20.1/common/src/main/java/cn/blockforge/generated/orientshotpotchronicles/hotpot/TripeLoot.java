package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * 牛/哞菇掉毛肚与牛脊肉，鱿鱼掉生鱿鱼，睡莲掉莲藕，蘑菇掉金针菇（mryt233 改：1.20.1 适配与 4K 配置键）。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class TripeLoot {
    private TripeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin) -> {
            if (!builtin) {
                return;
            }
            if (key.equals(EntityType.SQUID.getDefaultLootTable()) || key.equals(EntityType.GLOW_SQUID.getDefaultLootTable())) {
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(HotPotContent.RAW_SQUID)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))
                        .apply(HotPotLoot.smeltWhenBurning())
                        .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0, 1)))));
                return;
            }
            // mryt233 改（2026-11-13，4K）：莲藕、金针菇、牛脊肉概率改读配置；概率为 0 时仍保留池子仅条件不中，确保重载恢复稳定。
            if (key.equals(Blocks.LILY_PAD.getLootTable())) {
                float chance = clampChance(Config.lootLilyPadLotusRootChance());
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(HotPotLoot.withoutShears())
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(HotPotContent.LOTUS_ROOT)));
                return;
            }
            if (key.equals(Blocks.BROWN_MUSHROOM.getLootTable()) || key.equals(Blocks.RED_MUSHROOM.getLootTable())) {
                float chance = clampChance(Config.lootMushroomEnokiChance());
                tableBuilder.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(LootItemRandomChanceCondition.randomChance(chance))
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
                    .apply(HotPotLoot.smeltWhenBurning())
                    .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0, 1)))));
            float beefLoinChance = clampChance(Config.lootCowBeefLoinChance());
            float beefLoinLootingBonus = clampChance(Config.lootCowBeefLoinLootingBonus());
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(HotPotContent.BEEF_LOIN)
                    .when(LootItemRandomChanceWithLootingCondition.randomChanceAndLootingBoost(
                        beefLoinChance, beefLoinLootingBonus))
                    .apply(HotPotLoot.smeltWhenBurning())));
        });
    }

    private static float clampChance(float chance) {
        return Math.max(0F, Math.min(1F, chance));
    }
}
