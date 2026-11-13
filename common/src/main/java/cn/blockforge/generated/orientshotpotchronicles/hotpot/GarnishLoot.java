package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;

/**
 * 打草掉小葱、香菜（r103，用户：打草掉落，概率「多一点」= 各 25%）。和原版打草掉小麦种子一样：用剪刀剪下来的是草本身，
 * 不掉这两样；高草只算下半格，免得一株掉两次。
 * 2026-10-23 加黄豆（做豆腐用，用户：打草掉，以后再考虑做成作物）：15%，比小葱 / 香菜少一点，打草已经掉得够多了。
 * mryt233 改（2026-11-05）：辣椒（r136a）也打草掉，概率同大蒜 15%。
 * mryt233 改（2026-11-06）：花生、青菜（r137）也打草掉，同样 15%。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public final class GarnishLoot {
    public static final float CHANCE = 0.25F;
    public static final float SOYBEAN_CHANCE = 0.15F;
    /** 大蒜（2026-10-25 用户：和小葱、黄豆、香菜一样打草掉）：同黄豆 15%。 */
    public static final float GARLIC_CHANCE = 0.15F;
    /** 辣椒（mryt233 改 2026-11-05，r136a）：同大蒜 15%。 */
    public static final float CHILI_CHANCE = 0.15F;
    /** 番茄（mryt233 改 2026-11-13，4K）：从大蒜拆出独立配置键，默认 15%。 */
    public static final float TOMATO_CHANCE = 0.15F;
    /** mryt233 改（2026-11-06，r137）：花生、青菜也打草掉，概率同黄豆 / 大蒜 15%。 */
    public static final float PEANUT_CHANCE = 0.15F;
    public static final float GREENS_CHANCE = 0.15F;
    /** mryt233 改（2026-11-06，r139）：折耳根也打草掉（西南田埂上到处是），概率同青菜 15%。 */
    public static final float ZHE_ER_GEN_CHANCE = 0.15F;

    private record GarnishDrop(Item item, String id, float fallback) {
    }

    private GarnishLoot() {
    }

    private static GarnishDrop[] drops() {
        return new GarnishDrop[]{
            new GarnishDrop(HotPotContent.SCALLION, "scallion", CHANCE),
            new GarnishDrop(HotPotContent.CILANTRO, "cilantro", CHANCE),
            new GarnishDrop(HotPotContent.SOYBEAN, "soybean", SOYBEAN_CHANCE),
            new GarnishDrop(HotPotContent.GARLIC, "garlic", GARLIC_CHANCE),
            new GarnishDrop(HotPotContent.TOMATO, "tomato", TOMATO_CHANCE),
            new GarnishDrop(HotPotContent.CHILI_PEPPER, "chili_pepper", CHILI_CHANCE),
            new GarnishDrop(HotPotContent.PEANUT, "peanut", PEANUT_CHANCE),
            new GarnishDrop(HotPotContent.GREENS, "greens", GREENS_CHANCE),
            new GarnishDrop(HotPotContent.ZHE_ER_GEN, "zhe_er_gen", ZHE_ER_GEN_CHANCE)
        };
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin) {
                return;
            }
            boolean shortGrass = key.equals(Blocks.SHORT_GRASS.getLootTable());
            boolean tallGrass = key.equals(Blocks.TALL_GRASS.getLootTable());
            if (!shortGrass && !tallGrass) {
                return;
            }
            // mryt233 改（2026-11-05）：辣椒（r136a）加进打草掉落，和番茄、大蒜一样 15%。
            // mryt233 改（2026-11-06）：花生、青菜（r137）也打草掉，同样是 15%。
            // mryt233 改（2026-11-06，r139）：折耳根也进打草掉落，15%。
            // mryt233 改（2026-11-13，4K）：9 种配菜改读各自配置键，番茄独立为 loot.garnish.tomato.chance；
            // 概率为 0 时仍保留池子仅条件不中，便于 /reload 恢复。
            for (GarnishDrop drop : drops()) {
                float chance = Math.max(0F, Math.min(1F, Config.lootGarnishChance(drop.id(), drop.fallback())));
                LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(HotPotLoot.withoutShears())
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(drop.item()));
                if (tallGrass) {
                    pool.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
                }
                tableBuilder.withPool(pool);
            }
        });
    }
}
