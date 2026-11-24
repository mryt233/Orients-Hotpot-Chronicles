// mryt233 改：26.2 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock; // mryt233 改：26.3 战利品谓词
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;

/**
 * 9 种配菜在短草 / 高草中的稀疏兜底掉落：
 * <ul>
 *   <li>完整模式（{@code mode.crops_replace_grass = true}，默认）：默认仅 {@code 0.02}（2%）作野外找种兜底，
 *       主力获取交给耕地种植与野生植株。</li>
 *   <li>若关闭 {@code mode.crops_replace_grass} 且对应配置键仍为 {@code 0.02} 默认值，
 *       {@code /reload} 后自动抬回旧版打草掉率（小葱/香菜 {@code 0.25}、其余 {@code 0.15}）。</li>
 * </ul>
 */
public final class GarnishLoot {
    public static final float DEFAULT_CHANCE = 0.02F;
    public static final float CHANCE = DEFAULT_CHANCE;
    public static final float SOYBEAN_CHANCE = DEFAULT_CHANCE;
    public static final float GARLIC_CHANCE = DEFAULT_CHANCE;
    public static final float CHILI_CHANCE = DEFAULT_CHANCE;
    public static final float TOMATO_CHANCE = DEFAULT_CHANCE;
    public static final float PEANUT_CHANCE = DEFAULT_CHANCE;
    public static final float GREENS_CHANCE = DEFAULT_CHANCE;
    public static final float ZHE_ER_GEN_CHANCE = DEFAULT_CHANCE;

    private record GarnishDrop(Item item, String id, float legacyChance) {
    }

    private GarnishLoot() {
    }

    private static GarnishDrop[] drops() {
        return new GarnishDrop[]{
            new GarnishDrop(HotPotContent.SCALLION, "scallion", 0.25F),
            new GarnishDrop(HotPotContent.CILANTRO, "cilantro", 0.25F),
            new GarnishDrop(HotPotContent.SOYBEAN, "soybean", 0.15F),
            new GarnishDrop(HotPotContent.GARLIC, "garlic", 0.15F),
            new GarnishDrop(HotPotContent.TOMATO, "tomato", 0.15F),
            new GarnishDrop(HotPotContent.CHILI_PEPPER, "chili_pepper", 0.15F),
            new GarnishDrop(HotPotContent.PEANUT, "peanut", 0.15F),
            new GarnishDrop(HotPotContent.GREENS, "greens", 0.15F),
            new GarnishDrop(HotPotContent.ZHE_ER_GEN, "zhe_er_gen", 0.15F)
        };
    }

    static float effectiveChance(String id, float configured, float legacyChance, boolean cropsReplaceGrass) {
        if (!cropsReplaceGrass && Math.abs(configured - DEFAULT_CHANCE) < 1.0E-6F) {
            return legacyChance;
        }
        return Math.max(0F, Math.min(1F, configured));
    }

    public static void register() {
        Framework.lootEvents().register((key, tableBuilder, builtin, registries) -> {
            if (!builtin) {
                return;
            }
            // mryt233 改（26.1+）：getLootTable() / getDefaultLootTable() 返回 Optional，直接 equals 永远不相等。
            boolean shortGrass = Blocks.SHORT_GRASS.getLootTable().map(key::equals).orElse(false);
            boolean tallGrass = Blocks.TALL_GRASS.getLootTable().map(key::equals).orElse(false);
            if (!shortGrass && !tallGrass) {
                return;
            }
            boolean replaceGrass = Config.cropsReplaceGrass();
            for (GarnishDrop drop : drops()) {
                float chance = effectiveChance(
                    drop.id(), Config.lootGarnishChance(drop.id(), DEFAULT_CHANCE), drop.legacyChance(), replaceGrass);
                LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1))
                    .when(HotPotLoot.withoutShears(registries))
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(drop.item()));
                if (tallGrass) {
                    pool.when(MatchBlock.blockMatches(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),
                        Blocks.TALL_GRASS, StatePropertiesPredicate.Builder.properties()
                            .hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
                }
                tableBuilder.withPool(pool);
            }
        });
    }
}
