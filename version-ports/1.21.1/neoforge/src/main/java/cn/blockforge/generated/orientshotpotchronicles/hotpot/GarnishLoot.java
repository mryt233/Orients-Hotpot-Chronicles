package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.item.ItemPredicate;

/**
 * 打草掉小葱、香菜（r103，用户：打草掉落，概率「多一点」= 各 25%）。和原版打草掉小麦种子一样：用剪刀剪下来的是草本身，
 * 不掉这两样；高草只算下半格，免得一株掉两次。
 * 2026-10-23 加黄豆（做豆腐用，用户：打草掉，以后再考虑做成作物）：15%，比小葱 / 香菜少一点，打草已经掉得够多了。
 * 作者：
 */
public final class GarnishLoot {
    public static final float CHANCE = 0.25F;
    public static final float SOYBEAN_CHANCE = 0.15F;

    private GarnishLoot() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            var key = event.getKey();
            boolean shortGrass = key.equals(Blocks.SHORT_GRASS.getLootTableKey());
            boolean tallGrass = key.equals(Blocks.TALL_GRASS.getLootTableKey());
            if (!shortGrass && !tallGrass) {
                return;
            }
            for (Item item : new Item[]{HotPotContent.SCALLION, HotPotContent.CILANTRO, HotPotContent.SOYBEAN}) {
                LootPool.Builder pool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(MatchToolLootCondition.builder(ItemPredicate.Builder.create().items(Items.SHEARS)).invert())
                    .conditionally(RandomChanceLootCondition.builder(item == HotPotContent.SOYBEAN ? SOYBEAN_CHANCE : CHANCE))
                    .with(ItemEntry.builder(item));
                if (tallGrass) {
                    pool.conditionally(BlockStatePropertyLootCondition.builder(Blocks.TALL_GRASS)
                        .properties(StatePredicate.Builder.create().exactMatch(TallPlantBlock.HALF, DoubleBlockHalf.LOWER)));
                }
                event.getTable().addPool(pool.build());
            }
        });
    }
}
