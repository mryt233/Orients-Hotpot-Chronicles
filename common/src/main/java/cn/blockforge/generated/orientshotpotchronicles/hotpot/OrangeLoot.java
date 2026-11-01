package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.Registries;

/**
 * 橙子：橡木 / 深色橡木树叶掉落，概率和原版掉苹果一样（0.5%，时运 I～IV 分别 0.56% / 0.63% / 0.83% / 2.5%），
 * 和苹果各算各的；用剪刀剪树叶不掉（原版树叶整块掉）。2026-10-25 用户：橙子饮料要有原料。
 * 作者：mryt233 */
public final class OrangeLoot {
    private static final float[] CHANCES = {0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F};

    private OrangeLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin
                || !(key.equals(Blocks.OAK_LEAVES.getLootTable()) || key.equals(Blocks.DARK_OAK_LEAVES.getLootTable()))) {
                return;
            }
            tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS)).invert())
                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                    registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), CHANCES))
                .add(LootItem.lootTableItem(HotPotContent.ORANGE)));
        });
    }
}
