// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;

/**
 * 打草掉小葱、香菜（r103，用户：打草掉落，概率「多一点」= 各 25%）。和原版打草掉小麦种子一样：用剪刀剪下来的是草本身，
 * 不掉这两样；高草只算下半格，免得一株掉两次。
 * 2026-10-23 加黄豆（做豆腐用，用户：打草掉，以后再考虑做成作物）：15%，比小葱 / 香菜少一点，打草已经掉得够多了。
 * mryt233 改（2026-11-05）：辣椒（r136a）也打草掉，概率同大蒜 15%。
 * mryt233 改（2026-11-06）：花生、青菜（r137）也打草掉，同样 15%。
 * 作者：mryt233 */
public final class GarnishLoot {
    public static final float CHANCE = 0.25F;
    public static final float SOYBEAN_CHANCE = 0.15F;
    /** 大蒜（2026-10-25 用户：和小葱、黄豆、香菜一样打草掉）：同黄豆 15%。 */
    public static final float GARLIC_CHANCE = 0.15F;
    /** 辣椒（mryt233 改 2026-11-05，r136a）：同大蒜 15%。 */
    public static final float CHILI_CHANCE = 0.15F;
    /** mryt233 改（2026-11-06，r137）：花生、青菜也打草掉，概率同黄豆 / 大蒜 15%。 */
    public static final float PEANUT_CHANCE = 0.15F;
    public static final float GREENS_CHANCE = 0.15F;
    /** mryt233 改（2026-11-06，r139）：折耳根也打草掉（西南田埂上到处是），概率同青菜 15%。 */
    public static final float ZHE_ER_GEN_CHANCE = 0.15F;

    private GarnishLoot() {
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin) {
                return;
            }
            // mryt233 改（26.1）：getLootTable() / getDefaultLootTable() 返回 Optional 了，直接 equals 永远不相等（掉落会悄悄失效）
            boolean shortGrass = Blocks.SHORT_GRASS.getLootTable().map(key::equals).orElse(false);
            boolean tallGrass = Blocks.TALL_GRASS.getLootTable().map(key::equals).orElse(false);
            if (!shortGrass && !tallGrass) {
                return;
            }
            // mryt233 改（2026-11-05）：辣椒（r136a）加进打草掉落，和番茄、大蒜一样 15%。
            // mryt233 改（2026-11-06）：花生、青菜（r137）也打草掉，同样是 15%。
            // mryt233 改（2026-11-06，r139）：折耳根也进打草掉落，15%。
            for (Item item : new Item[]{HotPotContent.SCALLION, HotPotContent.CILANTRO, HotPotContent.SOYBEAN, HotPotContent.GARLIC,
                HotPotContent.TOMATO, HotPotContent.CHILI_PEPPER, HotPotContent.PEANUT, HotPotContent.GREENS,
                HotPotContent.ZHE_ER_GEN}) {
                LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(net.minecraft.core.registries.BuiltInRegistries.ITEM, Items.SHEARS)).invert())
                    .when(LootItemRandomChanceCondition.randomChance(item == HotPotContent.SOYBEAN ? SOYBEAN_CHANCE
                        : item == HotPotContent.GARLIC || item == HotPotContent.TOMATO ? GARLIC_CHANCE
                        : item == HotPotContent.CHILI_PEPPER ? CHILI_CHANCE
                        : item == HotPotContent.PEANUT ? PEANUT_CHANCE
                        : item == HotPotContent.GREENS ? GREENS_CHANCE
                        : item == HotPotContent.ZHE_ER_GEN ? ZHE_ER_GEN_CHANCE : CHANCE))
                    .add(LootItem.lootTableItem(item));
                if (tallGrass) {
                    pool.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
                }
                tableBuilder.withPool(pool);
            }
        });
    }
}
