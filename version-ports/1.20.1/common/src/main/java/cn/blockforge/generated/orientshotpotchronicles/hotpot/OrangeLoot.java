package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 橙子：橡木 / 深色橡木树叶掉落（mryt233 改：1.20.1 适配 BLOCK_FORTUNE 与 3 参数 LootEventBridge）。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class OrangeLoot {
    private static final float[] CHANCES = {0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F};

    private OrangeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin) -> {
            if (!builtin
                || !(key.equals(Blocks.OAK_LEAVES.getLootTable()) || key.equals(Blocks.DARK_OAK_LEAVES.getLootTable()))) {
                return;
            }
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(HotPotLoot.withoutShears())
                .when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE, CHANCES))
                .add(LootItem.lootTableItem(HotPotContent.ORANGE)));
        });
    }
}
