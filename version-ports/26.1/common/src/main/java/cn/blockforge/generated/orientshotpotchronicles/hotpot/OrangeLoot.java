// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 橙子：橡木 / 深色橡木树叶掉落，概率和原版掉苹果一样（0.5%，时运 I～IV 分别 0.56% / 0.63% / 0.83% / 2.5%），
 * 和苹果各算各的；用剪刀剪树叶不掉（原版树叶整块掉）。2026-10-25 用户：橙子饮料要有原料。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class OrangeLoot {
    private static final float[] CHANCES = {0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F};

    private OrangeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin
                // mryt233 改（26.1）：getLootTable() / getDefaultLootTable() 返回 Optional 了，直接 equals 永远不相等（掉落会悄悄失效）
                || !(Blocks.OAK_LEAVES.getLootTable().map(key::equals).orElse(false) || Blocks.DARK_OAK_LEAVES.getLootTable().map(key::equals).orElse(false))) {
                return;
            }
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(HotPotLoot.withoutShears())
                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                    registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), CHANCES))
                .add(LootItem.lootTableItem(HotPotContent.ORANGE)));
        });
    }
}
