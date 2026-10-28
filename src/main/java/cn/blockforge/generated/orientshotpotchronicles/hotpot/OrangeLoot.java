package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.TableBonusLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKeys;

/**
 * 橙子：橡木 / 深色橡木树叶掉落，概率和原版掉苹果一样（0.5%，时运 I～IV 分别 0.56% / 0.63% / 0.83% / 2.5%），
 * 和苹果各算各的；用剪刀剪树叶不掉（原版树叶整块掉）。2026-10-25 用户：橙子饮料要有原料。
 *
 */
public final class OrangeLoot {
    private static final float[] CHANCES = {0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F};

    private OrangeLoot() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()
                || !(key.equals(Blocks.OAK_LEAVES.getLootTableKey()) || key.equals(Blocks.DARK_OAK_LEAVES.getLootTableKey()))) {
                return;
            }
            tableBuilder.pool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(MatchToolLootCondition.builder(ItemPredicate.Builder.create().items(Items.SHEARS)).invert())
                .conditionally(TableBonusLootCondition.builder(
                    registries.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), CHANCES))
                .with(ItemEntry.builder(HotPotContent.ORANGE)));
        });
    }
}
