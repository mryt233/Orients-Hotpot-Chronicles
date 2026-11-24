// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.loader.Framework;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 9 种配菜在短草 / 高草中的稀疏兜底掉落（1.20.1 适配 {@code Blocks.GRASS} 与 3 参数 LootEventBridge）：
 * <ul>
 *   <li>完整模式（{@code mode.crops_replace_grass = true}，默认）：默认仅 {@code 0.02}（2%）作野外找种兜底，
 *       主力获取交给耕地种植与野生植株。</li>
 *   <li>若关闭 {@code mode.crops_replace_grass} 且对应配置键仍为 {@code 0.02} 默认值，
 *       {@code /reload} 后自动抬回旧版打草掉率（小葱/香菜 {@code 0.25}、其余 {@code 0.15}）。</li>
 * </ul>
 * 作者：mryt233
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
        Framework.lootEvents().register((key, tableBuilder, builtin) -> {
            if (!builtin) {
                return;
            }
            boolean shortGrass = key.equals(Blocks.GRASS.getLootTable());
            boolean tallGrass = key.equals(Blocks.TALL_GRASS.getLootTable());
            if (!shortGrass && !tallGrass) {
                return;
            }
            boolean replaceGrass = Config.cropsReplaceGrass();
            for (GarnishDrop drop : drops()) {
                float chance = effectiveChance(
                    drop.id(), Config.lootGarnishChance(drop.id(), DEFAULT_CHANCE), drop.legacyChance(), replaceGrass);
                LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(HotPotLoot.withoutShears())
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(drop.item()));
                if (tallGrass) {
                    pool.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
                }
                tableBuilder.withPool(pool);
            }
        });
    }
}
